package com.aem.bulkauthoring.docadapt.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotCatalogTest {

    @Test
    void infersPageTitleAndSingleRichTextFromNormalPagePaths() {
        SlotCatalog catalog = SlotCatalog.fromPaths(List.of(
                "/jcr:content/jcr:title",
                "/jcr:content/root/container/container/text/text"
        ));

        assertEquals(2, catalog.getGroups().size());
        assertEquals(SlotGroup.Role.PAGE_TITLE, catalog.getGroups().get(0).getRole());
        assertEquals("/jcr:content/jcr:title", catalog.getGroups().get(0).getContentPath());
        assertEquals(SlotGroup.Role.RICH_TEXT, catalog.getGroups().get(1).getRole());
        assertEquals(
                "/jcr:content/root/container/container/text/text",
                catalog.getGroups().get(1).getContentPath());
        assertTrue(catalog.isDumpShape());
    }

    @Test
    void groupsSatellitesOnSameComponent() {
        SlotCatalog catalog = SlotCatalog.fromPaths(List.of(
                "/jcr:content/jcr:title",
                "/jcr:content/root/container/container/text/text",
                "/jcr:content/root/container/container/text/textIsRich"
        ));

        SlotGroup text = catalog.getGroups().get(1);
        assertEquals(SlotGroup.Role.RICH_TEXT, text.getRole());
        assertEquals(
                "/jcr:content/root/container/container/text/textIsRich",
                text.getSatellitePaths().get("textIsRich"));
        assertTrue(catalog.isDumpShape());
    }

    @Test
    void infersHeadingListAndRichTextWithoutProjectSpecificNames() {
        SlotCatalog catalog = SlotCatalog.fromPaths(List.of(
                "/jcr:content/jcr:title",
                "/jcr:content/root/container/comp_a/body",
                "/jcr:content/root/container/comp_b/text",
                "/jcr:content/root/container/comp_b/level",
                "/jcr:content/root/container/comp_b/anchorId",
                "/jcr:content/root/container/comp_c/listType",
                "/jcr:content/root/container/comp_d/body"
        ));

        assertEquals(SlotGroup.Role.RICH_TEXT, catalog.getGroups().get(1).getRole());
        SlotGroup heading = catalog.getGroups().get(2);
        assertEquals(SlotGroup.Role.HEADING, heading.getRole());
        assertEquals("/jcr:content/root/container/comp_b/text", heading.getContentPath());
        assertEquals("/jcr:content/root/container/comp_b/level", heading.getSatellitePaths().get("level"));
        SlotGroup list = catalog.getGroups().get(3);
        assertEquals(SlotGroup.Role.LIST, list.getRole());
        assertNull(list.getContentPath());
        assertEquals(SlotGroup.Role.RICH_TEXT, catalog.getGroups().get(4).getRole());
        assertTrue(!catalog.isDumpShape());
    }
}
