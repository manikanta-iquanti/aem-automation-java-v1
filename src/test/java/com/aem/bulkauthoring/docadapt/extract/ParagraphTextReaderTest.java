package com.aem.bulkauthoring.docadapt.extract;

import com.aem.bulkauthoring.docadapt.content.ContentDocument;
import com.aem.bulkauthoring.docadapt.model.FormattedText;
import com.aem.bulkauthoring.docadapt.model.NormalizedDocument;
import com.aem.bulkauthoring.docadapt.recipe.UsbTableFormRecipe;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFldChar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParagraphTextReaderTest {

    private static final File USB_SOURCE =
            new File("input/raw-source-docs/USB new content hub page1.docx");

    @TempDir
    Path temp;

    @Test
    void readsNormalExternalHyperlink() throws Exception {
        File file = writeDocx(temp.resolve("linked.docx").toFile(), doc -> {
            XWPFParagraph p = doc.createParagraph();
            p.createRun().setText("See ");
            XWPFHyperlinkRun link = p.createHyperlinkRun("https://example.com/page");
            link.setText("Example");
            p.createRun().setText(" for details.");
        });

        try (FileInputStream in = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(in)) {
            FormattedText formatted = ParagraphTextReader.read(document.getParagraphs().get(0));
            assertEquals("See Example for details.", formatted.plainText());
            assertEquals(
                    "See <a href=\"https://example.com/page\">Example</a> for details.",
                    formatted.toHtml());
        }
    }

    @Test
    void readsLegacyHyperlinkField() throws Exception {
        File file = writeDocx(temp.resolve("field-link.docx").toFile(), doc -> {
            XWPFParagraph p = doc.createParagraph();
            p.createRun().setText("Please ");
            addHyperlinkField(p, "https://www.example.com/contact", "Contact a specialist");
            p.createRun().setText(" today.");
        });

        try (FileInputStream in = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(in)) {
            FormattedText formatted = ParagraphTextReader.read(document.getParagraphs().get(0));
            assertEquals("Please Contact a specialist today.", formatted.plainText());
            assertEquals(
                    "Please <a href=\"https://www.example.com/contact\">Contact a specialist</a> today.",
                    formatted.toHtml());
        }
    }

    @Test
    void unsafeSchemesStayPlainText() {
        FormattedText unsafe = FormattedText.of(java.util.List.of(
                FormattedText.Segment.link("Click me", "javascript:alert(1)")));
        assertEquals("Click me", unsafe.plainText());
        assertEquals("Click me", unsafe.toHtml());
        assertFalse(unsafe.toHtml().contains("<a "));

        FormattedText bookmark = FormattedText.of(java.util.List.of(
                FormattedText.Segment.link("Jump", "#section-2")));
        assertEquals("Jump", bookmark.toHtml());
    }

    @Test
    void usbSampleKeepsKnownExternalLinks() throws Exception {
        assertTrue(USB_SOURCE.isFile(), "USB sample source missing");
        NormalizedDocument doc = new SourceDocumentNormalizer().normalize(USB_SOURCE);
        ContentDocument content = new UsbTableFormRecipe().extract(doc);
        String html = content.blocksHtml();

        assertTrue(html.contains(
                "<a href=\"https://www.usbank.com/business-banking/business-resource-center/business-life-stages.html\">"
                        + "practical steps for business optimization</a>"));
        assertTrue(html.contains(
                "<a href=\"https://www.usbank.com/corporate-and-commercial-banking/insights/credit-finance/equipment/maximize-deductions-section-179.html\">"
                        + "Section 179</a>"));
        assertTrue(html.contains(
                "<a href=\"https://www.usbank.com/business-banking/business-lending/business-loans/business-equipment-financing/contact-equipment-finance.html\">"
                        + "Contact a manufacturing specialist</a>"));
        assertFalse(html.toLowerCase().contains("javascript:"));
    }

    private static File writeDocx(File file, DocWriter writer) throws Exception {
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(file)) {
            writer.write(doc);
            doc.write(out);
        }
        return file;
    }

    private static void addHyperlinkField(XWPFParagraph paragraph, String url, String display) {
        XWPFRun begin = paragraph.createRun();
        CTFldChar beginChar = begin.getCTR().addNewFldChar();
        beginChar.setFldCharType(STFldCharType.BEGIN);

        XWPFRun instr = paragraph.createRun();
        CTText instrText = instr.getCTR().addNewInstrText();
        instrText.setStringValue(" HYPERLINK \"" + url + "\" ");

        XWPFRun sep = paragraph.createRun();
        CTFldChar sepChar = sep.getCTR().addNewFldChar();
        sepChar.setFldCharType(STFldCharType.SEPARATE);

        XWPFRun text = paragraph.createRun();
        text.setText(display);

        XWPFRun end = paragraph.createRun();
        CTFldChar endChar = end.getCTR().addNewFldChar();
        endChar.setFldCharType(STFldCharType.END);
    }

    @FunctionalInterface
    private interface DocWriter {
        void write(XWPFDocument doc) throws Exception;
    }
}
