package com.aem.bulkauthoring.studio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Persists Studio defaults under {@code input/studio-settings.json}. */
public final class StudioSettings {

    public static final File SETTINGS_FILE = new File("input/studio-settings.json");
    public static final String DEFAULT_PACKAGE_NAME = "bulk-content";
    public static final String DEFAULT_AEM_BASE_URL = "http://localhost:4502";
    public static final String DEFAULT_AEM_USERNAME = "admin";
    public static final String DEFAULT_AEM_PASSWORD = "admin";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String defaultPackageName = DEFAULT_PACKAGE_NAME;
    /** Daily authoring default: Generate hidden until setup is needed. */
    private boolean showGenerateTab = false;
    /** Daily authoring default: Adapt hidden until source conversion is needed. */
    private boolean showAdaptTab = false;
    /** Keep create/upload available when adding a new blueprint. */
    private boolean showCreateBlueprint = true;
    /** Daily authoring default: hide instructional copy. */
    private boolean showHelpText = false;
    /** How new blueprints are created in Studio: {@code aem} or {@code upload}. */
    private String createApproach = "aem";

    private String aemBaseUrl = DEFAULT_AEM_BASE_URL;
    private String aemUsername = DEFAULT_AEM_USERNAME;
    private String aemPassword = DEFAULT_AEM_PASSWORD;

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

    public boolean isShowGenerateTab() {
        return showGenerateTab;
    }

    public void setShowGenerateTab(boolean showGenerateTab) {
        this.showGenerateTab = showGenerateTab;
    }

    public boolean isShowAdaptTab() {
        return showAdaptTab;
    }

    public void setShowAdaptTab(boolean showAdaptTab) {
        this.showAdaptTab = showAdaptTab;
    }

    public boolean isShowCreateBlueprint() {
        return showCreateBlueprint;
    }

    public void setShowCreateBlueprint(boolean showCreateBlueprint) {
        this.showCreateBlueprint = showCreateBlueprint;
    }

    public boolean isShowHelpText() {
        return showHelpText;
    }

    public void setShowHelpText(boolean showHelpText) {
        this.showHelpText = showHelpText;
    }

    public String getCreateApproach() {
        return createApproach;
    }

    public void setCreateApproach(String createApproach) {
        if ("upload".equalsIgnoreCase(createApproach)) {
            this.createApproach = "upload";
        } else {
            this.createApproach = "aem";
        }
    }

    public String getAemBaseUrl() {
        return aemBaseUrl;
    }

    public void setAemBaseUrl(String aemBaseUrl) {
        if (aemBaseUrl == null || aemBaseUrl.isBlank()) {
            this.aemBaseUrl = DEFAULT_AEM_BASE_URL;
        } else {
            String trimmed = aemBaseUrl.trim();
            while (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            this.aemBaseUrl = trimmed;
        }
    }

    public String getAemUsername() {
        return aemUsername;
    }

    public void setAemUsername(String aemUsername) {
        if (aemUsername == null || aemUsername.isBlank()) {
            this.aemUsername = DEFAULT_AEM_USERNAME;
        } else {
            this.aemUsername = aemUsername.trim();
        }
    }

    public String getAemPassword() {
        return aemPassword;
    }

    public void setAemPassword(String aemPassword) {
        if (aemPassword == null) {
            this.aemPassword = DEFAULT_AEM_PASSWORD;
        } else {
            this.aemPassword = aemPassword;
        }
    }

    /** Snapshot for JSON API responses (stable key order). */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("defaultPackageName", defaultPackageName);
        map.put("showGenerateTab", showGenerateTab);
        map.put("showAdaptTab", showAdaptTab);
        map.put("showCreateBlueprint", showCreateBlueprint);
        map.put("showHelpText", showHelpText);
        map.put("createApproach", createApproach);
        map.put("aemBaseUrl", aemBaseUrl);
        map.put("aemUsername", aemUsername);
        map.put("aemPassword", aemPassword);
        return map;
    }

    public void applyFrom(JsonNode body) {
        if (body == null || !body.isObject()) {
            return;
        }
        if (body.has("defaultPackageName")) {
            setDefaultPackageName(body.get("defaultPackageName").asText());
        }
        if (body.has("showGenerateTab")) {
            setShowGenerateTab(body.get("showGenerateTab").asBoolean());
        }
        if (body.has("showAdaptTab")) {
            setShowAdaptTab(body.get("showAdaptTab").asBoolean());
        }
        if (body.has("showCreateBlueprint")) {
            setShowCreateBlueprint(body.get("showCreateBlueprint").asBoolean());
        }
        if (body.has("showHelpText")) {
            setShowHelpText(body.get("showHelpText").asBoolean());
        }
        if (body.has("createApproach")) {
            setCreateApproach(body.get("createApproach").asText());
        }
        if (body.has("aemBaseUrl")) {
            setAemBaseUrl(body.get("aemBaseUrl").asText());
        }
        if (body.has("aemUsername")) {
            setAemUsername(body.get("aemUsername").asText());
        }
        if (body.has("aemPassword")) {
            setAemPassword(body.get("aemPassword").asText());
        }
    }

    public static StudioSettings load() {
        StudioSettings settings = new StudioSettings();
        if (!SETTINGS_FILE.isFile()) {
            return settings;
        }
        try {
            JsonNode root = MAPPER.readTree(SETTINGS_FILE);
            settings.applyFrom(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load studio settings", e);
        }
        return settings;
    }

    public void save() throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("defaultPackageName", defaultPackageName);
        root.put("showGenerateTab", showGenerateTab);
        root.put("showAdaptTab", showAdaptTab);
        root.put("showCreateBlueprint", showCreateBlueprint);
        root.put("showHelpText", showHelpText);
        root.put("createApproach", createApproach);
        root.put("aemBaseUrl", aemBaseUrl);
        root.put("aemUsername", aemUsername);
        root.put("aemPassword", aemPassword);
        if (SETTINGS_FILE.getParentFile() != null) {
            SETTINGS_FILE.getParentFile().mkdirs();
        }
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(SETTINGS_FILE, root);
    }
}
