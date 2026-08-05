package com.aem.bulkauthoring.docadapt.model;

/** One ordered block from a source DOCX (paragraph or table). */
public interface DocBlock {
    String toPlainText();
}
