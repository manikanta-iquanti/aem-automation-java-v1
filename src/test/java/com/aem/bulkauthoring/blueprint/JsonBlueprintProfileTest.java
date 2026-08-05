package com.aem.bulkauthoring.blueprint;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonBlueprintProfileTest {

    @Test
    void loadsMigratedSampleProfiles() {
        BlueprintProfileRegistry.reload();
        assertTrue(BlueprintProfileRegistry.keys().contains("normal-page"));
        assertTrue(BlueprintProfileRegistry.keys().contains("meridian-article"));

        BlueprintTemplateProfile normal = BlueprintProfileRegistry.get("normal-page");
        assertEquals("normal-page", normal.id());
        assertTrue(normal.blueprintJson().isFile());
        assertFalse(normal.fieldsFor("my-aem-site53/components/author").isEmpty());
        assertEquals("normal-page1", normal.packageConfig().getSamplePageName());
        assertEquals("bulk-normal-pages", normal.packageConfig().getPackageName());

        BlueprintTemplateProfile meridian = BlueprintProfileRegistry.get("meridian-article");
        assertEquals("vietnam-central-coast-train",
                meridian.packageConfig().getSamplePageName());
        assertTrue(new File("input/profiles/meridian-article.json").isFile());
    }
}
