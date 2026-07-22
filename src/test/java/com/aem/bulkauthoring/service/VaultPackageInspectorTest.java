package com.aem.bulkauthoring.service;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultPackageInspectorTest {

    private final VaultPackageInspector inspector = new VaultPackageInspector();

    @Test
    void derivesNormalPage1FromFilter() {
        File dir = new File("input/blueprint-xml/normal-page1");
        assertTrue(dir.isDirectory(), "sample vault package must exist");

        VaultPackageInspector.DerivedVaultConfig cfg = inspector.inspect(dir);

        assertEquals("normal-page1", cfg.getSamplePageName());
        assertEquals("/content/my-aem-site53/us/en/articles", cfg.getContentParentPath());
        assertEquals(dir.getPath(), cfg.getVaultPackageDir().getPath());
    }

    @Test
    void derivesPoc3FromFilter() {
        File dir = new File("input/blueprint-xml/poc-3");
        assertTrue(dir.isDirectory(), "sample vault package must exist");

        VaultPackageInspector.DerivedVaultConfig cfg = inspector.inspect(dir);

        assertEquals("vietnam-central-coast-train", cfg.getSamplePageName());
        assertEquals("/content/my-aem-site53/us/en/articles", cfg.getContentParentPath());
    }
}
