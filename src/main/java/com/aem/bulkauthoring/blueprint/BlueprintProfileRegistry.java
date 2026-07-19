package com.aem.bulkauthoring.blueprint;

import com.aem.bulkauthoring.blueprint.profiles.MeridianArticleProfile;
import com.aem.bulkauthoring.blueprint.profiles.NormalPageProfile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class BlueprintProfileRegistry {

    private static final Map<String, BlueprintTemplateProfile> PROFILES =
            new LinkedHashMap<>();

    static {
        register(new NormalPageProfile());
        register(new MeridianArticleProfile());
    }

    private BlueprintProfileRegistry() {
    }

    public static void register(BlueprintTemplateProfile profile) {
        PROFILES.put(profile.id(), profile);
    }

    public static BlueprintTemplateProfile get(String key) {
        BlueprintTemplateProfile profile = PROFILES.get(key);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "Unknown blueprint profile key: " + key
                            + ". Registered: " + PROFILES.keySet());
        }
        return profile;
    }

    public static Set<String> keys() {
        return Collections.unmodifiableSet(PROFILES.keySet());
    }
}
