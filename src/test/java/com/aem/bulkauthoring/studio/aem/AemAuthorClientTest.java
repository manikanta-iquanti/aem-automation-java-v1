package com.aem.bulkauthoring.studio.aem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AemAuthorClientTest {

    @Test
    void normalizesFullHtmlUrl() {
        assertEquals(
                "/content/my-aem-site53/us/en/articles/normal-page1",
                AemAuthorClient.normalizeContentPath(
                        "http://localhost:4502/content/my-aem-site53/us/en/articles/normal-page1.html"));
    }

    @Test
    void normalizesPathWithoutHost() {
        assertEquals(
                "/content/site/us/en/page",
                AemAuthorClient.normalizeContentPath("/content/site/us/en/page.html"));
    }

    @Test
    void stripsInfinityJsonSelector() {
        assertEquals(
                "/content/site/page",
                AemAuthorClient.normalizeContentPath(
                        "http://localhost:4502/content/site/page.infinity.json"));
    }

    @Test
    void stripsTidyInfinityJson() {
        assertEquals(
                "/content/site/page",
                AemAuthorClient.normalizeContentPath("/content/site/page.tidy.infinity.json"));
    }

    @Test
    void stripsQueryAndFragment() {
        assertEquals(
                "/content/site/page",
                AemAuthorClient.normalizeContentPath(
                        "http://localhost:4502/content/site/page.html?wcmmode=disabled#foo"));
    }

    @Test
    void addsLeadingSlash() {
        assertEquals(
                "/content/site/page",
                AemAuthorClient.normalizeContentPath("content/site/page"));
    }

    @Test
    void rejectsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> AemAuthorClient.normalizeContentPath("  "));
    }
}
