package com.aem.bulkauthoring.docadapt;

import com.aem.bulkauthoring.docadapt.mapping.SlotBinding;
import com.aem.bulkauthoring.docadapt.review.AdaptReviewModel;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocAdaptAutoBindTest {

    private static final File USB_SOURCE =
            new File("input/raw-source-docs/USB new content hub page1.docx");
    private static final File NORMAL_PAGE = new File("input/templates/normal-page.docx");

    @TempDir
    Path temp;

    @Test
    void autoDetectBindsUsbSampleOntoNormalPageMarkers() throws Exception {
        DocAdaptService service = new DocAdaptService();
        DocAdaptService.AdaptJobResult result =
                service.runAuto(NORMAL_PAGE, "auto", List.of(USB_SOURCE));

        assertEquals(1, result.reviews.size());
        AdaptReviewModel review = result.reviews.get(0);
        assertEquals("usb-table-form", review.getRecipeId());
        assertEquals("dump", review.getBindStrategy());

        String title = valueAt(review, "/jcr:content/jcr:title");
        assertEquals("How manufacturers can remain competitive in uncertain times", title);
        String body = valueAt(review, "/jcr:content/root/container/container/text/text");
        assertTrue(body.contains("<p>During periods of turbulence"));
        assertTrue(body.contains("<h2>Fight uncertainty with optimization</h2>"));
        assertTrue(body.contains("<li>Streamlining payment processes</li>"));
    }

    @Test
    void wordOutlineDumpsOntoSingleRichTextTemplate() throws Exception {
        File source = writeWordOutline(temp.resolve("prose.docx").toFile());
        File template = writeTemplate(temp.resolve("simple.docx").toFile(), List.of(
                "/jcr:content/jcr:title",
                "/jcr:content/root/container/text/text"
        ));

        DocAdaptService service = new DocAdaptService();
        DocAdaptService.AdaptJobResult result =
                service.runAuto(template, "word-outline", List.of(source));

        AdaptReviewModel review = result.reviews.get(0);
        assertEquals("word-outline", review.getRecipeId());
        assertEquals("Widget launch", valueAt(review, "/jcr:content/jcr:title"));
        String body = valueAt(review, "/jcr:content/root/container/text/text");
        assertTrue(body.contains("<p>Intro paragraph about widgets.</p>"));
        assertTrue(body.contains("<h2>Why it matters</h2>"));
        assertTrue(body.contains("<li>First benefit</li>"));
    }

    @Test
    void sequentialBindsTypedBlocksToTypedMarkers() throws Exception {
        File source = writeWordOutline(temp.resolve("seq-src.docx").toFile());
        File template = writeTemplate(temp.resolve("seq.docx").toFile(), List.of(
                "/jcr:content/jcr:title",
                "/jcr:content/root/container/par/body",
                "/jcr:content/root/container/head/text",
                "/jcr:content/root/container/head/level",
                "/jcr:content/root/container/head/anchorId",
                "/jcr:content/root/container/lst/listType",
                "/jcr:content/root/container/par2/body"
        ));

        DocAdaptService service = new DocAdaptService();
        DocAdaptService.AdaptJobResult result =
                service.runAuto(template, "word-outline", List.of(source));

        AdaptReviewModel review = result.reviews.get(0);
        assertEquals("sequential", review.getBindStrategy());
        assertEquals("Widget launch", valueAt(review, "/jcr:content/jcr:title"));
        assertTrue(valueAt(review, "/jcr:content/root/container/par/body")
                .contains("Intro paragraph"));
        assertEquals("Why it matters", valueAt(review, "/jcr:content/root/container/head/text"));
        assertEquals("2", valueAt(review, "/jcr:content/root/container/head/level"));
        assertEquals("ul", valueAt(review, "/jcr:content/root/container/lst/listType"));
        assertTrue(valueAt(review, "/jcr:content/root/container/par2/body").contains("<li>"));
    }

    @Test
    void saveMappingWritesRecipeAndBindings() throws Exception {
        File saved = new File("input/source-mappings/test-word-outline-simple.json");
        saved.delete();
        try {
            DocAdaptService service = new DocAdaptService();
            saved = service.saveMapping(
                    "test-word-outline-simple",
                    "input/templates/normal-page.docx",
                    "word-outline",
                    "dump",
                    List.of(new SlotBinding("title", "/jcr:content/jcr:title")));
            assertTrue(saved.isFile());
            var loaded = com.aem.bulkauthoring.docadapt.mapping.SourceMappingLoader
                    .loadById("test-word-outline-simple");
            assertEquals("word-outline", loaded.getSourceRecipe());
            assertEquals(1, loaded.getBindings().size());
        } finally {
            saved.delete();
        }
    }

    @Test
    void rebindOverridesTitleSlot() throws Exception {
        File source = writeWordOutline(temp.resolve("rebind-src.docx").toFile());
        File template = writeTemplate(temp.resolve("rebind.docx").toFile(), List.of(
                "/jcr:content/jcr:title",
                "/jcr:content/root/container/text/text"
        ));
        DocAdaptService service = new DocAdaptService();
        DocAdaptService.AdaptJobResult first =
                service.runAuto(template, "word-outline", List.of(source));

        List<SlotBinding> bindings = List.of(
                new SlotBinding("blocksHtml", "/jcr:content/jcr:title"),
                new SlotBinding("title", "/jcr:content/root/container/text/text")
        );
        DocAdaptService.AdaptJobResult rebound = service.rebind(first.jobId, bindings);
        AdaptReviewModel review = rebound.reviews.get(0);
        assertEquals("Widget launch", valueAt(review, "/jcr:content/root/container/text/text"));
        assertTrue(valueAt(review, "/jcr:content/jcr:title").contains("Intro paragraph"));
    }

    private static String valueAt(AdaptReviewModel review, String path) {
        return review.getSlots().stream()
                .filter(s -> path.equals(s.getPath()))
                .map(AdaptReviewModel.SlotReview::getValue)
                .findFirst()
                .orElse("");
    }

    private static File writeWordOutline(File file) throws Exception {
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(file)) {
            para(doc, "Heading1", "Widget launch");
            para(doc, "Normal", "Intro paragraph about widgets.");
            para(doc, "Heading2", "Why it matters");
            para(doc, "Normal", "- First benefit");
            para(doc, "Normal", "- Second benefit");
            para(doc, "Normal", "Closing thought.");
            doc.write(out);
        }
        return file;
    }

    private static File writeTemplate(File file, List<String> paths) throws Exception {
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(file)) {
            for (String path : paths) {
                para(doc, null, "[[" + path + "]]");
                para(doc, null, "seed");
                doc.createParagraph();
            }
            doc.write(out);
        }
        return file;
    }

    private static void para(XWPFDocument doc, String style, String text) {
        XWPFParagraph p = doc.createParagraph();
        if (style != null) {
            p.setStyle(style);
        }
        p.createRun().setText(text);
    }
}
