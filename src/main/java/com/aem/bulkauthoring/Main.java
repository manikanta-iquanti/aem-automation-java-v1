package com.aem.bulkauthoring;

import com.aem.bulkauthoring.analyzer.BlueprintAnalyzer;
import com.aem.bulkauthoring.generator.DocumentTemplateGenerator;
import com.aem.bulkauthoring.model.Blueprint;
import com.aem.bulkauthoring.model.document.DocumentBlock;
import com.aem.bulkauthoring.packagebuilder.PackageBuilder;
import com.aem.bulkauthoring.packagebuilder.PageArtifact;
import com.aem.bulkauthoring.parser.DocumentParser;
import com.aem.bulkauthoring.updater.BlueprintUpdater;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Main {

    //    private static final String MODE = "generate-template";
    private static final String MODE = "parse-document";

    private static final File BLUEPRINT =
            new File("input/blueprint/page.json");

    private static final File ARTICLES_DIR =
            new File("input/articles");

    public static void main(String[] args) {

        switch (MODE) {

            case "generate-template":
                generateTemplate();
                break;

            case "parse-document":
                parseDocumentsAndBuildPackage();
                break;

            default:
                System.out.println("Unknown mode.");
        }
    }

    private static void generateTemplate() {

        Blueprint blueprint =
                new BlueprintAnalyzer().analyze(BLUEPRINT);

        new DocumentTemplateGenerator()
                .generate(
                        blueprint,
                        "input/templates/template.docx");

        System.out.println();
        System.out.println("Template generated.");
    }

    private static void parseDocumentsAndBuildPackage() {

        File[] docs = ARTICLES_DIR.listFiles(
                (dir, name) -> name.toLowerCase().endsWith(".docx")
                        && !name.startsWith("~$"));

        if (docs == null || docs.length == 0) {
            throw new IllegalStateException(
                    "No .docx files found in " + ARTICLES_DIR.getPath());
        }

        Arrays.sort(docs, Comparator.comparing(File::getName));

        DocumentParser parser = new DocumentParser();
        BlueprintUpdater updater = new BlueprintUpdater();
        ObjectMapper mapper = new ObjectMapper();
        List<PageArtifact> pages = new ArrayList<>();

        File pagesDir = new File("output/pages");
        pagesDir.mkdirs();

        for (File doc : docs) {
            String pageName = sanitizePageName(stripExtension(doc.getName()));

            List<DocumentBlock> blocks = parser.parse(doc.getPath());

            File updatedJson = new File(pagesDir, pageName + ".json");
            updater.update(BLUEPRINT, blocks, updatedJson);

            try {
                JsonNode pageRoot = mapper.readTree(updatedJson);
                pages.add(new PageArtifact(pageName, pageRoot));
            } catch (IOException e) {
                throw new RuntimeException("Failed to read updated blueprint: "
                        + updatedJson, e);
            }

            System.out.println("Prepared page: " + pageName
                    + " from " + doc.getName());
        }

        File zip = new PackageBuilder().build(pages);

        System.out.println();
        System.out.println("Updated blueprints written under:");
        System.out.println(pagesDir.getAbsolutePath());
        System.out.println("Install package:");
        System.out.println(zip.getAbsolutePath());
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private static String sanitizePageName(String name) {
        String sanitized = name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9-_]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");

        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("Invalid page name from: " + name);
        }

        return sanitized;
    }
}
