package com.aem.bulkauthoring.docadapt;

import com.aem.bulkauthoring.docadapt.extract.SourceDocumentNormalizer;
import com.aem.bulkauthoring.docadapt.mapping.SourceMappingLoader;
import com.aem.bulkauthoring.docadapt.model.NormalizedDocument;
import com.aem.bulkauthoring.docadapt.review.AdaptReviewModel;
import com.aem.bulkauthoring.docadapt.strategy.ExtractedSlot;
import com.aem.bulkauthoring.docadapt.strategy.RuleEngineStrategy;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocAdaptServiceTest {

    private static final File USB_SOURCE =
            new File("input/raw-source-docs/USB new content hub page1.docx");
    private static final File TEMPLATE = new File("input/templates/normal-page.docx");

    @Test
    void usbMappingExtractsTitleAndHtmlBody() throws Exception {
        assertTrue(USB_SOURCE.isFile(), "USB sample source missing");
        assertTrue(TEMPLATE.isFile(), "normal-page template missing");

        var mapping = SourceMappingLoader.loadById("usb-content-hub-normal-page");
        NormalizedDocument doc = new SourceDocumentNormalizer().normalize(USB_SOURCE);
        List<ExtractedSlot> slots = new RuleEngineStrategy().apply(doc, mapping);

        assertEquals(2, slots.size());
        ExtractedSlot title = slots.get(0);
        assertEquals("/jcr:content/jcr:title", title.getPath());
        assertEquals(
                "How manufacturers can remain competitive in uncertain times",
                title.getValue());

        ExtractedSlot body = slots.get(1);
        assertEquals("/jcr:content/root/container/container/text/text", body.getPath());
        assertTrue(body.getValue().contains("<p>During periods of turbulence"));
        assertTrue(body.getValue().contains("<h2>Fight uncertainty with optimization</h2>"));
        assertTrue(body.getValue().contains("<ul>"));
        assertTrue(body.getValue().contains("<li>Streamlining payment processes</li>"));
        assertFalse(body.getValue().toLowerCase().contains("disclosures"),
                "Body should stop before Disclosures");
    }

    @Test
    void adaptWritesFilledTemplateAndReview() throws Exception {
        assertTrue(USB_SOURCE.isFile());
        DocAdaptService service = new DocAdaptService();
        DocAdaptService.AdaptJobResult result =
                service.run("usb-content-hub-normal-page", List.of(USB_SOURCE));

        assertEquals(1, result.reviews.size());
        AdaptReviewModel review = result.reviews.get(0);
        assertEquals("USB new content hub page1.docx", review.getSourceFile());
        assertFalse(review.getSourcePlainText().isBlank());

        File adapted = new File("output/adapted-articles", review.getAdaptedFile());
        assertTrue(adapted.isFile());

        try (FileInputStream in = new FileInputStream(adapted);
             XWPFDocument document = new XWPFDocument(in)) {
            List<XWPFParagraph> paras = document.getParagraphs();
            String titleMarker = null;
            String titleValue = null;
            String bodyMarker = null;
            String bodyValue = null;
            String current = null;
            StringBuilder buf = new StringBuilder();
            for (XWPFParagraph p : paras) {
                String text = p.getText() == null ? "" : p.getText().trim();
                if (text.startsWith("[[") && text.endsWith("]]")) {
                    if ("/jcr:content/jcr:title".equals(current)) {
                        titleValue = buf.toString().trim();
                    } else if (current != null && current.contains("text/text")) {
                        bodyValue = buf.toString().trim();
                    }
                    current = text.substring(2, text.length() - 2);
                    buf.setLength(0);
                    if ("/jcr:content/jcr:title".equals(current)) {
                        titleMarker = text;
                    } else if (current.contains("text/text")) {
                        bodyMarker = text;
                    }
                } else if (current != null && !text.isEmpty()) {
                    if (buf.length() > 0) {
                        buf.append('\n');
                    }
                    buf.append(text);
                }
            }
            if ("/jcr:content/jcr:title".equals(current)) {
                titleValue = buf.toString().trim();
            } else if (current != null && current.contains("text/text")) {
                bodyValue = buf.toString().trim();
            }

            assertTrue(titleMarker != null && titleMarker.contains("jcr:title"));
            assertEquals(
                    "How manufacturers can remain competitive in uncertain times",
                    titleValue);
            assertTrue(bodyMarker != null && bodyMarker.contains("text/text"));
            assertTrue(bodyValue != null && bodyValue.contains("<h2>"));
        }

        File reviewJson = new File("output/adapted-articles",
                review.getAdaptedFile().replace(".docx", ".review.json"));
        assertTrue(reviewJson.isFile());
    }
}
