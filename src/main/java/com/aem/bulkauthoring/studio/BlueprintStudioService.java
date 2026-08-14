package com.aem.bulkauthoring.studio;

import com.aem.bulkauthoring.blueprint.BlueprintPackageConfig;
import com.aem.bulkauthoring.blueprint.BlueprintProfileRegistry;
import com.aem.bulkauthoring.blueprint.BlueprintTemplateProfile;
import com.aem.bulkauthoring.blueprint.FieldFormat;
import com.aem.bulkauthoring.blueprint.JsonBlueprintProfile;
import com.aem.bulkauthoring.model.Blueprint;
import com.aem.bulkauthoring.model.BlueprintComponent;
import com.aem.bulkauthoring.model.BlueprintProperty;
import com.aem.bulkauthoring.service.BulkAuthoringService;
import com.aem.bulkauthoring.service.VaultPackageInspector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Blueprint Studio orchestration: upload, derive, field picker, readiness, Phase 1/2.
 */
public class BlueprintStudioService {

    private final BulkAuthoringService authoring = new BulkAuthoringService();
    private final VaultPackageInspector inspector = new VaultPackageInspector();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Map<String, Object>> listBlueprints() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (BlueprintTemplateProfile profile : BlueprintProfileRegistry.list()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", profile.id());
            row.put("blueprintJson", profile.blueprintJson().getPath().replace('\\', '/'));
            BlueprintPackageConfig pkg = profile.packageConfig();
            Map<String, Object> packageMap = new LinkedHashMap<>();
            packageMap.put("vaultPackageDir",
                    pkg.getVaultPackageDir().getPath().replace('\\', '/'));
            packageMap.put("contentParentPath", pkg.getContentParentPath());
            packageMap.put("samplePageName", pkg.getSamplePageName());
            packageMap.put("packageName", pkg.getPackageName());
            row.put("package", packageMap);
            if (profile instanceof JsonBlueprintProfile) {
                row.put("fieldCount", ((JsonBlueprintProfile) profile).selectedFieldCount());
            }
            out.add(row);
        }
        return out;
    }

    public Map<String, Object> createBlueprint(String rawName,
                                               InputStream jsonStream,
                                               String jsonFilename,
                                               InputStream zipStream,
                                               String zipFilename) throws IOException {
        if (rawName == null || rawName.isBlank()) {
            rawName = stripExtension(jsonFilename != null ? jsonFilename : "blueprint");
        }
        String id = BulkAuthoringService.sanitizeId(rawName);
        if (BlueprintProfileRegistry.contains(id)) {
            throw new IllegalArgumentException("Blueprint already exists: " + id);
        }

        Path blueprintPath = Path.of("input", "blueprint", id + ".json");
        Files.createDirectories(blueprintPath.getParent());
        byte[] jsonBytes = jsonStream.readAllBytes();
        try {
            mapper.readTree(jsonBytes);
        } catch (IOException e) {
            throw new IllegalArgumentException("Blueprint content is not valid JSON");
        }
        Files.write(blueprintPath, jsonBytes);

        Path vaultDir = Path.of("input", "blueprint-xml", id);
        if (Files.exists(vaultDir)) {
            deleteRecursive(vaultDir);
        }
        Files.createDirectories(vaultDir);
        unzip(zipStream, vaultDir);

        VaultPackageInspector.DerivedVaultConfig derived =
                inspector.inspect(vaultDir.toFile());

        StudioSettings settings = StudioSettings.load();
        String packageName = settings.getDefaultPackageName();
        BlueprintPackageConfig packageConfig = new BlueprintPackageConfig(
                vaultDir.toFile(),
                derived.getContentParentPath(),
                derived.getSamplePageName(),
                packageName
        );

        Path profilePath = Path.of("input", "profiles", id + ".json");
        JsonBlueprintProfile.writeNew(
                profilePath.toFile(),
                id,
                blueprintPath.toString().replace('\\', '/'),
                packageConfig,
                Collections.emptyList(),
                Collections.emptyList()
        );

        BlueprintProfileRegistry.reload();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("blueprintJson", blueprintPath.toString().replace('\\', '/'));
        result.put("derived", Map.of(
                "vaultPackageDir", vaultDir.toString().replace('\\', '/'),
                "contentParentPath", derived.getContentParentPath(),
                "samplePageName", derived.getSamplePageName(),
                "packageName", packageName
        ));
        result.put("hint",
                "Tip: in AEM, append .infinity.json to the page URL to copy/paste blueprint JSON.");
        return result;
    }

    public ObjectNode componentsTree(String id) {
        Blueprint analyzed = authoring.analyze(id);
        JsonBlueprintProfile profile = BlueprintProfileRegistry.getJson(id);

        ObjectNode root = mapper.createObjectNode();
        ArrayNode components = mapper.createArrayNode();

        Map<String, FieldFormat> selectedByPath = new LinkedHashMap<>();
        Map<String, FieldFormat> selectedByType = new LinkedHashMap<>();
        for (Map<String, String> row : profile.selectedFieldsSnapshot()) {
            String property = row.get("property");
            FieldFormat format = FieldFormat.valueOf(row.get("format"));
            String path = row.get("path");
            if (path != null && !path.isBlank()) {
                selectedByPath.put(normalizePath(path) + "|" + property, format);
            } else {
                selectedByType.put(row.get("resourceType") + "|" + property, format);
            }
        }

        for (BlueprintComponent component : analyzed.getComponents()) {
            if (isStructural(component.getResourceType())) {
                continue;
            }
            if (component.getProperties() == null || component.getProperties().isEmpty()) {
                continue;
            }

            ObjectNode node = mapper.createObjectNode();
            node.put("name", component.getName());
            node.put("path", component.getPath());
            node.put("resourceType", component.getResourceType());
            ArrayNode props = mapper.createArrayNode();
            int selectedCount = 0;
            for (BlueprintProperty property : component.getProperties()) {
                ObjectNode prop = mapper.createObjectNode();
                prop.put("property", property.getName());
                prop.put("sample", property.getValue() == null ? "" : property.getValue());
                String pathKey = normalizePath(component.getPath()) + "|" + property.getName();
                String typeKey = component.getResourceType() + "|" + property.getName();
                FieldFormat format = selectedByPath.get(pathKey);
                if (format == null) {
                    format = selectedByType.get(typeKey);
                }
                prop.put("selected", format != null);
                if (format != null) {
                    selectedCount++;
                }
                prop.put("format", format != null
                        ? format.name()
                        : guessFormat(property.getName(), property.getValue()).name());
                props.add(prop);
            }
            node.set("properties", props);
            node.put("selected", selectedCount > 0
                    && selectedCount == component.getProperties().size());
            node.put("partial", selectedCount > 0
                    && selectedCount < component.getProperties().size());
            components.add(node);
        }
        root.set("components", components);

        ArrayNode pageFields = mapper.createArrayNode();
        ObjectNode title = mapper.createObjectNode();
        title.put("property", "/jcr:content/jcr:title");
        title.put("sample", "");
        FieldFormat pageFormat = null;
        for (Map<String, String> snap : profile.selectedPageFieldsSnapshot()) {
            if ("/jcr:content/jcr:title".equals(snap.get("property"))) {
                pageFormat = FieldFormat.valueOf(snap.get("format"));
                break;
            }
        }
        title.put("selected", pageFormat != null);
        title.put("format", pageFormat != null ? pageFormat.name() : "PLAIN");
        pageFields.add(title);
        root.set("pageFields", pageFields);
        return root;
    }

    public void saveFields(String id, JsonNode body) throws IOException {
        JsonBlueprintProfile profile = BlueprintProfileRegistry.getJson(id);
        List<JsonBlueprintProfile.FieldSelection> fields = new ArrayList<>();
        List<JsonBlueprintProfile.FieldSelection> pageFields = new ArrayList<>();

        JsonNode fieldsNode = body.get("fields");
        if (fieldsNode != null && fieldsNode.isArray()) {
            for (JsonNode field : fieldsNode) {
                fields.add(new JsonBlueprintProfile.FieldSelection(
                        text(field, "resourceType"),
                        text(field, "property"),
                        parseFormat(text(field, "format")),
                        text(field, "path")
                ));
            }
        }
        JsonNode pageNode = body.get("pageFields");
        if (pageNode != null && pageNode.isArray()) {
            for (JsonNode field : pageNode) {
                pageFields.add(new JsonBlueprintProfile.FieldSelection(
                        null,
                        text(field, "property"),
                        parseFormat(text(field, "format"))
                ));
            }
        }

        profile.saveFields(fields, pageFields);
        BlueprintProfileRegistry.reload();
    }

    private static boolean isStructural(String resourceType) {
        if (resourceType == null) {
            return true;
        }
        String rt = resourceType.toLowerCase();
        return rt.endsWith("/container")
                || rt.endsWith("/page")
                || rt.endsWith("/responsivegrid")
                || rt.endsWith("/xfpage")
                || rt.contains("/structure/");
    }

    private static FieldFormat guessFormat(String property, String sample) {
        if (sample != null && sample.indexOf('<') >= 0 && sample.indexOf('>') > sample.indexOf('<')) {
            return FieldFormat.HTML;
        }
        String name = property == null ? "" : property.toLowerCase();
        if (name.contains("html") || name.equals("text") || name.equals("body")
                || name.equals("description") || name.equals("content")) {
            return FieldFormat.HTML;
        }
        return FieldFormat.PLAIN;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = path.trim().replace('\\', '/');
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public void updatePackageConfig(String id, JsonNode body) throws IOException {
        JsonBlueprintProfile profile = BlueprintProfileRegistry.getJson(id);
        BlueprintPackageConfig current = profile.packageConfig();
        String vault = body.has("vaultPackageDir")
                ? body.get("vaultPackageDir").asText()
                : current.getVaultPackageDir().getPath();
        String parent = body.has("contentParentPath")
                ? body.get("contentParentPath").asText()
                : current.getContentParentPath();
        String sample = body.has("samplePageName")
                ? body.get("samplePageName").asText()
                : current.getSamplePageName();
        String pkgName = body.has("packageName")
                ? body.get("packageName").asText()
                : current.getPackageName();

        profile.savePackageConfig(new BlueprintPackageConfig(
                new java.io.File(vault), parent, sample, pkgName));
        BlueprintProfileRegistry.reload();
    }

    public ObjectNode readiness(String id) {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode generate = mapper.createObjectNode();
        ObjectNode build = mapper.createObjectNode();
        ArrayNode generateMissing = mapper.createArrayNode();
        ArrayNode buildMissing = mapper.createArrayNode();

        if (!BlueprintProfileRegistry.contains(id)) {
            generateMissing.add("blueprint");
            buildMissing.add("blueprint");
            generate.put("ready", false);
            generate.set("missing", generateMissing);
            build.put("ready", false);
            build.set("missing", buildMissing);
            root.set("generateTemplate", generate);
            root.set("buildPackage", build);
            return root;
        }

        JsonBlueprintProfile profile = BlueprintProfileRegistry.getJson(id);
        boolean hasJson = profile.blueprintJson().isFile();
        boolean hasFields = profile.selectedFieldCount() > 0;
        BlueprintPackageConfig pkg = profile.packageConfig();
        boolean hasVault = pkg.getVaultPackageDir() != null
                && pkg.getVaultPackageDir().isDirectory();
        boolean hasPackageMeta = pkg.getContentParentPath() != null
                && !pkg.getContentParentPath().isBlank()
                && pkg.getSamplePageName() != null
                && !pkg.getSamplePageName().isBlank()
                && pkg.getPackageName() != null
                && !pkg.getPackageName().isBlank();
        File[] articles = BulkAuthoringService.listDocx(articlesDir(id));
        boolean hasArticles = articles.length > 0;

        if (!hasJson) {
            generateMissing.add("blueprintJson");
            buildMissing.add("blueprintJson");
        }
        if (!hasFields) {
            generateMissing.add("fields");
            buildMissing.add("fields");
        }
        if (!hasVault) {
            buildMissing.add("vaultPackage");
        }
        if (!hasPackageMeta) {
            buildMissing.add("packageConfig");
        }

        ArrayNode setupMissing = mapper.createArrayNode();
        buildMissing.forEach(setupMissing::add);
        if (!hasArticles) {
            buildMissing.add("articles");
        }

        generate.put("ready", generateMissing.isEmpty());
        generate.set("missing", generateMissing);
        build.put("ready", buildMissing.isEmpty());
        build.put("setupReady", setupMissing.isEmpty());
        build.set("missing", buildMissing);
        build.set("setupMissing", setupMissing);
        ArrayNode articleNames = mapper.createArrayNode();
        for (File article : articles) {
            articleNames.add(article.getName());
        }
        build.set("articles", articleNames);
        root.set("generateTemplate", generate);
        root.set("buildPackage", build);
        return root;
    }

    public File generateTemplate(String id) {
        ObjectNode ready = readiness(id);
        if (!ready.get("generateTemplate").get("ready").asBoolean()) {
            throw new IllegalStateException(
                    "Not ready to generate template: "
                            + ready.get("generateTemplate").get("missing"));
        }
        return authoring.generateTemplate(id);
    }

    public ObjectNode templatePreview(String id) throws IOException {
        File template = new File("input/templates/" + id + ".docx");
        if (!template.isFile()) {
            throw new IllegalStateException("Template not found. Generate it first.");
        }
        ObjectNode result = mapper.createObjectNode();
        result.put("path", template.getPath().replace('\\', '/'));
        result.set("blocks", extractDocxBlocks(template));
        return result;
    }

    public List<String> listArticles(String id) {
        File[] docs = BulkAuthoringService.listDocx(articlesDir(id));
        List<String> names = new ArrayList<>();
        for (File doc : docs) {
            names.add(doc.getName());
        }
        return names;
    }

    public List<String> uploadArticles(String id, List<File> uploadedArticles)
            throws IOException {
        if (!BlueprintProfileRegistry.contains(id)) {
            throw new IllegalArgumentException("Unknown blueprint: " + id);
        }
        Path articlesPath = articlesDir(id).toPath();
        Files.createDirectories(articlesPath);
        if (uploadedArticles != null) {
            for (File uploaded : uploadedArticles) {
                if (uploaded != null && uploaded.isFile()) {
                    Path dest = articlesPath.resolve(uploaded.getName());
                    Files.copy(uploaded.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        return listArticles(id);
    }

    static ArrayNode extractDocxBlocks(File docx) throws IOException {
        ObjectMapper localMapper = new ObjectMapper();
        ArrayNode blocks = localMapper.createArrayNode();
        try (java.io.InputStream in = Files.newInputStream(docx.toPath());
             org.apache.poi.xwpf.usermodel.XWPFDocument document =
                     new org.apache.poi.xwpf.usermodel.XWPFDocument(in)) {
            String pendingMarker = null;
            StringBuilder pendingValue = new StringBuilder();
            boolean collectingValue = false;

            for (org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph
                    : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text == null) {
                    text = "";
                }
                String trimmed = text.trim();
                if (trimmed.startsWith("[[") && trimmed.endsWith("]]")) {
                    if (collectingValue && pendingMarker != null) {
                        ObjectNode block = localMapper.createObjectNode();
                        block.put("marker", pendingMarker);
                        block.put("value", pendingValue.toString().trim());
                        blocks.add(block);
                    }
                    pendingMarker = trimmed;
                    pendingValue.setLength(0);
                    collectingValue = true;
                } else if (collectingValue) {
                    if (trimmed.isEmpty() && pendingValue.length() == 0) {
                        continue;
                    }
                    if (pendingValue.length() > 0) {
                        pendingValue.append('\n');
                    }
                    pendingValue.append(text);
                }
            }
            if (collectingValue && pendingMarker != null) {
                ObjectNode block = localMapper.createObjectNode();
                block.put("marker", pendingMarker);
                block.put("value", pendingValue.toString().trim());
                blocks.add(block);
            }
        }
        return blocks;
    }

    public File buildPackage(String id, List<File> uploadedArticles)
            throws IOException {
        Path articlesPath = articlesDir(id).toPath();
        Files.createDirectories(articlesPath);

        if (uploadedArticles != null) {
            for (java.io.File uploaded : uploadedArticles) {
                if (uploaded != null && uploaded.isFile()) {
                    Path dest = articlesPath.resolve(uploaded.getName());
                    Files.copy(uploaded.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        ObjectNode ready = readiness(id);
        if (!ready.get("buildPackage").get("ready").asBoolean()) {
            throw new IllegalStateException(
                    "Not ready to build package: "
                            + ready.get("buildPackage").get("missing"));
        }

        return authoring.buildPackage(id, articlesDir(id));
    }

    public void deleteBlueprint(String id) throws IOException {
        if (!BlueprintProfileRegistry.contains(id)) {
            throw new IllegalArgumentException("Unknown blueprint: " + id);
        }
        JsonBlueprintProfile profile = BlueprintProfileRegistry.getJson(id);
        Files.deleteIfExists(profile.profileFile().toPath());
        BlueprintProfileRegistry.reload();
    }

    public java.io.File articlesDir(String id) {
        return Path.of("input", "articles", id).toFile();
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }

    private static FieldFormat parseFormat(String raw) {
        if (raw == null || raw.isBlank()) {
            return FieldFormat.PLAIN;
        }
        return FieldFormat.valueOf(raw.trim().toUpperCase());
    }

    private static String stripExtension(String filename) {
        if (filename == null) {
            return "blueprint";
        }
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String name = slash >= 0 ? filename.substring(slash + 1) : filename;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    static void unzip(InputStream zipStream, Path destDir) throws IOException {
        Path normalizedDest = destDir.toAbsolutePath().normalize();
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path target = normalizedDest.resolve(entry.getName()).normalize();
                if (!target.startsWith(normalizedDest)) {
                    throw new IOException("Zip entry outside target dir: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    if (target.getParent() != null) {
                        Files.createDirectories(target.getParent());
                    }
                    try (OutputStream out = Files.newOutputStream(target)) {
                        zis.transferTo(out);
                    }
                }
                zis.closeEntry();
            }
        }

            // Some exports wrap contents; if jcr_root is nested one level, hoist it.
        Path jcrRoot = destDir.resolve("jcr_root");
        if (!Files.isDirectory(jcrRoot)) {
            try (Stream<Path> stream = Files.list(destDir)) {
                List<Path> children = stream.filter(Files::isDirectory)
                        .collect(Collectors.toList());
                if (children.size() == 1) {
                    Path nested = children.get(0);
                    Path nestedJcr = nested.resolve("jcr_root");
                    if (Files.isDirectory(nestedJcr)) {
                        hoistChildren(nested, destDir);
                        deleteRecursive(nested);
                    }
                }
            }
        }
    }

    private static void hoistChildren(Path from, Path to) throws IOException {
        try (Stream<Path> stream = Files.list(from)) {
            List<Path> children = stream.collect(Collectors.toList());
            for (Path child : children) {
                Path target = to.resolve(child.getFileName());
                Files.move(child, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void deleteRecursive(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            List<Path> paths = walk.sorted((a, b) -> b.compareTo(a))
                    .collect(Collectors.toList());
            for (Path p : paths) {
                Files.deleteIfExists(p);
            }
        }
    }
}
