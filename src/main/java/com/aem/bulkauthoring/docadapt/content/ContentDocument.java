package com.aem.bulkauthoring.docadapt.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public List<SelectableUnit> selectableUnits() {
        List<SelectableUnit> out = new ArrayList<>();
        out.add(new SelectableUnit("title", "Title: " + DocTexts.truncate(title, 60), title));
        out.add(new SelectableUnit(
                "blocksHtml",
                "All body (HTML)",
                blocksHtml()));
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
