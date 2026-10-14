package com.aem.bulkauthoring.docadapt.fill;

import com.aem.bulkauthoring.docadapt.bind.BindingEngine;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.XmlCursor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clones a target template and replaces value paragraphs under [[path]] markers.
 * Unmapped slots are cleared (empty value) so stale seed text is not left behind.
 * Values for {@code path#n} with no marker of their own get a copy of the {@code path}
 * marker block inserted after the previous section.
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
            List<Slot> slots = new ArrayList<>();
            Slot current = null;

            for (XWPFParagraph p : paragraphs) {
                String text = p.getText() == null ? "" : p.getText().trim();
                if (TemplateSlotDiscovery.isMarker(text)) {
                    current = new Slot(text.substring(2, text.length() - 2), p);
                    slots.add(current);
                } else if (current != null) {
                    current.valueParas.add(p);
                }
            }

            Set<String> markerPaths = new HashSet<>();
            for (Slot slot : slots) {
                markerPaths.add(slot.path);
            }
            for (Slot slot : slots) {
                flushSlot(slot.path, slot.valueParas, values);
                repeatSections(document, slot, values, markerPaths);
            }

            try (FileOutputStream out = new FileOutputStream(outputDocx)) {
                document.write(out);
            }
        }
    }

    private static void repeatSections(XWPFDocument document,
                                       Slot slot,
                                       Map<String, String> values,
                                       Set<String> markerPaths) {
        if (slot.path.contains(BindingEngine.SECTION_SEPARATOR) || slot.valueParas.isEmpty()) {
            return;
        }
        XWPFParagraph anchor = slot.valueParas.get(slot.valueParas.size() - 1);
        for (int n = 2; ; n++) {
            String path = BindingEngine.sectionPath(slot.path, n);
            if (!values.containsKey(path) || markerPaths.contains(path)) {
                return;
            }
            XWPFParagraph marker = insertAfter(document, anchor, slot.marker);
            setText(marker, "[[" + path + "]]");
            anchor = marker;
            List<XWPFParagraph> copies = new ArrayList<>();
            for (XWPFParagraph source : slot.valueParas) {
                XWPFParagraph copy = insertAfter(document, anchor, source);
                copies.add(copy);
                anchor = copy;
            }
            flushSlot(path, copies, values);
        }
    }

    private static XWPFParagraph insertAfter(XWPFDocument document,
                                             XWPFParagraph anchor,
                                             XWPFParagraph styleFrom) {
        XWPFParagraph created;
        try (XmlCursor cursor = anchor.getCTP().newCursor()) {
            created = cursor.toNextSibling()
                    ? document.insertNewParagraph(cursor)
                    : document.createParagraph();
        }
        if (styleFrom.getCTP().getPPr() != null) {
            created.getCTP().setPPr(styleFrom.getCTP().getPPr());
        }
        return created;
    }

    private static void setText(XWPFParagraph p, String text) {
        clearRuns(p);
        p.createRun().setText(text);
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

    private static final class Slot {
        final String path;
        final XWPFParagraph marker;
        final List<XWPFParagraph> valueParas = new ArrayList<>();

        Slot(String path, XWPFParagraph marker) {
            this.path = path;
            this.marker = marker;
        }
    }
}
