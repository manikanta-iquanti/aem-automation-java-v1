package com.aem.bulkauthoring.docadapt.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Canonical extracted article: title + typed blocks. */
public final class ContentDocument {

    private final String recipeId;
    private final String title;
    private final List<ContentUnit> blocks;

    public ContentDocument(String recipeId, String title, List<ContentUnit> blocks) {
        this.recipeId = recipeId == null ? "" : recipeId;
        this.title = title == null ? "" : title;
        this.blocks = Collections.unmodifiableList(new ArrayList<>(
                blocks == null ? List.of() : blocks));
    }

    public String getRecipeId() {
        return recipeId;
    }

    public String getTitle() {
        return title;
    }

    public List<ContentUnit> getBlocks() {
        return blocks;
    }

    public String blocksHtml() {
        StringBuilder sb = new StringBuilder();
        for (ContentUnit block : blocks) {
            String html = block.toHtml();
            if (html.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(html);
        }
        return sb.toString();
    }

    /** HTML per source section in document order; empty when the source has no sections. */
    public List<String> sections() {
        Map<Integer, StringBuilder> bySection = new TreeMap<>();
        for (ContentUnit block : blocks) {
            if (block.getSection() <= 0) {
                continue;
            }
            String html = block.toHtml();
            if (html.isBlank()) {
                continue;
            }
            StringBuilder sb = bySection.computeIfAbsent(block.getSection(), k -> new StringBuilder());
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(html);
        }
        List<String> out = new ArrayList<>();
        for (StringBuilder sb : bySection.values()) {
            out.add(sb.toString());
        }
        return out;
    }

    public String sectionHtml(int oneBased) {
        List<String> all = sections();
        if (oneBased < 1 || oneBased > all.size()) {
            return "";
        }
        return all.get(oneBased - 1);
    }

    public List<SelectableUnit> selectableUnits() {
        List<SelectableUnit> out = new ArrayList<>();
        out.add(new SelectableUnit("title", "Title: " + DocTexts.truncate(title, 60), title));
        out.add(new SelectableUnit(
                "blocksHtml",
                "All body (HTML)",
                blocksHtml()));
        List<String> sections = sections();
        for (int i = 0; i < sections.size(); i++) {
            out.add(new SelectableUnit(
                    "section:" + (i + 1),
                    "Section " + (i + 1) + " (HTML)",
                    sections.get(i)));
        }
        for (int i = 0; i < blocks.size(); i++) {
            ContentUnit block = blocks.get(i);
            out.add(new SelectableUnit("block:" + i, block.label(), block.toHtml()));
        }
        return out;
    }

    public ContentUnit blockAt(int index) {
        if (index < 0 || index >= blocks.size()) {
            return null;
        }
        return blocks.get(index);
    }
}
