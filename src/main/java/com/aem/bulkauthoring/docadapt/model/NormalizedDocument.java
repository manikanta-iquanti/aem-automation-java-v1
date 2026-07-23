package com.aem.bulkauthoring.docadapt.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ordered, source-family-agnostic view of a client DOCX. */
public final class NormalizedDocument {

    private final String fileName;
    private final List<DocBlock> blocks;

    public NormalizedDocument(String fileName, List<DocBlock> blocks) {
        this.fileName = fileName;
        this.blocks = Collections.unmodifiableList(new ArrayList<>(blocks));
    }

    public String getFileName() {
        return fileName;
    }

    public List<DocBlock> getBlocks() {
        return blocks;
    }

    /** Readable plain text for side-by-side review. */
    public String toPlainText() {
        StringBuilder sb = new StringBuilder();
        for (DocBlock block : blocks) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(block.toPlainText());
        }
        return sb.toString();
    }
}
