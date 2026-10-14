package com.aem.bulkauthoring.generator;

import com.aem.bulkauthoring.analyzer.BlueprintAnalyzer;
import com.aem.bulkauthoring.blueprint.BlueprintPackageConfig;
import com.aem.bulkauthoring.blueprint.FieldFormat;
import com.aem.bulkauthoring.blueprint.JsonBlueprintProfile;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentTemplateGeneratorTest {

    @Test
    void skipsExcludedComponentMarkerAndKeepsTheOther(@TempDir Path dir) throws Exception {
        Path blueprint = dir.resolve("page.json");
        Files.writeString(blueprint, "{"
                + "\"jcr:content\":{"
                + "\"jcr:title\":\"Page\","
                + "\"root\":{"
                + "\"text\":{\"sling:resourceType\":\"mysite/components/text\",\"text\":\"<p>Keep</p>\"},"
                + "\"hero\":{\"sling:resourceType\":\"mysite/components/hero\",\"title\":\"Drop me\"}"
                + "}}}");
        Path profileFile = dir.resolve("profile.json");
        JsonBlueprintProfile.writeNew(
                profileFile.toFile(),
                "exclude-template",
                blueprint.toString().replace('\\', '/'),
                new BlueprintPackageConfig(
                        new File("unused"),
                        "/content/site",
                        "sample",
                        "pkg"),
                List.of(
                        new JsonBlueprintProfile.FieldSelection(
                                "mysite/components/text", "text", FieldFormat.HTML,
                                "/jcr:content/root/text"),
                        new JsonBlueprintProfile.FieldSelection(
                                "mysite/components/hero", "title", FieldFormat.PLAIN,
                                "/jcr:content/root/hero")),
                List.of());
        JsonBlueprintProfile profile = JsonBlueprintProfile.load(profileFile.toFile());
        profile.saveFields(
                profile.selectedFieldsSnapshot().stream()
                        .map(row -> new JsonBlueprintProfile.FieldSelection(
                                row.get("resourceType"),
                                row.get("property"),
                                FieldFormat.valueOf(row.get("format")),
                                row.get("path")))
                        .collect(Collectors.toList()),
                List.of(),
                List.of("/jcr:content/root/hero"));
        profile = JsonBlueprintProfile.load(profileFile.toFile());

        Path docx = dir.resolve("template.docx");
        new DocumentTemplateGenerator().generate(
                new BlueprintAnalyzer().analyze(blueprint.toFile()),
                profile,
                blueprint.toFile(),
                docx.toString());

        String text;
        try (XWPFDocument document = new XWPFDocument(new FileInputStream(docx.toFile()))) {
            text = document.getParagraphs().stream()
                    .map(p -> p.getText())
                    .collect(Collectors.joining("\n"));
        }
        assertTrue(text.contains("[[/jcr:content/root/text/text]]"));
        assertFalse(text.contains("[[/jcr:content/root/hero/title]]"));
    }
}
