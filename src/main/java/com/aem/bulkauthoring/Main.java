package com.aem.bulkauthoring;

import com.aem.bulkauthoring.blueprint.BlueprintProfileRegistry;
import com.aem.bulkauthoring.blueprint.BlueprintTemplateProfile;
import com.aem.bulkauthoring.service.BulkAuthoringService;

import java.io.File;

/**
 * CLI entry point for Phase 1 / Phase 2. Prefer Blueprint Studio for non-tech flows:
 * {@code com.aem.bulkauthoring.studio.StudioMain}.
 */
public class Main {

    // private static final String MODE = "generate-template";
    private static final String MODE = "parse-document";

    /**
     * Blueprint profile key — must match a JSON file under {@code input/profiles/}.
     */
    private static final String BLUEPRINT_KEY = "normal-page";

    private static final File ARTICLES_DIR =
            new File("input/articles");

    public static void main(String[] args) {
        BulkAuthoringService service = new BulkAuthoringService();

        switch (MODE) {
            case "generate-template":
                File template = service.generateTemplate(BLUEPRINT_KEY);
                BlueprintTemplateProfile profile =
                        BlueprintProfileRegistry.get(BLUEPRINT_KEY);
                System.out.println();
                System.out.println("Template generated: " + template.getPath());
                System.out.println("Profile: " + profile.id());
                break;

            case "parse-document":
                File zip = service.buildPackage(BLUEPRINT_KEY, ARTICLES_DIR);
                BlueprintTemplateProfile pkgProfile =
                        BlueprintProfileRegistry.get(BLUEPRINT_KEY);
                System.out.println();
                System.out.println("Profile: " + pkgProfile.id());
                System.out.println("Updated blueprints written under:");
                System.out.println(new File("output/pages").getAbsolutePath());
                System.out.println("Install package:");
                System.out.println(zip.getAbsolutePath());
                break;

            default:
                System.out.println("Unknown mode.");
        }
    }
}
