package com.aem.bulkauthoring.docadapt.bind;

import com.aem.bulkauthoring.docadapt.catalog.SlotCatalog;
import com.aem.bulkauthoring.docadapt.content.ContentDocument;
import com.aem.bulkauthoring.docadapt.content.ContentUnit;
import com.aem.bulkauthoring.docadapt.mapping.SlotBinding;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BindingEngineTest {

    @Test
    void dumpPutsTitleAndAllBlocksHtmlIntoSingleRichText() {
        ContentDocument content = new ContentDocument(
                "usb-table-form",
                "Page title",
                List.of(
                        ContentUnit.paragraph("Hello"),
                        ContentUnit.heading(2, "Next"),
                        ContentUnit.list(false, List.of("A", "B"))
                ));
        SlotCatalog catalog = SlotCatalog.fromPaths(List.of(
                "/jcr:content/jcr:title",
                "/jcr:content/root/container/text/text",
                "/jcr:content/root/container/text/textIsRich"
        ));

        BoundSlots bound = new BindingEngine().bind(content, catalog, List.of());

        assertEquals("Page title", bound.getPathToValue().get("/jcr:content/jcr:title"));
        String html = bound.getPathToValue().get("/jcr:content/root/container/text/text");
        assertTrue(html.contains("<p>Hello</p>"));
        assertTrue(html.contains("<h2>Next</h2>"));
        assertTrue(html.contains("<li>A</li>"));
        assertEquals("true", bound.getPathToValue().get("/jcr:content/root/container/text/textIsRich"));
        assertEquals("dump", bound.getBindStrategy());
    }

    @Test
    void sequentialFillsHeadingParagraphAndListInOrder() {
        ContentDocument content = new ContentDocument(
                "word-outline",
                "Article title",
                List.of(
                        ContentUnit.paragraph("Lead paragraph"),
                        ContentUnit.heading(2, "Section"),
                        ContentUnit.list(true, List.of("One", "Two")),
                        ContentUnit.paragraph("Tail")
                ));
        SlotCatalog catalog = SlotCatalog.fromPaths(List.of(
                "/jcr:content/jcr:title",
                "/jcr:content/root/container/par/body",
                "/jcr:content/root/container/head/text",
                "/jcr:content/root/container/head/level",
                "/jcr:content/root/container/head/anchorId",
                "/jcr:content/root/container/lst/listType",
                "/jcr:content/root/container/par2/body"
        ));

        BoundSlots bound = new BindingEngine().bind(content, catalog, List.of());
        Map<String, String> v = bound.getPathToValue();

        assertEquals("sequential", bound.getBindStrategy());
        assertEquals("Article title", v.get("/jcr:content/jcr:title"));
        assertEquals("<p>Lead paragraph</p>", v.get("/jcr:content/root/container/par/body"));
        assertEquals("Section", v.get("/jcr:content/root/container/head/text"));
        assertEquals("2", v.get("/jcr:content/root/container/head/level"));
        assertEquals("section", v.get("/jcr:content/root/container/head/anchorId"));
        assertEquals("ol", v.get("/jcr:content/root/container/lst/listType"));
        assertTrue(v.get("/jcr:content/root/container/par2/body").contains("<li>One</li>"));
        assertTrue(v.get("/jcr:content/root/container/par2/body").contains("<p>Tail</p>"));
    }

    @Test
    void explicitBindingsWinOverAuto() {
        ContentDocument content = new ContentDocument(
                "word-outline",
                "T",
                List.of(ContentUnit.paragraph("Only body")));
        SlotCatalog catalog = SlotCatalog.fromPaths(List.of(
                "/jcr:content/jcr:title",
                "/jcr:content/root/a/text"
        ));
        List<SlotBinding> overrides = List.of(
                bind("title", "/jcr:content/root/a/text"),
                bind("blocksHtml", "/jcr:content/jcr:title")
        );

        BoundSlots bound = new BindingEngine().bind(content, catalog, overrides);
        assertEquals("T", bound.getPathToValue().get("/jcr:content/root/a/text"));
        assertTrue(bound.getPathToValue().get("/jcr:content/jcr:title").contains("Only body"));
        assertEquals("manual", bound.getBindStrategy());
    }

    private static SlotBinding bind(String unit, String path) {
        SlotBinding b = new SlotBinding();
        b.setUnit(unit);
        b.setPath(path);
        return b;
    }
}
