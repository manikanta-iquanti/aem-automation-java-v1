package com.aem.bulkauthoring.blueprint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

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
        assertFalse(normal.fieldsFor("my-aem-site53/components/text").isEmpty());
        assertEquals("normal-page1", normal.packageConfig().getSamplePageName());
        assertEquals("bulk-normal-pages", normal.packageConfig().getPackageName());

        BlueprintTemplateProfile meridian = BlueprintProfileRegistry.get("meridian-article");
        assertEquals("vietnam-central-coast-train",
                meridian.packageConfig().getSamplePageName());
        assertTrue(new File("input/profiles/meridian-article.json").isFile());
        assertTrue(JsonBlueprintProfile.load(new File("input/profiles/normal-page.json"))
                .excludedPaths().isEmpty());
    }

    @Test
    void savesAndReloadsExcludedComponentPaths(@TempDir Path dir) throws Exception {
        File profileFile = dir.resolve("sample.json").toFile();
        JsonBlueprintProfile.writeNew(
                profileFile,
                "sample",
                "input/blueprint/page.json",
                new BlueprintPackageConfig(
                        new File("input/blueprint-xml/normal-page1"),
                        "/content/site/us/en/articles",
                        "sample",
                        "sample-pkg"),
                List.of(),
                List.of());

        JsonBlueprintProfile profile = JsonBlueprintProfile.load(profileFile);
        profile.saveFields(
                List.of(new JsonBlueprintProfile.FieldSelection(
                        "mysite/components/text",
                        "text",
                        FieldFormat.HTML,
                        "/jcr:content/root/text")),
                List.of(),
                List.of("jcr:content/root/hero/"));

        JsonBlueprintProfile reloaded = JsonBlueprintProfile.load(profileFile);
        assertEquals(List.of("/jcr:content/root/hero"), reloaded.excludedPaths());
        assertEquals(1, reloaded.selectedFieldsSnapshot().size());
    }
}
