package com.aem.bulkauthoring.service;

import com.aem.bulkauthoring.analyzer.BlueprintAnalyzer;
import com.aem.bulkauthoring.blueprint.BlueprintProfileRegistry;
import com.aem.bulkauthoring.blueprint.BlueprintTemplateProfile;
import com.aem.bulkauthoring.blueprint.FieldFormat;
import com.aem.bulkauthoring.blueprint.PathFormatIndex;
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
import java.util.Map;

/**
 * Shared Phase 1 / Phase 2 orchestration used by CLI {@code Main} and Blueprint Studio.
 */
public class BulkAuthoringService {

    private final BlueprintAnalyzer analyzer = new BlueprintAnalyzer();
    private final DocumentTemplateGenerator templateGenerator = new DocumentTemplateGenerator();
    private final DocumentParser parser = new DocumentParser();
    private final BlueprintUpdater updater = new BlueprintUpdater();
    private final PackageBuilder packageBuilder = new PackageBuilder();
    private final ObjectMapper mapper = new ObjectMapper();

    public File generateTemplate(String profileKey) {
        return generateTemplate(profileKey, null);
    }

    public File generateTemplate(String profileKey, File outputFile) {
        BlueprintTemplateProfile profile = BlueprintProfileRegistry.get(profileKey);
        File blueprint = requireBlueprint(profile);

        Blueprint analyzed = analyzer.analyze(blueprint);

        File out = outputFile != null
                ? outputFile
                : new File("input/templates/" + profile.id() + ".docx");
        if (out.getParentFile() != null) {
            out.getParentFile().mkdirs();
        }

        templateGenerator.generate(analyzed, profile, blueprint, out.getPath());
        return out;
    }

    public File buildPackage(String profileKey, File articlesDir) {
        File[] docs = listDocx(articlesDir);
        if (docs.length == 0) {
            throw new IllegalStateException(
                    "No .docx files found in " + articlesDir.getPath());
        }
        return buildPackage(profileKey, Arrays.asList(docs));
    }

    public File buildPackage(String profileKey, List<File> articleFiles) {
        if (articleFiles == null || articleFiles.isEmpty()) {
            throw new IllegalStateException("No article DOCX files provided");
        }

        BlueprintTemplateProfile profile = BlueprintProfileRegistry.get(profileKey);
        File blueprint = requireBlueprint(profile);

        Blueprint analyzed = analyzer.analyze(blueprint);
        Map<String, FieldFormat> formats = PathFormatIndex.build(analyzed, profile);

        List<File> docs = new ArrayList<>(articleFiles);
        docs.sort(Comparator.comparing(File::getName));

        List<PageArtifact> pages = new ArrayList<>();
        File pagesDir = new File("output/pages");
        pagesDir.mkdirs();

        for (File doc : docs) {
            String pageName = sanitizePageName(stripExtension(doc.getName()));
            List<DocumentBlock> blocks = parser.parse(doc.getPath());

            File updatedJson = new File(pagesDir, pageName + ".json");
            updater.update(blueprint, blocks, updatedJson, formats);

            try {
                JsonNode pageRoot = mapper.readTree(updatedJson);
                pages.add(new PageArtifact(pageName, pageRoot));
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to read updated blueprint: " + updatedJson, e);
            }
        }

        return packageBuilder.build(pages, profile.packageConfig());
    }

    public Blueprint analyze(String profileKey) {
        BlueprintTemplateProfile profile = BlueprintProfileRegistry.get(profileKey);
        return analyzer.analyze(requireBlueprint(profile));
    }

    public Blueprint analyzeFile(File blueprintJson) {
        return analyzer.analyze(blueprintJson);
    }

    public static File[] listDocx(File articlesDir) {
        if (articlesDir == null || !articlesDir.isDirectory()) {
            return new File[0];
        }
        File[] docs = articlesDir.listFiles(
                (dir, name) -> name.toLowerCase().endsWith(".docx")
                        && !name.startsWith("~$"));
        if (docs == null) {
            return new File[0];
        }
        Arrays.sort(docs, Comparator.comparing(File::getName));
        return docs;
    }

    public static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    public static String sanitizePageName(String name) {
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

    public static String sanitizeId(String name) {
        return sanitizePageName(name);
    }

    private static File requireBlueprint(BlueprintTemplateProfile profile) {
        File blueprint = profile.blueprintJson();
        if (blueprint == null || !blueprint.isFile()) {
            throw new IllegalStateException(
                    "Blueprint JSON missing for profile " + profile.id()
                            + ": " + (blueprint == null ? "null" : blueprint.getPath()));
        }
        return blueprint;
    }
}
