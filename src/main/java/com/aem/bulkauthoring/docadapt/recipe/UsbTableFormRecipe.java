package com.aem.bulkauthoring.docadapt.recipe;

import com.aem.bulkauthoring.docadapt.content.ContentDocument;
import com.aem.bulkauthoring.docadapt.content.ContentUnit;
import com.aem.bulkauthoring.docadapt.content.DocTexts;
import com.aem.bulkauthoring.docadapt.model.DocBlock;
import com.aem.bulkauthoring.docadapt.model.NormalizedDocument;
import com.aem.bulkauthoring.docadapt.model.ParagraphBlock;
import com.aem.bulkauthoring.docadapt.model.TableBlock;
import com.aem.bulkauthoring.docadapt.model.TableCell;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * USB-style publishing forms: Headline/*H1 tables and Element/Content body tables.
 */
public final class UsbTableFormRecipe implements SourceRecipe {

    static final String ID = "usb-table-form";
    private static final Pattern HEADING_ELEMENT = Pattern.compile("(?i)^h\\s*([1-6])\\b");

    @Override
    public String id() {
        return ID;
    }

    @Override
    public int score(NormalizedDocument doc) {
        int score = 0;
        if (hasElementContentTable(doc)) {
            score += 50;
        }
        if (findHeadlineTitle(doc) != null) {
            score += 40;
        }
        if (indexOfSection(doc.getBlocks(), "*Body") >= 0
                || indexOfSection(doc.getBlocks(), "Body") >= 0) {
            score += 20;
        }
        return score;
    }

    @Override
    public ContentDocument extract(NormalizedDocument doc) {
        String title = findHeadlineTitle(doc);
        if (title == null) {
            title = "";
        }
        List<ContentUnit> blocks = extractBodyBlocks(doc);
        return new ContentDocument(ID, title, blocks);
    }

    private static String findHeadlineTitle(NormalizedDocument doc) {
        TableBlock section = findTableSection(doc, "Headline");
        if (section == null) {
            section = findTableSection(doc, "*H1");
        }
        if (section == null) {
            return null;
        }
        for (List<TableCell> row : section.getRows()) {
            if (row.isEmpty()) {
                continue;
            }
            String label = row.get(0).joinedText();
            if (DocTexts.containsIgnoreCase(label, "*H1")
                    || DocTexts.containsIgnoreCase(label, "H1")) {
                return row.size() > 1 ? row.get(1).joinedText().trim() : "";
            }
        }
        return null;
    }

    private static List<ContentUnit> extractBodyBlocks(NormalizedDocument doc) {
        List<DocBlock> blocks = doc.getBlocks();
        int start = indexOfSection(blocks, "*Body");
        if (start < 0) {
            start = indexOfSection(blocks, "Body");
        }
        int stop = indexOfSection(blocks, "Disclosure");
        int from = start < 0 ? 0 : start + 1;
        int to = stop < 0 ? blocks.size() : stop;

        List<ContentUnit> out = new ArrayList<>();
        for (int i = from; i < to; i++) {
            DocBlock block = blocks.get(i);
            if (!(block instanceof TableBlock)) {
                continue;
            }
            TableBlock table = (TableBlock) block;
            if (!isElementHeaderTable(table)) {
                continue;
            }
            appendElementRows(table, out);
        }
        return out;
    }

    private static boolean hasElementContentTable(NormalizedDocument doc) {
        for (DocBlock block : doc.getBlocks()) {
            if (block instanceof TableBlock && isElementHeaderTable((TableBlock) block)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isElementHeaderTable(TableBlock table) {
        if (table.getRows().isEmpty()) {
            return false;
        }
        List<TableCell> header = table.getRows().get(0);
        if (header.size() < 2) {
            return false;
        }
        return DocTexts.containsIgnoreCase(header.get(0).joinedText(), "Element")
                && DocTexts.containsIgnoreCase(header.get(1).joinedText(), "Content");
    }

    private static void appendElementRows(TableBlock table, List<ContentUnit> out) {
        for (int r = 1; r < table.getRows().size(); r++) {
            List<TableCell> row = table.getRows().get(r);
            if (row.size() < 2) {
                continue;
            }
            String element = row.get(0).joinedText().trim();
            TableCell content = row.get(1);
            ContentUnit unit = toUnit(element, content);
            if (unit != null) {
                out.add(unit);
            }
        }
    }

    private static ContentUnit toUnit(String element, TableCell content) {
        String el = element.toLowerCase(Locale.ROOT);
        Matcher heading = HEADING_ELEMENT.matcher(element.trim());
        if (heading.find()) {
            return ContentUnit.heading(Integer.parseInt(heading.group(1)), content.joinedText());
        }
        if (el.contains("unordered") || el.equals("ul") || el.contains("bullet")) {
            return ContentUnit.list(false, nonEmpty(content.getParagraphs()));
        }
        if (el.contains("ordered") || el.equals("ol") || el.contains("numbered")) {
            return ContentUnit.list(true, nonEmpty(content.getParagraphs()));
        }
        if (el.contains("p-text") || el.equals("p") || el.contains("paragraph")) {
            return ContentUnit.paragraph(content.joinedText());
        }
        return null;
    }

    private static List<String> nonEmpty(List<String> paras) {
        List<String> out = new ArrayList<>();
        for (String p : paras) {
            if (p != null && !p.isBlank()) {
                out.add(p.trim());
            }
        }
        return out;
    }

    private static TableBlock findTableSection(NormalizedDocument doc, String sectionNeedle) {
        for (DocBlock block : doc.getBlocks()) {
            if (block instanceof TableBlock) {
                TableBlock table = (TableBlock) block;
                if (DocTexts.containsIgnoreCase(table.headerText(), sectionNeedle)
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
                if (DocTexts.containsIgnoreCase(cell.joinedText(), needle)) {
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
                if (DocTexts.containsIgnoreCase(((TableBlock) block).headerText(), needle)) {
                    return i;
                }
            } else if (block instanceof ParagraphBlock) {
                if (DocTexts.containsIgnoreCase(((ParagraphBlock) block).getText(), needle)) {
                    return i;
                }
            }
        }
        return -1;
    }
}
