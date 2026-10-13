package com.aem.bulkauthoring.docadapt.content;

import com.aem.bulkauthoring.docadapt.model.FormattedText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** One typed piece of extracted source content. Source-family agnostic. */
public final class ContentUnit {

    public enum Role {
        HEADING,
        PARAGRAPH,
        LIST
    }

    private final Role role;
    private final FormattedText text;
    private final int level;
    private final List<FormattedText> items;
    private final boolean ordered;

    private ContentUnit(Role role,
                        FormattedText text,
                        int level,
                        List<FormattedText> items,
                        boolean ordered) {
        this.role = role;
        this.text = text == null ? FormattedText.empty() : text;
        this.level = level;
        this.items = Collections.unmodifiableList(new ArrayList<>(
                items == null ? List.of() : items));
        this.ordered = ordered;
    }

    public static ContentUnit heading(int level, String text) {
        return heading(level, FormattedText.plain(text));
    }

    public static ContentUnit heading(int level, FormattedText text) {
        int lvl = Math.min(6, Math.max(1, level));
        return new ContentUnit(Role.HEADING, text, lvl, List.of(), false);
    }

    public static ContentUnit paragraph(String text) {
        return paragraph(FormattedText.plain(text));
    }

    public static ContentUnit paragraph(FormattedText text) {
        return new ContentUnit(Role.PARAGRAPH, text, 0, List.of(), false);
    }

    public static ContentUnit list(boolean ordered, List<String> items) {
        List<FormattedText> formatted = new ArrayList<>();
        if (items != null) {
            for (String item : items) {
                formatted.add(FormattedText.plain(item));
            }
        }
        return listFormatted(ordered, formatted);
    }

    public static ContentUnit listFormatted(boolean ordered, List<FormattedText> items) {
        return new ContentUnit(Role.LIST, FormattedText.empty(), 0, items, ordered);
    }

    public Role getRole() {
        return role;
    }

    public String getText() {
        if (role == Role.LIST) {
            return items.stream()
                    .map(FormattedText::plainText)
                    .collect(Collectors.joining("\n"));
        }
        return text.plainText();
    }

    public FormattedText getFormatted() {
        return text;
    }

    public int getLevel() {
        return level;
    }

    public List<String> getItems() {
        return items.stream()
                .map(FormattedText::plainText)
                .collect(Collectors.toList());
    }

    public List<FormattedText> getFormattedItems() {
        return items;
    }

    public boolean isOrdered() {
        return ordered;
    }

    public String toHtml() {
        switch (role) {
            case HEADING:
                String tag = "h" + level;
                return "<" + tag + ">" + text.toHtml() + "</" + tag + ">";
            case LIST:
                String listTag = ordered ? "ol" : "ul";
                StringBuilder sb = new StringBuilder();
                sb.append('<').append(listTag).append('>');
                for (FormattedText item : items) {
                    sb.append("<li>").append(item.trim().toHtml()).append("</li>");
                }
                sb.append("</").append(listTag).append('>');
                return sb.toString();
            case PARAGRAPH:
            default:
                return paragraphHtml(text);
        }
    }

    public String label() {
        String excerpt = DocTexts.truncate(getText(), 60);
        switch (role) {
            case HEADING:
                return "H" + level + ": " + excerpt;
            case LIST:
                return (ordered ? "List (ol): " : "List (ul): ") + excerpt;
            case PARAGRAPH:
            default:
                return "Paragraph: " + excerpt;
        }
    }

    private static String paragraphHtml(FormattedText text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        // Preserve links while still splitting multi-line paragraphs into <p> tags.
        String plain = text.plainText();
        String[] lines = plain.split("\\R", -1);
        if (lines.length <= 1) {
            FormattedText trimmed = text.trim();
            if (trimmed.isBlank()) {
                return "";
            }
            return "<p>" + trimmed.toHtml() + "</p>";
        }
        StringBuilder sb = new StringBuilder();
        int cursor = 0;
        for (String line : lines) {
            int lineStart = cursor;
            int lineEnd = cursor + line.length();
            cursor = lineEnd + 1; // skip the newline that split consumed
            if (line.trim().isEmpty()) {
                continue;
            }
            int leading = 0;
            while (leading < line.length() && Character.isWhitespace(line.charAt(leading))) {
                leading++;
            }
            int trailing = 0;
            while (trailing < line.length() - leading
                    && Character.isWhitespace(line.charAt(line.length() - 1 - trailing))) {
                trailing++;
            }
            FormattedText slice = text.slice(lineStart + leading, lineEnd - trailing);
            if (slice.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("<p>").append(slice.toHtml()).append("</p>");
        }
        return sb.toString();
    }
}
