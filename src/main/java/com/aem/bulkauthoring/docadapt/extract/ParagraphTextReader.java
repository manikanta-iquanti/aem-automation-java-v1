package com.aem.bulkauthoring.docadapt.extract;

import com.aem.bulkauthoring.docadapt.model.FormattedText;
import com.aem.bulkauthoring.docadapt.model.FormattedText.Segment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlink;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a Word paragraph as ordered plain/link segments, including legacy HYPERLINK fields.
 */
public final class ParagraphTextReader {

    private static final String W_NS =
            "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String R_NS =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final Pattern HYPERLINK_URL =
            Pattern.compile("(?i)HYPERLINK\\s+\"([^\"]+)\"");

    private ParagraphTextReader() {
    }

    public static FormattedText read(XWPFParagraph paragraph) {
        if (paragraph == null) {
            return FormattedText.empty();
        }
        CTP ctp = paragraph.getCTP();
        if (ctp == null) {
            return FormattedText.empty();
        }
        XWPFDocument document = paragraph.getDocument();
        List<Segment> segments = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        FieldState field = new FieldState();

        try (XmlCursor cursor = ctp.newCursor()) {
            if (!cursor.toFirstChild()) {
                return FormattedText.empty();
            }
            do {
                QName name = cursor.getName();
                if (name == null) {
                    continue;
                }
                String local = name.getLocalPart();
                if ("hyperlink".equals(local)) {
                    flushPlain(plain, segments);
                    appendHyperlinkElement(cursor, document, segments);
                } else if ("r".equals(local)) {
                    walkRun(cursor, plain, segments, field);
                } else if ("fldSimple".equals(local)) {
                    flushPlain(plain, segments);
                    appendFldSimple(cursor, segments);
                }
            } while (cursor.toNextSibling());
        }
        flushPlain(plain, segments);
        if (field.active && field.display.length() > 0) {
            appendFieldLink(field, segments);
        }
        return FormattedText.of(segments).trim();
    }

    private static void walkRun(XmlCursor runCursor,
                                StringBuilder plain,
                                List<Segment> segments,
                                FieldState field) {
        try (XmlCursor cursor = runCursor.newCursor()) {
            if (!cursor.toFirstChild()) {
                return;
            }
            do {
                QName name = cursor.getName();
                if (name == null) {
                    continue;
                }
                String local = name.getLocalPart();
                if ("fldChar".equals(local)) {
                    String type = cursor.getAttributeText(new QName(W_NS, "fldCharType"));
                    if ("begin".equals(type)) {
                        flushPlain(plain, segments);
                        field.reset();
                        field.active = true;
                        field.phase = FieldPhase.INSTRUCTION;
                    } else if ("separate".equals(type)) {
                        field.phase = FieldPhase.DISPLAY;
                    } else if ("end".equals(type)) {
                        if (field.active) {
                            appendFieldLink(field, segments);
                        }
                        field.reset();
                    }
                } else if ("instrText".equals(local) || "delInstrText".equals(local)) {
                    if (field.active && field.phase == FieldPhase.INSTRUCTION) {
                        field.instruction.append(textValue(cursor));
                    }
                } else if ("t".equals(local)) {
                    String text = textValue(cursor);
                    if (field.active && field.phase == FieldPhase.DISPLAY) {
                        field.display.append(text);
                    } else if (!field.active) {
                        plain.append(text);
                    }
                } else if ("tab".equals(local)) {
                    if (field.active && field.phase == FieldPhase.DISPLAY) {
                        field.display.append('\t');
                    } else if (!field.active) {
                        plain.append('\t');
                    }
                } else if ("br".equals(local) || "cr".equals(local)) {
                    if (field.active && field.phase == FieldPhase.DISPLAY) {
                        field.display.append('\n');
                    } else if (!field.active) {
                        plain.append('\n');
                    }
                }
            } while (cursor.toNextSibling());
        }
    }

    private static void appendHyperlinkElement(XmlCursor hyperlinkCursor,
                                               XWPFDocument document,
                                               List<Segment> segments) {
        String rid = hyperlinkCursor.getAttributeText(new QName(R_NS, "id"));
        String href = resolveExternalUrl(document, rid);
        String text = collectDescendantText(hyperlinkCursor);
        if (text.isEmpty()) {
            return;
        }
        if (href != null && FormattedText.isSafeHref(href)) {
            segments.add(Segment.link(text, href.trim()));
        } else {
            segments.add(Segment.plain(text));
        }
    }

    private static void appendFldSimple(XmlCursor fldCursor, List<Segment> segments) {
        String instr = fldCursor.getAttributeText(new QName(W_NS, "instr"));
        if (instr == null) {
            instr = "";
        }
        String href = extractHyperlinkUrl(instr);
        String text = collectDescendantText(fldCursor);
        if (text.isEmpty()) {
            return;
        }
        if (href != null && FormattedText.isSafeHref(href)) {
            segments.add(Segment.link(text, href.trim()));
        } else {
            segments.add(Segment.plain(text));
        }
    }

    private static void appendFieldLink(FieldState field, List<Segment> segments) {
        String href = extractHyperlinkUrl(field.instruction.toString());
        String text = field.display.toString();
        if (text.isEmpty()) {
            return;
        }
        if (href != null && FormattedText.isSafeHref(href)) {
            segments.add(Segment.link(text, href.trim()));
        } else {
            segments.add(Segment.plain(text));
        }
    }

    private static String resolveExternalUrl(XWPFDocument document, String rid) {
        if (document == null || rid == null || rid.isBlank()) {
            return null;
        }
        XWPFHyperlink link = document.getHyperlinkByID(rid);
        if (link == null) {
            return null;
        }
        return link.getURL();
    }

    private static String extractHyperlinkUrl(String instruction) {
        if (instruction == null || instruction.isBlank()) {
            return null;
        }
        Matcher matcher = HYPERLINK_URL.matcher(instruction.trim());
        if (!matcher.find()) {
            String upper = instruction.toUpperCase(Locale.ROOT);
            if (!upper.contains("HYPERLINK")) {
                return null;
            }
            return null;
        }
        return matcher.group(1);
    }

    private static String collectDescendantText(XmlCursor start) {
        StringBuilder sb = new StringBuilder();
        try (XmlCursor cursor = start.newCursor()) {
            cursor.selectPath("declare namespace w='" + W_NS + "'; .//w:t");
            while (cursor.toNextSelection()) {
                sb.append(textValue(cursor));
            }
        }
        return sb.toString();
    }

    private static String textValue(XmlCursor cursor) {
        String value = cursor.getTextValue();
        return value == null ? "" : value;
    }

    private static void flushPlain(StringBuilder plain, List<Segment> segments) {
        if (plain.length() == 0) {
            return;
        }
        segments.add(Segment.plain(plain.toString()));
        plain.setLength(0);
    }

    private enum FieldPhase {
        INSTRUCTION,
        DISPLAY
    }

    private static final class FieldState {
        boolean active;
        FieldPhase phase = FieldPhase.INSTRUCTION;
        final StringBuilder instruction = new StringBuilder();
        final StringBuilder display = new StringBuilder();

        void reset() {
            active = false;
            phase = FieldPhase.INSTRUCTION;
            instruction.setLength(0);
            display.setLength(0);
        }
    }
}
