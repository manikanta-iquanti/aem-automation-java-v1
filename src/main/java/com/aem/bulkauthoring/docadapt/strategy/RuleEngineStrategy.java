package com.aem.bulkauthoring.docadapt.strategy;

import com.aem.bulkauthoring.docadapt.mapping.ExtractRule;
import com.aem.bulkauthoring.docadapt.mapping.SlotRule;
import com.aem.bulkauthoring.docadapt.mapping.SourceMapping;
import com.aem.bulkauthoring.docadapt.model.DocBlock;
import com.aem.bulkauthoring.docadapt.model.NormalizedDocument;
import com.aem.bulkauthoring.docadapt.model.ParagraphBlock;
import com.aem.bulkauthoring.docadapt.model.TableBlock;
import com.aem.bulkauthoring.docadapt.model.TableCell;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Interprets declarative extract rules. Source-family specifics live in JSON only.
 */
public final class RuleEngineStrategy implements AdaptStrategy {

    private static final int EXCERPT_MAX = 500;

    @Override
    public List<ExtractedSlot> apply(NormalizedDocument doc, SourceMapping mapping) {
        List<ExtractedSlot> out = new ArrayList<>();
        if (mapping.getSlots() == null) {
            return out;
        }
        for (SlotRule slot : mapping.getSlots()) {
            if (slot.getPath() == null || slot.getExtract() == null) {
                continue;
            }
            out.add(extract(doc, slot.getPath(), slot.getExtract()));
        }
        return out;
    }

    private ExtractedSlot extract(NormalizedDocument doc, String path, ExtractRule rule) {
        String type = rule.getType() == null ? "" : rule.getType().trim();
        switch (type) {
            case "tableRowValue":
                return tableRowValue(doc, path, rule);
            case "elementSequenceHtml":
                return elementSequenceHtml(doc, path, rule);
            case "headingBody":
                return headingBody(doc, path, rule);
            case "paragraphIndex":
                return paragraphIndex(doc, path, rule);
            case "literal":
                String lit = rule.getValue() == null ? "" : rule.getValue();
                return new ExtractedSlot(path, lit, lit);
            case "documentTitle":
                return documentTitle(doc, path);
            default:
                throw new IllegalArgumentException("Unknown extract type: " + type);
        }
    }

    private ExtractedSlot tableRowValue(NormalizedDocument doc, String path, ExtractRule rule) {
        String sectionNeedle = rule.getSectionContains();
        String labelNeedle = rule.getLabelContains();
        int valueCol = rule.getValueColumn() == null ? 1 : rule.getValueColumn();

        TableBlock section = findTableSection(doc, sectionNeedle);
        if (section == null) {
            return new ExtractedSlot(path, "", "");
        }
        for (List<TableCell> row : section.getRows()) {
            if (row.isEmpty()) {
                continue;
            }
            String label = row.get(0).joinedText();
            if (containsIgnoreCase(label, labelNeedle)) {
                String value = valueCol < row.size() ? row.get(valueCol).joinedText() : "";
                return new ExtractedSlot(path, value, truncate(value));
            }
        }
        return new ExtractedSlot(path, "", "");
    }

    private ExtractedSlot elementSequenceHtml(NormalizedDocument doc, String path, ExtractRule rule) {
        List<DocBlock> blocks = doc.getBlocks();
        int start = indexOfSection(blocks, rule.getStartAfterSectionContains());
        int stop = indexOfSection(blocks, rule.getStopBeforeSectionContains());
        if (start < 0) {
            return new ExtractedSlot(path, "", "");
        }
        int from = start + 1;
        int to = stop < 0 ? blocks.size() : stop;
        int elementCol = rule.getElementColumn() == null ? 0 : rule.getElementColumn();
        int contentCol = rule.getContentColumn() == null ? 1 : rule.getContentColumn();
        Map<String, String> tagMap = rule.getTagMap();

        StringBuilder html = new StringBuilder();
        StringBuilder excerpt = new StringBuilder();
        for (int i = from; i < to; i++) {
            DocBlock block = blocks.get(i);
            if (!(block instanceof TableBlock)) {
                continue;
            }
            TableBlock table = (TableBlock) block;
            if (isElementHeaderTable(table, elementCol, contentCol)) {
                appendElementRows(table, elementCol, contentCol, tagMap, html, excerpt);
            }
        }
        String value = html.toString().trim();
        return new ExtractedSlot(path, value, truncate(excerpt.length() > 0 ? excerpt.toString() : value));
    }

    private static boolean isElementHeaderTable(TableBlock table, int elementCol, int contentCol) {
        if (table.getRows().isEmpty()) {
            return false;
        }
        List<TableCell> header = table.getRows().get(0);
        if (elementCol >= header.size() || contentCol >= header.size()) {
            return false;
        }
        String el = header.get(elementCol).joinedText();
        String content = header.get(contentCol).joinedText();
        return containsIgnoreCase(el, "Element") && containsIgnoreCase(content, "Content");
    }

    private static void appendElementRows(TableBlock table,
                                          int elementCol,
                                          int contentCol,
                                          Map<String, String> tagMap,
                                          StringBuilder html,
                                          StringBuilder excerpt) {
        for (int r = 1; r < table.getRows().size(); r++) {
            List<TableCell> row = table.getRows().get(r);
            if (elementCol >= row.size() || contentCol >= row.size()) {
                continue;
            }
            String element = row.get(elementCol).joinedText().trim();
            TableCell contentCell = row.get(contentCol);
            String tag = resolveTag(element, tagMap);
            if (tag == null) {
                continue;
            }
            String fragment = toHtml(tag, contentCell);
            if (fragment.isEmpty()) {
                continue;
            }
            if (html.length() > 0) {
                html.append('\n');
            }
            html.append(fragment);
            if (excerpt.length() < EXCERPT_MAX) {
                if (excerpt.length() > 0) {
                    excerpt.append(' ');
                }
                excerpt.append(contentCell.joinedText());
            }
        }
    }

