package com.aem.bulkauthoring.docadapt;

import com.aem.bulkauthoring.blueprint.FieldFormat;
import com.aem.bulkauthoring.docadapt.mapping.SlotBinding;
import com.aem.bulkauthoring.docadapt.review.AdaptReviewModel;
import com.aem.bulkauthoring.model.document.DocumentBlock;
import com.aem.bulkauthoring.parser.DocumentParser;
import com.aem.bulkauthoring.updater.BlueprintUpdater;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        assertEquals("sections", review.getBindStrategy());

        String title = valueAt(review, "/jcr:content/jcr:title");
        assertEquals("How manufacturers can remain competitive in uncertain times", title);
        String article = "/jcr:content/root/container/container/text/text";
        assertTrue(valueAt(review, article + "#2").length() > 0);
        String body = review.getSlots().stream()
                .filter(s -> s.getPath().startsWith(article))
                .map(AdaptReviewModel.SlotReview::getValue)
                .collect(java.util.stream.Collectors.joining("\n"));
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

    @Test
    void usbSectionsBecomeStackedTextModulesInBbUsbBlueprint() throws Exception {
        String leftPar = "/jcr:content/root/responsivegrid/advancedlayout_copy/leftPar";
        String article = leftPar + "/textmodule_copy/text";
        File template = writeTemplate(temp.resolve("bb-usb.docx").toFile(), List.of(article));

        DocAdaptService service = new DocAdaptService();
        DocAdaptService.AdaptJobResult result =
                service.runAuto(template, "auto", List.of(USB_SOURCE));
        AdaptReviewModel review = result.reviews.get(0);
        assertEquals("sections", review.getBindStrategy());
        File adapted = service.resolveAdaptedFile(result.jobId, review.getAdaptedFile());

        List<DocumentBlock> blocks = new DocumentParser().parse(adapted.getPath());
        assertTrue(blocks.size() > 1);
        assertEquals(article, blocks.get(0).getId());
        assertEquals(article + "#2", blocks.get(1).getId());

        File updated = temp.resolve("bb-usb-updated.json").toFile();
        new BlueprintUpdater().update(
                new File("input/blueprint/bb-usb.json"),
                blocks,
                updated,
                Map.of(article, FieldFormat.HTML),
                List.of(leftPar + "/textmodule",
                        leftPar + "/textmodule_559176784",
                        leftPar + "/textmodule_344092626"));

        JsonNode par = new ObjectMapper().readTree(updated).at(leftPar);
        List<String> components = new ArrayList<>();
        par.fieldNames().forEachRemaining(name -> {
            if (par.get(name).isObject()) {
                components.add(name);
            }
        });
        List<String> expected = new ArrayList<>(List.of("rightpar_copy", "textmodule_copy"));
        for (int n = 2; n <= blocks.size(); n++) {
            expected.add("textmodule_copy_" + n);
        }
        expected.add("fullspancontentblock");
        expected.add("imagemodule");
        assertEquals(expected, components);
        assertTrue(par.at("/textmodule_copy_2/text").asText().startsWith("<"));
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
