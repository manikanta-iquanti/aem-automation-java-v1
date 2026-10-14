package com.aem.bulkauthoring.docadapt.model;

import com.aem.bulkauthoring.docadapt.content.DocTexts;

public final class ParagraphBlock implements DocBlock {

    private final FormattedText formatted;
    private final String style;
    private final int headingLevel;
    private final boolean listItem;
    private final boolean orderedList;

    public ParagraphBlock(String text, String style) {
        this(FormattedText.plain(text), style, DocTexts.headingLevelFromStyle(style), false, false);
    }

    public ParagraphBlock(String text, String style, int headingLevel,
                          boolean listItem, boolean orderedList) {
        this(FormattedText.plain(text), style, headingLevel, listItem, orderedList);
    }

    public ParagraphBlock(FormattedText formatted, String style, int headingLevel,
                          boolean listItem, boolean orderedList) {
        this.formatted = formatted == null ? FormattedText.empty() : formatted;
        this.style = style == null ? "" : style;
        this.headingLevel = headingLevel;
        this.listItem = listItem;
        this.orderedList = orderedList;
    }

    public FormattedText getFormatted() {
        return formatted;
    }

    public String getText() {
        return formatted.plainText();
    }

    public String getStyle() {
        return style;
    }

    public int getHeadingLevel() {
        if (headingLevel > 0) {
            return headingLevel;
        }
        return DocTexts.headingLevelFromStyle(style);
    }

    public boolean isListItem() {
        return listItem;
    }

    public boolean isOrderedList() {
        return orderedList;
    }

    public boolean isHeading() {
        if (getHeadingLevel() > 0) {
            return true;
        }
        String s = style.toLowerCase();
        return s.startsWith("heading") || s.startsWith("标题");
    }

    @Override
    public String toPlainText() {
        return formatted.plainText();
    }
}
