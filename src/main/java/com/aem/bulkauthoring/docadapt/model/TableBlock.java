package com.aem.bulkauthoring.docadapt.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class TableBlock implements DocBlock {

    private final List<List<TableCell>> rows;

    public TableBlock(List<List<TableCell>> rows) {
        List<List<TableCell>> copy = new ArrayList<>();
        for (List<TableCell> row : rows) {
            copy.add(Collections.unmodifiableList(new ArrayList<>(row)));
        }
        this.rows = Collections.unmodifiableList(copy);
    }

    public List<List<TableCell>> getRows() {
        return rows;
    }

    /** First-row joined text — often a section title in publishing forms. */
    public String headerText() {
        if (rows.isEmpty()) {
            return "";
        }
        return rows.get(0).stream()
                .map(TableCell::joinedText)
                .collect(Collectors.joining(" "))
                .trim();
    }

    @Override
    public String toPlainText() {
        StringBuilder sb = new StringBuilder();
        for (List<TableCell> row : rows) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(row.stream()
                    .map(TableCell::joinedText)
                    .collect(Collectors.joining(" | ")));
        }
        return sb.toString();
    }
}
