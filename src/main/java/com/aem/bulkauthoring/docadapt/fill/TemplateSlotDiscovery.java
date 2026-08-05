package com.aem.bulkauthoring.docadapt.fill;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Discovers [[path]] markers in a target template DOCX. */
public final class TemplateSlotDiscovery {

    public static final String MARKER_PREFIX = "[[";
    public static final String MARKER_SUFFIX = "]]";

    private TemplateSlotDiscovery() {
    }

    public static List<String> discoverPaths(File templateDocx) throws IOException {
        List<String> paths = new ArrayList<>();
        try (FileInputStream in = new FileInputStream(templateDocx);
             XWPFDocument document = new XWPFDocument(in)) {
            for (XWPFParagraph p : document.getParagraphs()) {
                String text = p.getText();
                if (text == null) {
                    continue;
                }
                text = text.trim();
                if (isMarker(text)) {
                    paths.add(text.substring(2, text.length() - 2));
                }
            }
        }
        return paths;
    }

    public static boolean isMarker(String text) {
        return text != null
                && text.startsWith(MARKER_PREFIX)
                && text.endsWith(MARKER_SUFFIX)
                && text.length() > 4;
    }
}
