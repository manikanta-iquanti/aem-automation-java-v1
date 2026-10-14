package com.aem.bulkauthoring.docadapt.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** One table cell: paragraphs preserved (needed for list items). */
public final class TableCell {

    private final List<FormattedText> paragraphs;

    public TableCell(List<String> paragraphs) {
        List<FormattedText> formatted = new ArrayList<>();
        if (paragraphs != null) {
            for (String paragraph : paragraphs) {
                formatted.add(FormattedText.plain(paragraph));
            }
        }
        this.paragraphs = Collections.unmodifiableList(formatted);
    }

    public TableCell(List<FormattedText> paragraphs, boolean formatted) {
        this.paragraphs = Collections.unmodifiableList(new ArrayList<>(
                paragraphs == null ? List.of() : paragraphs));
    }

    public static TableCell ofFormatted(List<FormattedText> paragraphs) {
        return new TableCell(paragraphs, true);
    }

    public List<String> getParagraphs() {
        return paragraphs.stream()
                .map(FormattedText::plainText)
                .collect(Collectors.toList());
    }

    public List<FormattedText> getFormattedParagraphs() {
        return paragraphs;
    }

    public String joinedText() {
        return paragraphs.stream()
                .map(FormattedText::plainText)
                .collect(Collectors.joining("\n"))
                .trim();
    }

    public FormattedText joinedFormatted() {
        List<FormattedText.Segment> segments = new ArrayList<>();
        boolean first = true;
        for (FormattedText paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                continue;
            }
            if (!first) {
                segments.add(FormattedText.Segment.plain("\n"));
            }
            first = false;
            segments.addAll(paragraph.getSegments());
        }
        return FormattedText.of(segments).trim();
    }
}
