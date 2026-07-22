package com.aem.bulkauthoring.studio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** REST routes for Blueprint Studio. */
public final class StudioApi {

    private final BlueprintStudioService studio = new BlueprintStudioService();
    private final ObjectMapper mapper = new ObjectMapper();

    public void register(Javalin app) {
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
                ObjectNode result = mapper.createObjectNode();
                result.put("path", zip.getPath().replace('\\', '/'));
                result.put("absolutePath", zip.getAbsolutePath());
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
            StudioSettings settings = StudioSettings.load();
            ctx.json(Map.of("defaultPackageName", settings.getDefaultPackageName()));
        });

        app.put("/api/settings", ctx -> {
            JsonNode body = mapper.readTree(ctx.body());
            StudioSettings settings = StudioSettings.load();
            if (body.has("defaultPackageName")) {
                settings.setDefaultPackageName(body.get("defaultPackageName").asText());
            }
            settings.save();
            ctx.json(Map.of("defaultPackageName", settings.getDefaultPackageName()));
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

    private static Map<String, String> error(String message) {
        return Map.of("error", message == null ? "Unknown error" : message);
    }
}
