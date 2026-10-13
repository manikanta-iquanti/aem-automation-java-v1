package com.aem.bulkauthoring.docadapt.model;

import com.aem.bulkauthoring.docadapt.content.DocTexts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Ordered plain and linked text segments from a Word paragraph. */
public final class FormattedText {

    private final List<Segment> segments;

    private FormattedText(List<Segment> segments) {
        this.segments = Collections.unmodifiableList(new ArrayList<>(
                segments == null ? List.of() : segments));
    }

    public static FormattedText empty() {
        return new FormattedText(List.of());
    }

    public static FormattedText plain(String text) {
        if (text == null || text.isEmpty()) {
            return empty();
        }
        return new FormattedText(List.of(Segment.plain(text)));
    }

    public static FormattedText of(List<Segment> segments) {
        if (segments == null || segments.isEmpty()) {
            return empty();
        }
        List<Segment> merged = new ArrayList<>();
        for (Segment segment : segments) {
            if (segment == null || segment.getText().isEmpty()) {
                continue;
            }
            if (merged.isEmpty()) {
                merged.add(segment);
                continue;
            }
            Segment last = merged.get(merged.size() - 1);
            if (Objects.equals(last.getHref(), segment.getHref())) {
                merged.set(merged.size() - 1,
                        new Segment(last.getText() + segment.getText(), last.getHref()));
            } else {
                merged.add(segment);
            }
        }
        return new FormattedText(merged);
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public boolean isBlank() {
        return plainText().isBlank();
    }

    public String plainText() {
        StringBuilder sb = new StringBuilder();
        for (Segment segment : segments) {
            sb.append(segment.getText());
        }
        return sb.toString();
    }

    /**
     * Inner HTML for the paragraph content (no wrapper tag). Text and hrefs are escaped.
     * Unsafe or blank hrefs render as plain escaped text.
     */
    public String toHtml() {
        StringBuilder sb = new StringBuilder();
        for (Segment segment : segments) {
            String text = DocTexts.escapeXml(segment.getText());
            String href = segment.getHref();
            if (href != null && isSafeHref(href)) {
                sb.append("<a href=\"").append(DocTexts.escapeXml(href.trim())).append("\">")
                        .append(text)
                        .append("</a>");
            } else {
                sb.append(text);
            }
        }
        return sb.toString();
    }

    public FormattedText trim() {
        String plain = plainText();
        int start = 0;
        int end = plain.length();
        while (start < end && Character.isWhitespace(plain.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(plain.charAt(end - 1))) {
            end--;
        }
        if (start == 0 && end == plain.length()) {
            return this;
        }
        if (start >= end) {
            return empty();
        }
        return slice(start, end);
    }

    /** Inclusive-exclusive slice over the concatenated plain text. */
    public FormattedText slice(int start, int end) {
        if (start >= end) {
            return empty();
        }
        List<Segment> out = new ArrayList<>();
        int cursor = 0;
        for (Segment segment : segments) {
            int segStart = cursor;
            int segEnd = cursor + segment.getText().length();
            cursor = segEnd;
            int from = Math.max(start, segStart);
            int to = Math.min(end, segEnd);
            if (from >= to) {
                continue;
            }
            String piece = segment.getText().substring(from - segStart, to - segStart);
            out.add(new Segment(piece, segment.getHref()));
        }
        return of(out);
    }

    public static boolean isSafeHref(String href) {
        if (href == null || href.isBlank()) {
            return false;
        }
        String lower = href.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("mailto:");
    }

    public static final class Segment {
        private final String text;
        private final String href;

        private Segment(String text, String href) {
            this.text = text == null ? "" : text;
            this.href = href;
        }

        public static Segment plain(String text) {
            return new Segment(text, null);
        }

        public static Segment link(String text, String href) {
            return new Segment(text, href);
        }

        public String getText() {
            return text;
        }

        public String getHref() {
            return href;
        }
    }
}
