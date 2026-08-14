package com.aem.bulkauthoring.docadapt.recipe;

import com.aem.bulkauthoring.docadapt.content.ContentDocument;
import com.aem.bulkauthoring.docadapt.content.ContentUnit;
import com.aem.bulkauthoring.docadapt.content.DocTexts;
import com.aem.bulkauthoring.docadapt.model.DocBlock;
import com.aem.bulkauthoring.docadapt.model.NormalizedDocument;
import com.aem.bulkauthoring.docadapt.model.ParagraphBlock;
import com.aem.bulkauthoring.docadapt.model.TableBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Authored Word articles: Heading 1/2 styles, paragraphs, and bullet/number lists.
 */
public final class WordOutlineRecipe implements SourceRecipe {

    static final String ID = "word-outline";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public int score(NormalizedDocument doc) {
        int headings = 0;
        int paras = 0;
        int tables = 0;
        for (DocBlock block : doc.getBlocks()) {
            if (block instanceof ParagraphBlock) {
                ParagraphBlock p = (ParagraphBlock) block;
                if (p.isHeading() || p.getHeadingLevel() > 0) {
                    headings++;
                } else if (!p.getText().isBlank()) {
                    paras++;
                }
            } else if (block instanceof TableBlock) {
                tables++;
            }
        }
        if (headings == 0 && paras == 0) {
            return 0;
        }
        return Math.max(0, headings * 15 + Math.min(paras, 8) * 3 - tables * 10);
    }

    @Override
    public ContentDocument extract(NormalizedDocument doc) {
        String title = null;
        List<ContentUnit> units = new ArrayList<>();
        List<String> listBuf = new ArrayList<>();
        boolean listOrdered = false;

        for (DocBlock block : doc.getBlocks()) {
            if (block instanceof ParagraphBlock) {
                ParagraphBlock p = (ParagraphBlock) block;
                String text = p.getText().trim();
                if (text.isEmpty()) {
                    continue;
                }
                int headingLevel = headingLevel(p);
                if (headingLevel > 0) {
                    flushList(units, listBuf, listOrdered);
                    if (title == null && headingLevel == 1) {
                        title = text;
                    } else {
                        units.add(ContentUnit.heading(headingLevel, text));
                    }
                    continue;
                }
                BulletPrefix bullet = bulletPrefix(text);
                if (p.isListItem() || bullet != null) {
                    String item = bullet == null ? text : bullet.text;
                    boolean ordered = p.isListItem() ? p.isOrderedList() : bullet.ordered;
                    if (!listBuf.isEmpty() && ordered != listOrdered) {
                        flushList(units, listBuf, listOrdered);
                    }
                    listOrdered = ordered;
                    listBuf.add(item);
                    continue;
                }
                flushList(units, listBuf, listOrdered);
                if (title == null && text.length() >= 3) {
                    title = text;
                } else {
                    units.add(ContentUnit.paragraph(text));
                }
            } else if (block instanceof TableBlock) {
                flushList(units, listBuf, listOrdered);
                String plain = block.toPlainText().trim();
                if (!plain.isEmpty()) {
                    if (title == null && plain.length() >= 3) {
                        title = firstLine(plain);
                    } else {
                        units.add(ContentUnit.paragraph(plain));
                    }
                }
            }
        }
        flushList(units, listBuf, listOrdered);
        if (title == null) {
            title = stem(doc.getFileName());
        }
        return new ContentDocument(ID, title, units);
    }

    private static int headingLevel(ParagraphBlock p) {
        int level = p.getHeadingLevel();
        if (level > 0) {
            return level;
        }
        if (p.isHeading()) {
            return 2;
        }
        if (DocTexts.looksLikeHeadingLine(p.getText())) {
            return 2;
        }
        return 0;
    }

    private static void flushList(List<ContentUnit> units, List<String> buf, boolean ordered) {
        if (buf.isEmpty()) {
            return;
        }
        units.add(ContentUnit.list(ordered, new ArrayList<>(buf)));
        buf.clear();
    }

    private static BulletPrefix bulletPrefix(String text) {
        if (text.startsWith("- ") || text.startsWith("* ") || text.startsWith("• ")) {
            return new BulletPrefix(false, text.substring(2).trim());
        }
        if (text.matches("^\\d+[.)]\\s+.+")) {
            return new BulletPrefix(true, text.replaceFirst("^\\d+[.)]\\s+", "").trim());
        }
        return null;
    }

    private static String firstLine(String text) {
        int nl = text.indexOf('\n');
        return nl < 0 ? text : text.substring(0, nl).trim();
    }

    private static String stem(String name) {
        if (name == null) {
            return "";
        }
        if (name.toLowerCase().endsWith(".docx")) {
            return name.substring(0, name.length() - 5);
        }
        return name;
    }

    private static final class BulletPrefix {
        final boolean ordered;
        final String text;

        BulletPrefix(boolean ordered, String text) {
            this.ordered = ordered;
            this.text = text;
        }
    }
}
