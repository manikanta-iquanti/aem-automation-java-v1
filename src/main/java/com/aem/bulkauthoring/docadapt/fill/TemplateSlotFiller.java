package com.aem.bulkauthoring.docadapt.fill;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clones a target template and replaces value paragraphs under [[path]] markers.
 * Unmapped slots are cleared (empty value) so stale seed text is not left behind.
 */
public final class TemplateSlotFiller {

    public void fill(File templateDocx, Map<String, String> pathToValue, File outputDocx)
            throws IOException {
        if (outputDocx.getParentFile() != null) {
            outputDocx.getParentFile().mkdirs();
        }

        byte[] templateBytes;
        try (FileInputStream in = new FileInputStream(templateDocx);
             ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
            in.transferTo(buf);
            templateBytes = buf.toByteArray();
        }

        Map<String, String> values = new HashMap<>();
        if (pathToValue != null) {
            values.putAll(pathToValue);
        }

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(templateBytes))) {
            List<XWPFParagraph> paragraphs = new ArrayList<>(document.getParagraphs());
            String currentPath = null;
            List<XWPFParagraph> valueParas = new ArrayList<>();

            for (XWPFParagraph p : paragraphs) {
                String text = p.getText() == null ? "" : p.getText().trim();
                if (TemplateSlotDiscovery.isMarker(text)) {
                    flushSlot(currentPath, valueParas, values);
                    currentPath = text.substring(2, text.length() - 2);
                    valueParas = new ArrayList<>();
                } else if (currentPath != null) {
                    valueParas.add(p);
                }
            }
            flushSlot(currentPath, valueParas, values);

            try (FileOutputStream out = new FileOutputStream(outputDocx)) {
                document.write(out);
            }
        }
    }

    private static void flushSlot(String path,
                                  List<XWPFParagraph> valueParas,
                                  Map<String, String> values) {
        if (path == null || valueParas.isEmpty()) {
            return;
        }
        String value = values.getOrDefault(path, "");
        String[] lines = value.isEmpty() ? new String[]{""} : value.split("\n", -1);

        XWPFParagraph first = valueParas.get(0);
        clearRuns(first);
        XWPFRun run = first.createRun();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                run.addBreak();
            }
            run.setText(lines[i] == null ? "" : lines[i]);
        }
        for (int i = 1; i < valueParas.size(); i++) {
            clearRuns(valueParas.get(i));
        }
    }

    private static void clearRuns(XWPFParagraph p) {
        int n = p.getRuns().size();
        for (int i = n - 1; i >= 0; i--) {
            p.removeRun(i);
        }
    }
}
