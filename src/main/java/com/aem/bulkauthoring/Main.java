package com.aem.bulkauthoring;

import com.aem.bulkauthoring.analyzer.BlueprintAnalyzer;
import com.aem.bulkauthoring.generator.DocumentTemplateGenerator;
import com.aem.bulkauthoring.model.Blueprint;
import com.aem.bulkauthoring.parser.DocumentParser;

import java.io.File;
import com.aem.bulkauthoring.updater.BlueprintUpdater;
import com.aem.bulkauthoring.model.document.DocumentBlock;
import com.aem.bulkauthoring.updater.BlueprintUpdater;

import java.util.List;
import com.aem.bulkauthoring.packagebuilder.PackageBuilder;

public class Main {

//    private static final String MODE = "generate-template";
     private static final String MODE = "parse-document";

    public static void main(String[] args) {

        switch (MODE) {

            case "generate-template":
                generateTemplate();
                break;

            case "parse-document":
                parseDocument();
                break;

            default:
                System.out.println("Unknown mode.");
        }
    }

    private static void generateTemplate() {

        File blueprintFile =
                new File("input/blueprint/page.json");

        Blueprint blueprint =
                new BlueprintAnalyzer().analyze(blueprintFile);

        new DocumentTemplateGenerator()
                .generate(
                        blueprint,
                        "input/templates/template.docx");

        System.out.println();

        System.out.println("Template generated.");

    }

    private static void parseDocument() {

        List<DocumentBlock> blocks =
                new DocumentParser()
                        .parse("input/articles/article1.docx");

        File blueprint =
                new File("input/blueprint/page.json");

        File output =
                new File("output/page-updated.json");

        new BlueprintUpdater().update(
                blueprint,
                blocks,
                output
        );

        System.out.println();

        System.out.println("Updated blueprint written to:");

        System.out.println(output.getAbsolutePath());

        new PackageBuilder().build(output);

    }

}