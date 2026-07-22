package com.aem.bulkauthoring.blueprint;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads {@link BlueprintTemplateProfile} instances from {@code input/profiles/*.json}.
 * Call {@link #reload()} after Studio saves or deletes a profile.
 */
public final class BlueprintProfileRegistry {

    public static final File PROFILES_DIR = new File("input/profiles");

    private static final Map<String, BlueprintTemplateProfile> PROFILES =
            new LinkedHashMap<>();

    static {
        reload();
    }

    private BlueprintProfileRegistry() {
    }

    public static synchronized void reload() {
        PROFILES.clear();
        if (!PROFILES_DIR.isDirectory()) {
            return;
        }
        File[] files = PROFILES_DIR.listFiles(
                (dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            JsonBlueprintProfile profile = JsonBlueprintProfile.load(file);
            PROFILES.put(profile.id(), profile);
        }
    }

    public static synchronized void register(BlueprintTemplateProfile profile) {
        PROFILES.put(profile.id(), profile);
    }

    public static synchronized BlueprintTemplateProfile get(String key) {
        BlueprintTemplateProfile profile = PROFILES.get(key);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "Unknown blueprint profile key: " + key
                            + ". Registered: " + PROFILES.keySet());
        }
        return profile;
    }

    public static synchronized boolean contains(String key) {
        return PROFILES.containsKey(key);
    }

    public static synchronized Set<String> keys() {
        return Collections.unmodifiableSet(PROFILES.keySet());
    }

    public static synchronized List<BlueprintTemplateProfile> list() {
        return Collections.unmodifiableList(new ArrayList<>(PROFILES.values()));
    }

    public static synchronized JsonBlueprintProfile getJson(String key) {
        BlueprintTemplateProfile profile = get(key);
        if (!(profile instanceof JsonBlueprintProfile)) {
            throw new IllegalStateException(
                    "Profile is not JSON-backed: " + key);
        }
        return (JsonBlueprintProfile) profile;
    }
}
