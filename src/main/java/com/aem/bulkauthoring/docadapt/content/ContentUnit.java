package com.aem.bulkauthoring.docadapt.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One typed piece of extracted source content. Source-family agnostic. */
public final class ContentUnit {

    public enum Role {
        HEADING,
        PARAGRAPH,
        LIST
    }

    private final Role role;
    private final String text;
    private final int level;
    private final List<String> items;
    private final boolean ordered;

    private ContentUnit(Role role, String text, int level, List<String> items, boolean ordered) {
        this.role = role;
        this.text = text == null ? "" : text;
        this.level = level;
        this.items = Collections.unmodifiableList(new ArrayList<>(
                items == null ? List.of() : items));
        this.ordered = ordered;
    }

    public static ContentUnit heading(int level, String text) {
        int lvl = Math.min(6, Math.max(1, level));
        return new ContentUnit(Role.HEADING, text, lvl, List.of(), false);
    }

    public static ContentUnit paragraph(String text) {
        return new ContentUnit(Role.PARAGRAPH, text, 0, List.of(), false);
    }

    public static ContentUnit list(boolean ordered, List<String> items) {
        return new ContentUnit(Role.LIST, "", 0, items, ordered);
    }

    public Role getRole() {
        return role;
    }

    public String getText() {
        if (role == Role.LIST) {
            return String.join("\n", items);
        }
        return text;
    }

    public int getLevel() {
        return level;
    }

    public List<String> getItems() {
        return items;
    }

    public boolean isOrdered() {
        return ordered;
    }

    public String toHtml() {
        switch (role) {
            case HEADING:
                String tag = "h" + level;
                return "<" + tag + ">" + DocTexts.escapeXml(text.trim()) + "</" + tag + ">";
            case LIST:
                String listTag = ordered ? "ol" : "ul";
                StringBuilder sb = new StringBuilder();
                sb.append('<').append(listTag).append('>');
                for (String item : items) {
                    sb.append("<li>").append(DocTexts.escapeXml(item.trim())).append("</li>");
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

    private static String paragraphHtml(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] lines = text.split("\\R", -1);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("<p>").append(DocTexts.escapeXml(t)).append("</p>");
        }
        return sb.toString();
    }
}
