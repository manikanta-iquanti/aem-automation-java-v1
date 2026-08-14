package com.aem.bulkauthoring.docadapt.recipe;

import com.aem.bulkauthoring.docadapt.content.ContentDocument;
import com.aem.bulkauthoring.docadapt.content.ContentUnit;
import com.aem.bulkauthoring.docadapt.extract.SourceDocumentNormalizer;
import com.aem.bulkauthoring.docadapt.model.NormalizedDocument;
import com.aem.bulkauthoring.docadapt.model.ParagraphBlock;
import com.aem.bulkauthoring.docadapt.model.TableBlock;
import com.aem.bulkauthoring.docadapt.model.TableCell;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceRecipeTest {

    private static final File USB_SOURCE =
            new File("input/raw-source-docs/USB new content hub page1.docx");

    @Test
    void usbRecipeExtractsTitleAndTypedBlocksFromRealSample() throws Exception {
        NormalizedDocument doc = new SourceDocumentNormalizer().normalize(USB_SOURCE);
        ContentDocument content = new UsbTableFormRecipe().extract(doc);

        assertEquals("usb-table-form", content.getRecipeId());
        assertEquals(
                "How manufacturers can remain competitive in uncertain times",
                content.getTitle());
        assertTrue(content.getBlocks().size() >= 3);
        assertTrue(content.getBlocks().stream()
                .anyMatch(b -> b.getRole() == ContentUnit.Role.HEADING
                        && b.getText().contains("Fight uncertainty with optimization")));
        assertTrue(content.getBlocks().stream()
                .anyMatch(b -> b.getRole() == ContentUnit.Role.PARAGRAPH
                        && b.getText().contains("During periods of turbulence")));
        assertTrue(content.getBlocks().stream()
                .anyMatch(b -> b.getRole() == ContentUnit.Role.LIST
                        && b.getItems().stream().anyMatch(i -> i.contains("Streamlining payment"))));
        assertTrue(content.blocksHtml().contains("<h2>Fight uncertainty with optimization</h2>"));
        assertTrue(content.blocksHtml().toLowerCase().contains("<ul>"));
        assertTrue(!content.blocksHtml().toLowerCase().contains("disclosures"));
    }

    @Test
    void wordOutlineRecipeUsesHeadingStylesAndLists() {
        NormalizedDocument doc = new NormalizedDocument("article.docx", List.of(
                new ParagraphBlock("Widget launch", "Heading1"),
                new ParagraphBlock("Intro paragraph about widgets.", "Normal"),
                new ParagraphBlock("Why it matters", "Heading2"),
                new ParagraphBlock("First benefit", "Normal", 0, true, false),
                new ParagraphBlock("Second benefit", "Normal", 0, true, false),
                new ParagraphBlock("Closing thought.", "Normal")
        ));

        ContentDocument content = new WordOutlineRecipe().extract(doc);

        assertEquals("word-outline", content.getRecipeId());
        assertEquals("Widget launch", content.getTitle());
        assertEquals(4, content.getBlocks().size());
        assertEquals(ContentUnit.Role.PARAGRAPH, content.getBlocks().get(0).getRole());
        assertEquals("Intro paragraph about widgets.", content.getBlocks().get(0).getText());
        assertEquals(ContentUnit.Role.HEADING, content.getBlocks().get(1).getRole());
        assertEquals(2, content.getBlocks().get(1).getLevel());
        assertEquals("Why it matters", content.getBlocks().get(1).getText());
        assertEquals(ContentUnit.Role.LIST, content.getBlocks().get(2).getRole());
        assertEquals(List.of("First benefit", "Second benefit"), content.getBlocks().get(2).getItems());
        assertEquals(ContentUnit.Role.PARAGRAPH, content.getBlocks().get(3).getRole());
    }

    @Test
    void detectorPicksUsbForTableFormsAndWordOutlineForProse() throws Exception {
        SourceRecipeDetector detector = new SourceRecipeDetector();
        NormalizedDocument usb = new SourceDocumentNormalizer().normalize(USB_SOURCE);
        assertEquals("usb-table-form", detector.resolve("auto", usb).id());

        NormalizedDocument prose = new NormalizedDocument("p.docx", List.of(
                new ParagraphBlock("A real title", "Heading1"),
                new ParagraphBlock("Body copy here.", "Normal"),
                new ParagraphBlock("Next heading", "Heading2"),
                new ParagraphBlock("More copy.", "Normal")
        ));
        assertEquals("word-outline", detector.resolve("auto", prose).id());
        assertEquals("word-outline", detector.resolve("word-outline", usb).id());
    }

    @Test
    void usbRecipeReadsInMemoryElementTables() {
        TableBlock headline = new TableBlock(List.of(
                List.of(cell("Headline")),
                List.of(cell("*H1"), cell("Sample headline"))
        ));
        TableBlock bodyHeader = new TableBlock(List.of(
                List.of(cell("*Body"))
        ));
        TableBlock elements = new TableBlock(List.of(
                List.of(cell("Element"), cell("Content")),
                List.of(cell("p-text"), cell("Hello body")),
                List.of(cell("H2"), cell("Section two")),
                List.of(cell("Unordered list"), new TableCell(List.of("Alpha", "Beta")))
        ));
        TableBlock disclosure = new TableBlock(List.of(
                List.of(cell("Disclosures")),
                List.of(cell("Legal"), cell("Do not include"))
        ));
        NormalizedDocument doc = new NormalizedDocument("t.docx",
                List.of(headline, bodyHeader, elements, disclosure));

        ContentDocument content = new UsbTableFormRecipe().extract(doc);
        assertEquals("Sample headline", content.getTitle());
        assertEquals(3, content.getBlocks().size());
        assertEquals("Hello body", content.getBlocks().get(0).getText());
        assertEquals(ContentUnit.Role.HEADING, content.getBlocks().get(1).getRole());
        assertEquals(List.of("Alpha", "Beta"), content.getBlocks().get(2).getItems());
        assertTrue(!content.blocksHtml().contains("Do not include"));
    }

    private static TableCell cell(String text) {
        return new TableCell(List.of(text));
    }
}
