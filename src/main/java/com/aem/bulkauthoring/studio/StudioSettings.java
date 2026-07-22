package com.aem.bulkauthoring.studio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;

/** Persists Studio defaults under {@code input/studio-settings.json}. */
public final class StudioSettings {

    public static final File SETTINGS_FILE = new File("input/studio-settings.json");
    public static final String DEFAULT_PACKAGE_NAME = "bulk-content";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String defaultPackageName = DEFAULT_PACKAGE_NAME;

    public String getDefaultPackageName() {
        return defaultPackageName;
    }

    public void setDefaultPackageName(String defaultPackageName) {
        if (defaultPackageName == null || defaultPackageName.isBlank()) {
            this.defaultPackageName = DEFAULT_PACKAGE_NAME;
        } else {
            this.defaultPackageName = defaultPackageName.trim();
        }
    }

    public static StudioSettings load() {
        StudioSettings settings = new StudioSettings();
        if (!SETTINGS_FILE.isFile()) {
            return settings;
        }
        try {
            ObjectNode root = (ObjectNode) MAPPER.readTree(SETTINGS_FILE);
            if (root.has("defaultPackageName")) {
                settings.setDefaultPackageName(root.get("defaultPackageName").asText());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load studio settings", e);
        }
        return settings;
    }

    public void save() throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("defaultPackageName", defaultPackageName);
        if (SETTINGS_FILE.getParentFile() != null) {
            SETTINGS_FILE.getParentFile().mkdirs();
        }
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(SETTINGS_FILE, root);
    }
}
