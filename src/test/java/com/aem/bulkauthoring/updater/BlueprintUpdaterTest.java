package com.aem.bulkauthoring.updater;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void toHtmlLeavesExistingMarkup() {
        String source = "<p>Already <strong>html</strong></p>";
        assertEquals(source, updater.toHtml(source));
    }
}
