package com.aem.bulkauthoring.docadapt.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One table cell: paragraphs preserved (needed for list items). */
public final class TableCell {

    private final List<String> paragraphs;

    public TableCell(List<String> paragraphs) {
        this.paragraphs = Collections.unmodifiableList(new ArrayList<>(
                paragraphs == null ? List.of() : paragraphs));
    }

    public List<String> getParagraphs() {
        return paragraphs;
    }

    public String joinedText() {
        return String.join("\n", paragraphs).trim();
    }
}
