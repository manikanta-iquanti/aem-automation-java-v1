package com.aem.bulkauthoring.updater;

import com.aem.bulkauthoring.blueprint.FieldFormat;
import com.aem.bulkauthoring.model.document.DocumentBlock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueprintUpdaterTest {

    private final BlueprintUpdater updater = new BlueprintUpdater();

    @Test
    void toHtmlWrapsParagraphs() {
        String html = updater.toHtml("Hello\n\nWorld");
        assertEquals("<p>Hello</p>\n<p>World</p>", html);
    }

    @Test
    void toHtmlTurnsBulletLinesIntoList() {
        String html = updater.toHtml("Intro\n\n- Alpha\n- Beta\n\nOutro");
        assertTrue(html.contains("<p>Intro</p>"));
        assertTrue(html.contains("<ul>"));
        assertTrue(html.contains("<li>Alpha</li>"));
        assertTrue(html.contains("<li>Beta</li>"));
        assertTrue(html.contains("<p>Outro</p>"));
    }

    @Test
    void toHtmlAcceptsStarBullets() {
        assertEquals("<ul>\n<li>One</li>\n<li>Two</li>\n</ul>", updater.toHtml("* One\n* Two"));
    }

    @Test
    void removesExcludedComponentAndKeepsSibling(@TempDir Path dir) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Path blueprint = dir.resolve("page.json");
        Files.writeString(blueprint, "{"
                + "\"jcr:content\":{\"root\":{"
                + "\"text\":{\"sling:resourceType\":\"mysite/components/text\",\"text\":\"Keep\"},"
                + "\"hero\":{\"sling:resourceType\":\"mysite/components/hero\",\"title\":\"Drop\"}"
                + "}}}");
        Path updated = dir.resolve("updated.json");

        updater.update(
                blueprint.toFile(),
                List.of(),
                updated.toFile(),
                Map.of(),
                List.of("/jcr:content/root/hero"));

        JsonNode root = mapper.readTree(updated.toFile());
        assertTrue(root.at("/jcr:content/root/hero").isMissingNode());
        assertEquals("Keep", root.at("/jcr:content/root/text/text").asText());
        assertFalse(root.at("/jcr:content/root/text").isMissingNode());
    }

    @Test
    void clonesArticleComponentPerSectionDirectlyAfterPreviousCopy(@TempDir Path dir) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Path blueprint = dir.resolve("usb.json");
        Files.writeString(blueprint, "{"
                + "\"jcr:content\":{\"root\":{\"leftPar\":{"
                + "\"textmodule_copy\":{\"sling:resourceType\":\"usbank/components/content/textModule\","
                + "\"jcr:uuid\":\"abc\",\"cq:lastModified\":\"x\",\"text\":\"Seed\",\"textIsRich\":\"true\"},"
                + "\"textmodule\":{\"sling:resourceType\":\"usbank/components/content/textModule\",\"text\":\"Other\"},"
                + "\"fullspancontentblock\":{\"title\":\"Span\"}"
                + "}}}}");
        Path updated = dir.resolve("updated.json");
        String article = "/jcr:content/root/leftPar/textmodule_copy/text";

        updater.update(
                blueprint.toFile(),
                List.of(block(article, "<p>One</p>"),
                        block(article + "#2", "<p>Two</p>"),
                        block(article + "#3", "<p>Three</p>")),
                updated.toFile(),
                Map.of(article, FieldFormat.HTML),
                List.of("/jcr:content/root/leftPar/textmodule"));

        JsonNode leftPar = mapper.readTree(updated.toFile()).at("/jcr:content/root/leftPar");
        List<String> order = new ArrayList<>();
        leftPar.fieldNames().forEachRemaining(order::add);
        assertEquals(List.of("textmodule_copy", "textmodule_copy_2", "textmodule_copy_3",
                "fullspancontentblock"), order);
        assertEquals("<p>One</p>", leftPar.at("/textmodule_copy/text").asText());
        assertEquals("<p>Two</p>", leftPar.at("/textmodule_copy_2/text").asText());
        assertEquals("<p>Three</p>", leftPar.at("/textmodule_copy_3/text").asText());
        assertEquals("true", leftPar.at("/textmodule_copy_3/textIsRich").asText());
        assertEquals("abc", leftPar.at("/textmodule_copy/jcr:uuid").asText());
        assertTrue(leftPar.at("/textmodule_copy_2/jcr:uuid").isMissingNode());
        assertTrue(leftPar.at("/textmodule_copy_2/cq:lastModified").isMissingNode());
    }

    private static DocumentBlock block(String id, String value) {
        DocumentBlock b =
                new DocumentBlock();
        b.setId(id);
        b.setValue(value);
        return b;
    }

    @Test
    void toHtmlLeavesExistingMarkup() {
        String source = "<p>Already <strong>html</strong></p>";
        assertEquals(source, updater.toHtml(source));
    }
}
