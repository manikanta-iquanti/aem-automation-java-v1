package com.aem.bulkauthoring.blueprint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data-driven {@link BlueprintTemplateProfile} loaded from {@code input/profiles/*.json}.
 *
 * <p>Field entries may include an optional {@code path} to target one component instance.
 * Without {@code path}, the entry applies to every instance of that {@code resourceType}
 * (legacy sample profiles).
 */
public final class JsonBlueprintProfile implements BlueprintTemplateProfile {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String id;
    private final File profileFile;
    private final File blueprintJson;
    private final List<StoredField> storedFields;
    private final List<EditableField> pageFields;
    private final BlueprintPackageConfig packageConfig;
    private final ObjectNode root;
    private final Set<String> typesWithPathScope;

    private JsonBlueprintProfile(String id,
                                 File profileFile,
                                 File blueprintJson,
                                 List<StoredField> storedFields,
                                 List<EditableField> pageFields,
                                 BlueprintPackageConfig packageConfig,
                                 ObjectNode root) {
        this.id = id;
        this.profileFile = profileFile;
        this.blueprintJson = blueprintJson;
        this.storedFields = storedFields;
        this.pageFields = pageFields;
        this.packageConfig = packageConfig;
        this.root = root;
        this.typesWithPathScope = new LinkedHashSet<>();
        for (StoredField field : storedFields) {
            if (field.path != null && !field.path.isBlank()) {
                typesWithPathScope.add(field.resourceType);
            }
        }
    }

    public static JsonBlueprintProfile load(File profileFile) {
        try {
            JsonNode parsed = MAPPER.readTree(profileFile);
            if (!(parsed instanceof ObjectNode)) {
                throw new IllegalArgumentException(
                        "Profile must be a JSON object: " + profileFile);
            }
            ObjectNode root = (ObjectNode) parsed;
            String id = text(root, "id");
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(
                        "Profile missing id: " + profileFile);
            }

            String blueprintPath = text(root, "blueprintJson");
            if (blueprintPath == null || blueprintPath.isBlank()) {
                throw new IllegalArgumentException(
                        "Profile missing blueprintJson: " + profileFile);
            }

            List<StoredField> stored = new ArrayList<>();
            JsonNode fieldsNode = root.get("fields");
            if (fieldsNode != null && fieldsNode.isArray()) {
                for (JsonNode field : fieldsNode) {
                    String resourceType = text(field, "resourceType");
                    String property = text(field, "property");
                    FieldFormat format = parseFormat(text(field, "format"));
                    String path = text(field, "path");
                    if (resourceType == null || property == null) {
                        continue;
                    }
                    stored.add(new StoredField(resourceType, property, format, path));
                }
            }

            List<EditableField> pageFields = new ArrayList<>();
            JsonNode pageFieldsNode = root.get("pageFields");
            if (pageFieldsNode != null && pageFieldsNode.isArray()) {
                for (JsonNode field : pageFieldsNode) {
                    String property = text(field, "property");
                    FieldFormat format = parseFormat(text(field, "format"));
                    if (property != null) {
                        pageFields.add(new EditableField(property, format));
                    }
                }
            }

            JsonNode pkg = root.get("package");
            if (pkg == null || !pkg.isObject()) {
                throw new IllegalArgumentException(
                        "Profile missing package config: " + profileFile);
            }

            String vaultDir = text(pkg, "vaultPackageDir");
            String contentParent = text(pkg, "contentParentPath");
            String samplePage = text(pkg, "samplePageName");
            String packageName = text(pkg, "packageName");
            if (vaultDir == null || contentParent == null
                    || samplePage == null || packageName == null) {
                throw new IllegalArgumentException(
                        "Incomplete package config in: " + profileFile);
            }

            BlueprintPackageConfig packageConfig = new BlueprintPackageConfig(
                    new File(vaultDir),
                    contentParent,
                    samplePage,
                    packageName
            );

            return new JsonBlueprintProfile(
                    id,
                    profileFile,
                    new File(blueprintPath),
                    stored,
                    pageFields,
                    packageConfig,
                    root
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to load profile: " + profileFile, e);
        }
    }

    public File profileFile() {
        return profileFile;
    }

    public List<Map<String, String>> selectedFieldsSnapshot() {
        List<Map<String, String>> out = new ArrayList<>();
        for (StoredField field : storedFields) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("resourceType", field.resourceType);
            row.put("property", field.property);
            row.put("format", field.format.name());
            if (field.path != null && !field.path.isBlank()) {
                row.put("path", normalizePath(field.path));
            }
            out.add(row);
        }
        return out;
    }

    public List<Map<String, String>> selectedPageFieldsSnapshot() {
        List<Map<String, String>> out = new ArrayList<>();
        for (EditableField field : pageFields) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("property", field.getProperty());
            row.put("format", field.getFormat().name());
            out.add(row);
        }
        return out;
    }

    public void saveFields(List<FieldSelection> componentFields,
                           List<FieldSelection> pageFieldSelections) throws IOException {
        ArrayNode fields = MAPPER.createArrayNode();
        for (FieldSelection sel : componentFields) {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("resourceType", sel.resourceType);
            node.put("property", sel.property);
            node.put("format", sel.format.name());
            if (sel.path != null && !sel.path.isBlank()) {
                node.put("path", normalizePath(sel.path));
            }
            fields.add(node);
        }
        root.set("fields", fields);

        ArrayNode pageArr = MAPPER.createArrayNode();
        for (FieldSelection sel : pageFieldSelections) {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("property", sel.property);
            node.put("format", sel.format.name());
            pageArr.add(node);
        }
        root.set("pageFields", pageArr);

        MAPPER.writerWithDefaultPrettyPrinter().writeValue(profileFile, root);
    }

    public void savePackageConfig(BlueprintPackageConfig config) throws IOException {
        ObjectNode pkg = root.with("package");
        pkg.put("vaultPackageDir", config.getVaultPackageDir().getPath().replace('\\', '/'));
        pkg.put("contentParentPath", config.getContentParentPath());
        pkg.put("samplePageName", config.getSamplePageName());
        pkg.put("packageName", config.getPackageName());
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(profileFile, root);
    }

    public static void writeNew(File profileFile,
                                String id,
                                String blueprintJsonPath,
                                BlueprintPackageConfig packageConfig,
                                List<FieldSelection> fields,
                                List<FieldSelection> pageFields) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("id", id);
        root.put("blueprintJson", blueprintJsonPath.replace('\\', '/'));

        ArrayNode fieldsArr = MAPPER.createArrayNode();
        if (fields != null) {
            for (FieldSelection sel : fields) {
                ObjectNode node = MAPPER.createObjectNode();
                node.put("resourceType", sel.resourceType);
                node.put("property", sel.property);
                node.put("format", sel.format.name());
                if (sel.path != null && !sel.path.isBlank()) {
                    node.put("path", normalizePath(sel.path));
                }
                fieldsArr.add(node);
            }
        }
        root.set("fields", fieldsArr);

        ArrayNode pageArr = MAPPER.createArrayNode();
        if (pageFields != null) {
            for (FieldSelection sel : pageFields) {
                ObjectNode node = MAPPER.createObjectNode();
                node.put("property", sel.property);
                node.put("format", sel.format.name());
                pageArr.add(node);
            }
        }
        root.set("pageFields", pageArr);

        ObjectNode pkg = MAPPER.createObjectNode();
        pkg.put("vaultPackageDir",
                packageConfig.getVaultPackageDir().getPath().replace('\\', '/'));
        pkg.put("contentParentPath", packageConfig.getContentParentPath());
        pkg.put("samplePageName", packageConfig.getSamplePageName());
        pkg.put("packageName", packageConfig.getPackageName());
        root.set("package", pkg);

        profileFile.getParentFile().mkdirs();
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(profileFile, root);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public File blueprintJson() {
        return blueprintJson;
    }

    @Override
    public List<EditableField> fieldsFor(String resourceType) {
        List<EditableField> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (StoredField field : storedFields) {
            if (!field.resourceType.equals(resourceType)) {
                continue;
            }
            if (seen.add(field.property)) {
                out.add(new EditableField(field.property, field.format));
            }
        }
        return out;
    }

    @Override
    public List<EditableField> fieldsFor(String resourceType, String componentPath) {
        String normalizedPath = normalizePath(componentPath);
        List<EditableField> out = new ArrayList<>();
        boolean pathScoped = typesWithPathScope.contains(resourceType);

        for (StoredField field : storedFields) {
            if (!field.resourceType.equals(resourceType)) {
                continue;
            }
            if (pathScoped) {
                if (field.path == null || field.path.isBlank()) {
                    continue;
                }
                if (!normalizePath(field.path).equals(normalizedPath)) {
                    continue;
                }
            }
            out.add(new EditableField(field.property, field.format));
        }
        return out;
    }

    @Override
    public List<EditableField> pageFields() {
        return Collections.unmodifiableList(pageFields);
    }

    @Override
    public BlueprintPackageConfig packageConfig() {
        return packageConfig;
    }

    public int selectedFieldCount() {
        return pageFields.size() + storedFields.size();
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

    private static final class StoredField {
        final String resourceType;
        final String property;
        final FieldFormat format;
        final String path;

        StoredField(String resourceType, String property, FieldFormat format, String path) {
            this.resourceType = resourceType;
            this.property = property;
            this.format = format;
            this.path = path;
        }
    }

    public static final class FieldSelection {
        public final String resourceType;
        public final String property;
        public final FieldFormat format;
        public final String path;

        public FieldSelection(String resourceType, String property, FieldFormat format) {
            this(resourceType, property, format, null);
        }

        public FieldSelection(String resourceType, String property, FieldFormat format, String path) {
            this.resourceType = resourceType;
            this.property = property;
            this.format = format;
            this.path = path;
        }
    }
}
