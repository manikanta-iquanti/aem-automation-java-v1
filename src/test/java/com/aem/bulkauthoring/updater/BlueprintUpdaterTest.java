package com.aem.bulkauthoring.updater;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
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
    void toHtmlLeavesExistingMarkup() {
        String source = "<p>Already <strong>html</strong></p>";
        assertEquals(source, updater.toHtml(source));
    }
}
