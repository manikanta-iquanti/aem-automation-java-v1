package com.aem.bulkauthoring.docadapt.fill;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateSlotFillerTest {

    private static final String ARTICLE =
            "/jcr:content/root/responsivegrid/advancedlayout_copy/leftPar/textmodule_copy/text";

    @TempDir
    Path temp;

    @Test
    void repeatsArticleMarkerBlockForEachExtraSectionInOrder() throws Exception {
        File template = temp.resolve("t.docx").toFile();
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(template)) {
            para(doc, "[[/jcr:content/jcr:title]]");
            para(doc, "seed title");
            para(doc, "[[" + ARTICLE + "]]");
            para(doc, "seed body");
            para(doc, "[[/jcr:content/root/footer/text]]");
            para(doc, "seed footer");
            doc.write(out);
        }

        Map<String, String> values = new LinkedHashMap<>();
        values.put("/jcr:content/jcr:title", "Title");
        values.put(ARTICLE, "<p>One</p>");
        values.put(ARTICLE + "#2", "<p>Two</p>");
        values.put(ARTICLE + "#3", "<p>Three</p>");
        values.put("/jcr:content/root/footer/text", "Foot");

        File output = temp.resolve("out.docx").toFile();
        new TemplateSlotFiller().fill(template, values, output);

        assertEquals(List.of(
                "[[/jcr:content/jcr:title]]", "Title",
                "[[" + ARTICLE + "]]", "<p>One</p>",
                "[[" + ARTICLE + "#2]]", "<p>Two</p>",
                "[[" + ARTICLE + "#3]]", "<p>Three</p>",
                "[[/jcr:content/root/footer/text]]", "Foot"
        ), texts(output));
        assertEquals(List.of(
                "/jcr:content/jcr:title",
                ARTICLE, ARTICLE + "#2", ARTICLE + "#3",
                "/jcr:content/root/footer/text"
        ), TemplateSlotDiscovery.discoverPaths(output));
    }

    private static List<String> texts(File docx) throws Exception {
        List<String> out = new ArrayList<>();
        try (FileInputStream in = new FileInputStream(docx);
             XWPFDocument doc = new XWPFDocument(in)) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                out.add(p.getText());
            }
        }
        return out;
    }

    private static void para(XWPFDocument doc, String text) {
        doc.createParagraph().createRun().setText(text);
    }
}