    private static String resolveTag(String element, Map<String, String> tagMap) {
        if (tagMap == null || tagMap.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, String> e : tagMap.entrySet()) {
            if (containsIgnoreCase(element, e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    private static String toHtml(String tag, TableCell cell) {
        List<String> paras = cell.getParagraphs();
        if (paras.isEmpty()) {
            return "";
        }
        String t = tag.toLowerCase(Locale.ROOT);
        if ("ul".equals(t) || "ol".equals(t)) {
            StringBuilder sb = new StringBuilder();
            sb.append('<').append(t).append('>');
            for (String item : paras) {
                sb.append("<li>").append(escapeXml(item.trim())).append("</li>");
            }
            sb.append("</").append(t).append('>');
            return sb.toString();
        }
        if ("p".equals(t)) {
            StringBuilder sb = new StringBuilder();
            for (String para : paras) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append("<p>").append(escapeXml(para.trim())).append("</p>");
            }
            return sb.toString();
        }
        // h1/h2/... or any other single wrapper: join paragraphs with space
        String joined = String.join(" ", paras).trim();
        return "<" + t + ">" + escapeXml(joined) + "</" + t + ">";
    }

    private ExtractedSlot headingBody(NormalizedDocument doc, String path, ExtractRule rule) {
        String headingNeedle = rule.getHeading();
        List<String> collected = new ArrayList<>();
        boolean capturing = false;
        for (DocBlock block : doc.getBlocks()) {
            if (block instanceof ParagraphBlock) {
                ParagraphBlock p = (ParagraphBlock) block;
                if (p.isHeading() || looksLikeHeadingLine(p.getText())) {
                    if (capturing) {
                        break;
                    }
                    if (containsIgnoreCase(p.getText(), headingNeedle)) {
                        capturing = true;
                    }
                    continue;
                }
                if (capturing) {
                    collected.add(p.getText());
                }
            } else if (block instanceof TableBlock && capturing) {
                collected.add(block.toPlainText());
            }
        }
        String join = rule.getJoin() == null ? "paragraphs" : rule.getJoin();
        String value = "paragraphs".equals(join)
                ? String.join("\n", collected)
                : String.join(" ", collected);
        return new ExtractedSlot(path, value.trim(), truncate(value));
    }

    private ExtractedSlot paragraphIndex(NormalizedDocument doc, String path, ExtractRule rule) {
        int index = rule.getIndex() == null ? 0 : rule.getIndex();
        List<String> paras = new ArrayList<>();
        for (DocBlock block : doc.getBlocks()) {
            if (block instanceof ParagraphBlock) {
                String t = ((ParagraphBlock) block).getText();
                if (!t.isBlank()) {
                    paras.add(t);
                }
            }
        }
        String value = (index >= 0 && index < paras.size()) ? paras.get(index) : "";
        return new ExtractedSlot(path, value, truncate(value));
    }

    private ExtractedSlot documentTitle(NormalizedDocument doc, String path) {
        for (DocBlock block : doc.getBlocks()) {
            if (block instanceof ParagraphBlock) {
                String t = ((ParagraphBlock) block).getText().trim();
                if (t.length() >= 3) {
                    return new ExtractedSlot(path, t, truncate(t));
                }
            }
        }
        String stem = doc.getFileName();
        if (stem.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            stem = stem.substring(0, stem.length() - 5);
        }
        return new ExtractedSlot(path, stem, stem);
    }

    private static TableBlock findTableSection(NormalizedDocument doc, String sectionNeedle) {
        for (DocBlock block : doc.getBlocks()) {
            if (block instanceof TableBlock) {
                TableBlock table = (TableBlock) block;
                if (containsIgnoreCase(table.headerText(), sectionNeedle)
                        || tableContains(table, sectionNeedle)) {
                    return table;
                }
            }
        }
        return null;
    }

    private static boolean tableContains(TableBlock table, String needle) {
        for (List<TableCell> row : table.getRows()) {
            for (TableCell cell : row) {
                if (containsIgnoreCase(cell.joinedText(), needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int indexOfSection(List<DocBlock> blocks, String needle) {
        if (needle == null || needle.isBlank()) {
            return -1;
        }
        for (int i = 0; i < blocks.size(); i++) {
            DocBlock block = blocks.get(i);
            if (block instanceof TableBlock) {
                if (containsIgnoreCase(((TableBlock) block).headerText(), needle)) {
                    return i;
                }
            } else if (block instanceof ParagraphBlock) {
                if (containsIgnoreCase(((ParagraphBlock) block).getText(), needle)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean looksLikeHeadingLine(String text) {
        if (text == null || text.length() > 80) {
            return false;
        }
        String trimmed = text.trim();
        return trimmed.equals(trimmed.toUpperCase(Locale.ROOT)) && trimmed.length() > 2;
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        if (haystack == null || needle == null || needle.isBlank()) {
            return false;
        }
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.length() <= EXCERPT_MAX) {
            return t;
        }
        return t.substring(0, EXCERPT_MAX) + "…";
    }

    private static String escapeXml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
