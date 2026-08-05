package com.aem.bulkauthoring.docadapt.model;

public final class ParagraphBlock implements DocBlock {

    private final String text;
    private final String style;

    public ParagraphBlock(String text, String style) {
        this.text = text == null ? "" : text;
        this.style = style == null ? "" : style;
    }

    public String getText() {
        return text;
    }

    public String getStyle() {
        return style;
    }

    public boolean isHeading() {
        String s = style.toLowerCase();
        return s.startsWith("heading") || s.startsWith("标题");
    }

    @Override
    public String toPlainText() {
        return text;
    }
}
