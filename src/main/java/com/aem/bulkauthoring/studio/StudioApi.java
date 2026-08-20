package com.aem.bulkauthoring.studio;

import com.aem.bulkauthoring.docadapt.DocAdaptService;
import com.aem.bulkauthoring.docadapt.mapping.SlotBinding;
import com.aem.bulkauthoring.docadapt.review.AdaptReviewModel;
import com.aem.bulkauthoring.studio.aem.AemAuthorClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** REST routes for Blueprint Studio. */
public final class StudioApi {

    private final BlueprintStudioService studio = new BlueprintStudioService();
    private final DocAdaptService adaptService = new DocAdaptService();
    private final ObjectMapper mapper = new ObjectMapper();

    public void register(Javalin app) {
        registerAdaptRoutes(app);
        app.get("/api/blueprints", ctx -> ctx.json(studio.listBlueprints()));

        app.post("/api/blueprints", ctx -> {
            String name = ctx.formParam("name");
            UploadedFile jsonFile = ctx.uploadedFile("json");
            String jsonText = ctx.formParam("jsonText");
            UploadedFile zipFile = ctx.uploadedFile("zip");
            boolean hasJsonFile = jsonFile != null;
            boolean hasJsonText = jsonText != null && !jsonText.isBlank();
            if ((!hasJsonFile && !hasJsonText) || zipFile == null) {
                ctx.status(400).json(error(
                        "Provide blueprint JSON (file or paste) and a FileVault zip"));
                return;
            }
            try {
                InputStream jsonIn;
                String jsonFilename;
                if (hasJsonFile) {
                    jsonIn = jsonFile.content();
                    jsonFilename = jsonFile.filename();
                } else {
                    jsonIn = new java.io.ByteArrayInputStream(
                            jsonText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    jsonFilename = (name == null || name.isBlank() ? "blueprint" : name) + ".json";
                }
                try (InputStream zipIn = zipFile.content();
                     InputStream jsonStream = jsonIn) {
                    Map<String, Object> created = studio.createBlueprint(
                            name,
                            jsonStream,
                            jsonFilename,
                            zipIn,
                            zipFile.filename()
                    );
                    ctx.status(201).json(created);
                }
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(error(e.getMessage()));
            } catch (com.fasterxml.jackson.core.JsonParseException e) {
                ctx.status(400).json(error("Pasted content is not valid JSON"));
            }
        });

        app.post("/api/blueprints/from-aem", ctx -> {
            JsonNode body = mapper.readTree(ctx.body());
            String name = body.has("name") ? body.get("name").asText() : null;
            String pageUrl = body.has("pageUrl") ? body.get("pageUrl").asText() : null;
            if (pageUrl == null || pageUrl.isBlank()) {
                ctx.status(400).json(error("pageUrl is required"));
                return;
            }
            StudioSettings settings = StudioSettings.load();
            AemAuthorClient aem = new AemAuthorClient(
                    settings.getAemBaseUrl(),
                    settings.getAemUsername(),
                    settings.getAemPassword());
            try {
                String path = AemAuthorClient.normalizeContentPath(pageUrl);
                String idHint = (name == null || name.isBlank())
                        ? path.substring(path.lastIndexOf('/') + 1)
                        : name;
                try (InputStream jsonIn = aem.fetchInfinityJsonStream(pageUrl);
                     InputStream zipIn = aem.exportPagePackageStream(pageUrl)) {
                    Map<String, Object> created = studio.createBlueprint(
                            idHint,
                            jsonIn,
                            idHint + ".json",
                            zipIn,
                            idHint + ".zip"
                    );
                    ctx.status(201).json(created);
                }
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(error(e.getMessage()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ctx.status(500).json(error("Interrupted while contacting AEM"));
            } catch (IOException e) {
                ctx.status(502).json(error(e.getMessage()));
            }
        });

        app.get("/api/blueprints/{id}/components", ctx -> {
            try {
                ctx.json(studio.componentsTree(ctx.pathParam("id")));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(error(e.getMessage()));
            }
        });

        app.put("/api/blueprints/{id}/fields", ctx -> {
            try {
                JsonNode body = mapper.readTree(ctx.body());
                studio.saveFields(ctx.pathParam("id"), body);
                ctx.json(Map.of("ok", true));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(error(e.getMessage()));
            } catch (Exception e) {
                ctx.status(400).json(error(e.getMessage()));
            }
        });

        app.put("/api/blueprints/{id}/package", ctx -> {
            try {
                JsonNode body = mapper.readTree(ctx.body());
                studio.updatePackageConfig(ctx.pathParam("id"), body);
                ctx.json(Map.of("ok", true));
            } catch (Exception e) {
                ctx.status(400).json(error(e.getMessage()));
            }
        });

        app.get("/api/blueprints/{id}/readiness", ctx -> {
            try {
                ctx.json(studio.readiness(ctx.pathParam("id")));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(error(e.getMessage()));
            }
        });

        app.post("/api/blueprints/{id}/generate-template", ctx -> {
            try {
                String id = ctx.pathParam("id");
                File template = studio.generateTemplate(id);
                ObjectNode result = mapper.createObjectNode();
                result.put("path", template.getPath().replace('\\', '/'));
                result.put("absolutePath", template.getAbsolutePath());
                result.set("blocks", BlueprintStudioService.extractDocxBlocks(template));
                ctx.json(result);
            } catch (IllegalStateException e) {
                ctx.status(400).json(error(e.getMessage()));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(error(e.getMessage()));
            } catch (java.io.IOException e) {
                ctx.status(500).json(error("Failed to read generated template: " + e.getMessage()));
            }
        });

        app.get("/api/blueprints/{id}/template-preview", ctx -> {
            try {
                ctx.json(studio.templatePreview(ctx.pathParam("id")));
            } catch (IllegalStateException e) {
                ctx.status(404).json(error(e.getMessage()));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(error(e.getMessage()));
            }
        });

        app.get("/api/blueprints/{id}/articles", ctx -> {
            try {
                ctx.json(Map.of("articles", studio.listArticles(ctx.pathParam("id"))));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(error(e.getMessage()));
            }
        });

        app.post("/api/blueprints/{id}/articles", ctx -> {
            String id = ctx.pathParam("id");
            List<File> uploaded = new ArrayList<>();
            File tempDir = Files.createTempDirectory("studio-articles-").toFile();
            try {
                for (UploadedFile file : ctx.uploadedFiles("articles")) {
                    File dest = new File(tempDir, file.filename());
                    try (InputStream in = file.content()) {
                        Files.copy(in, dest.toPath());
                    }
                    uploaded.add(dest);
                }
                if (uploaded.isEmpty()) {
                    ctx.status(400).json(error("Choose at least one .docx article"));
                    return;
                }
                List<String> names = studio.uploadArticles(id, uploaded);
                ctx.json(Map.of("articles", names));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(error(e.getMessage()));
            } finally {
                File[] leftovers = tempDir.listFiles();
                if (leftovers != null) {
                    for (File f : leftovers) {
                        f.delete();
                    }
                }
                tempDir.delete();
            }
        });

        app.post("/api/blueprints/{id}/build-package", ctx -> {
            String id = ctx.pathParam("id");
            List<File> uploaded = new ArrayList<>();
            List<UploadedFile> files = ctx.uploadedFiles("articles");
            File tempDir = Files.createTempDirectory("studio-articles-").toFile();
            try {
                for (UploadedFile file : files) {
                    File dest = new File(tempDir, file.filename());
                    try (InputStream in = file.content()) {
                        Files.copy(in, dest.toPath());
                    }
                    uploaded.add(dest);
                }
                File zip = studio.buildPackage(id, uploaded);
                StudioSettings settings = StudioSettings.load();
                ObjectNode result = mapper.createObjectNode();
                result.put("path", zip.getPath().replace('\\', '/'));
                result.put("absolutePath", zip.getAbsolutePath());
                result.put("installed", false);
                if ("install".equals(settings.getDeliveryMethod())) {
                    AemAuthorClient aem = new AemAuthorClient(
                            settings.getAemBaseUrl(),
                            settings.getAemUsername(),
                            settings.getAemPassword());
                    try {
                        String installMessage = aem.uploadAndInstall(zip);
                        result.put("installed", true);
                        result.put("installMessage", installMessage);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        result.put("error", "Package built, but AEM installation was interrupted");
                        ctx.status(500).json(result);
                        return;
                    } catch (IOException | IllegalArgumentException e) {
                        result.put("error", "Package built, but AEM installation failed: "
                                + e.getMessage());
                        ctx.status(502).json(result);
                        return;
                    }
                }
                ctx.json(result);
            } catch (IllegalStateException e) {
                ctx.status(400).json(error(e.getMessage()));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(error(e.getMessage()));
            } finally {
                File[] leftovers = tempDir.listFiles();
                if (leftovers != null) {
                    for (File f : leftovers) {
                        f.delete();
                    }
                }
                tempDir.delete();
            }
        });

        app.delete("/api/blueprints/{id}", ctx -> {
            try {
                studio.deleteBlueprint(ctx.pathParam("id"));
                ctx.json(Map.of("ok", true));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(error(e.getMessage()));
            }
        });

        app.get("/api/settings", ctx -> {
            ctx.json(StudioSettings.load().toMap());
        });

        app.put("/api/settings", ctx -> {
            JsonNode body = mapper.readTree(ctx.body());
            StudioSettings settings = StudioSettings.load();
            settings.applyFrom(body);
            settings.save();
            ctx.json(settings.toMap());
        });

        app.get("/api/blueprints/{id}/download/template", ctx -> {
            File template = new File("input/templates/" + ctx.pathParam("id") + ".docx");
            if (!template.isFile()) {
                ctx.status(404).json(error("Template not found. Generate it first."));
                return;
            }
            ctx.header("Content-Disposition",
                    "attachment; filename=\"" + template.getName() + "\"");
            ctx.result(Files.newInputStream(template.toPath()));
        });

        app.get("/api/blueprints/{id}/download/package", ctx -> {
            try {
                com.aem.bulkauthoring.blueprint.BlueprintTemplateProfile profile =
                        com.aem.bulkauthoring.blueprint.BlueprintProfileRegistry.get(
                                ctx.pathParam("id"));
                File zip = new File("output/" + profile.packageConfig().getPackageName() + ".zip");
                if (!zip.isFile()) {
                    ctx.status(404).json(error("Package zip not found. Build it first."));
                    return;
                }
                ctx.header("Content-Disposition",
                        "attachment; filename=\"" + zip.getName() + "\"");
                ctx.result(Files.newInputStream(zip.toPath()));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(error(e.getMessage()));
            }
        });
    }

    /**
     * Optional Adapt flow — isolated from Phase 1/2. Existing generate/upload/build
     * routes above do not call {@link DocAdaptService}.
     */
    private void registerAdaptRoutes(Javalin app) {
        app.get("/api/adapt/mappings", ctx -> {
            try {
                ctx.json(Map.of("mappings", adaptService.listMappings()));
            } catch (Exception e) {
                ctx.status(500).json(error(e.getMessage()));
            }
        });

        app.get("/api/adapt/templates", ctx -> {
            try {
                ctx.json(Map.of("templates", adaptService.listTemplates()));
            } catch (Exception e) {
                ctx.status(500).json(error(e.getMessage()));
            }
        });

        app.get("/api/adapt/recipes", ctx -> {
            ctx.json(Map.of("recipes", adaptService.listRecipes()));
        });

        app.post("/api/adapt/run", ctx -> {
            String mappingId = ctx.formParam("mappingId");
            String template = ctx.formParam("template");
            String recipe = ctx.formParam("recipe");
            boolean auto = mappingId == null
                    || mappingId.isBlank()
                    || DocAdaptService.AUTO_MAPPING_ID.equals(mappingId);
            if (auto && (template == null || template.isBlank())) {
                ctx.status(400).json(error("Select a template (or a saved mapping)"));
                return;
            }
            File tempDir = Files.createTempDirectory("studio-adapt-").toFile();
            List<File> uploaded = new ArrayList<>();
            try {
                for (UploadedFile file : ctx.uploadedFiles("sources")) {
                    File dest = new File(tempDir, file.filename());
                    try (InputStream in = file.content()) {
                        Files.copy(in, dest.toPath());
                    }
                    uploaded.add(dest);
                }
                List<File> sources = uploaded.isEmpty() ? null : uploaded;
                DocAdaptService.AdaptJobResult result = auto
                        ? adaptService.runAuto(DocAdaptService.resolveTemplate(template), recipe, sources)
                        : adaptService.run(mappingId, sources);
                ctx.json(toAdaptJson(result));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(error(e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(error(e.getMessage()));
            } finally {
                File[] leftovers = tempDir.listFiles();
                if (leftovers != null) {
                    for (File f : leftovers) {
                        f.delete();
                    }
                }
                tempDir.delete();
            }
        });

        app.post("/api/adapt/rebind", ctx -> {
            try {
                JsonNode body = mapper.readTree(ctx.body());
                String jobId = text(body, "jobId");
                if (jobId == null) {
                    ctx.status(400).json(error("jobId is required"));
                    return;
                }
                ctx.json(toAdaptJson(adaptService.rebind(jobId, readBindings(body.get("bindings")))));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(error(e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(error(e.getMessage()));
            }
        });

        app.post("/api/adapt/mappings", ctx -> {
            try {
                JsonNode body = mapper.readTree(ctx.body());
                String id = text(body, "id");
                String template = text(body, "targetTemplate");
                if (id == null || template == null) {
                    ctx.status(400).json(error("id and targetTemplate are required"));
                    return;
                }
                File saved = adaptService.saveMapping(
                        id,
                        template,
                        text(body, "sourceRecipe"),
                        text(body, "bindStrategy"),
                        readBindings(body.get("bindings")));
                ctx.json(Map.of("id", id, "path", saved.getPath().replace('\\', '/')));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(error(e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(error(e.getMessage()));
            }
        });

        app.get("/api/adapt/review/{jobId}", ctx -> {
            try {
                ctx.json(toAdaptJson(adaptService.getReview(ctx.pathParam("jobId"))));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(error(e.getMessage()));
            }
        });

        app.get("/api/adapt/download/{jobId}/{file}", ctx -> {
            try {
                File adapted = adaptService.resolveAdaptedFile(
                        ctx.pathParam("jobId"), ctx.pathParam("file"));
                ctx.header("Content-Disposition",
                        "attachment; filename=\"" + adapted.getName() + "\"");
                ctx.result(Files.newInputStream(adapted.toPath()));
            } catch (IllegalArgumentException e) {
                ctx.status(404).json(error(e.getMessage()));
            }
        });

        app.post("/api/adapt/send-to-articles", ctx -> {
            try {
                JsonNode body = mapper.readTree(ctx.body());
                String jobId = text(body, "jobId");
                String blueprintId = text(body, "blueprintId");
                if (jobId == null || blueprintId == null) {
                    ctx.status(400).json(error("jobId and blueprintId are required"));
                    return;
                }
                List<String> files = new ArrayList<>();
                if (body.has("files") && body.get("files").isArray()) {
                    for (JsonNode n : body.get("files")) {
                        files.add(n.asText());
                    }
                }
                if (files.isEmpty()) {
                    ctx.status(400).json(error("Choose at least one adapted file"));
                    return;
                }
                List<String> copied = adaptService.sendToArticles(
                        jobId, files, studio.articlesDir(blueprintId));
                ctx.json(Map.of("articles", copied));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(error(e.getMessage()));
            } catch (Exception e) {
                ctx.status(500).json(error(e.getMessage()));
            }
        });
    }

    private ObjectNode toAdaptJson(DocAdaptService.AdaptJobResult result) {
        ObjectNode root = mapper.createObjectNode();
        root.put("jobId", result.jobId);
        root.put("mappingId", result.mappingId);
        if (result.templatePath != null) {
            root.put("templatePath", result.templatePath);
        }
        if (result.recipeId != null) {
            root.put("recipeId", result.recipeId);
        }
        ArrayNode reviews = root.putArray("reviews");
        for (AdaptReviewModel review : result.reviews) {
            ObjectNode r = reviews.addObject();
            r.put("sourceFile", review.getSourceFile());
            r.put("adaptedFile", review.getAdaptedFile());
            r.put("sourcePlainText", review.getSourcePlainText());
            if (review.getRecipeId() != null) {
                r.put("recipeId", review.getRecipeId());
            }
            if (review.getBindStrategy() != null) {
                r.put("bindStrategy", review.getBindStrategy());
            }
            ArrayNode slots = r.putArray("slots");
            for (AdaptReviewModel.SlotReview slot : review.getSlots()) {
                ObjectNode s = slots.addObject();
                s.put("path", slot.getPath());
                s.put("value", slot.getValue());
                s.put("sourceExcerpt", slot.getSourceExcerpt());
                if (slot.getUnitId() != null) {
                    s.put("unitId", slot.getUnitId());
                }
            }
            ArrayNode units = r.putArray("units");
            if (review.getUnits() != null) {
                for (var unit : review.getUnits()) {
                    ObjectNode u = units.addObject();
                    u.put("id", unit.getId());
                    u.put("label", unit.getLabel());
                    u.put("value", unit.getValue());
                }
            }
            ArrayNode bindings = r.putArray("bindings");
            if (review.getBindings() != null) {
                for (var b : review.getBindings()) {
                    ObjectNode n = bindings.addObject();
                    n.put("unit", b.getUnit());
                    n.put("path", b.getPath());
                }
            }
        }
        return root;
    }

    private static List<SlotBinding> readBindings(JsonNode node) {
        List<SlotBinding> out = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return out;
        }
        for (JsonNode n : node) {
            String unit = text(n, "unit");
            String path = text(n, "path");
            if (path != null) {
                out.add(new SlotBinding(unit, path));
            }
        }
        return out;
    }

    private static String text(JsonNode body, String field) {
        if (body == null || !body.has(field) || body.get(field).isNull()) {
            return null;
        }
        String v = body.get(field).asText();
        return v == null || v.isBlank() ? null : v;
    }

    private static Map<String, String> error(String message) {
        return Map.of("error", message == null ? "Unknown error" : message);
    }
}
