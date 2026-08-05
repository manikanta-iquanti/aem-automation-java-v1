package com.aem.bulkauthoring.docadapt.strategy;

/** One mapped slot value plus the source excerpt used for review. */
public final class ExtractedSlot {

    private final String path;
    private final String value;
    private final String sourceExcerpt;

    public ExtractedSlot(String path, String value, String sourceExcerpt) {
        this.path = path;
        this.value = value == null ? "" : value;
        this.sourceExcerpt = sourceExcerpt == null ? "" : sourceExcerpt;
    }

    public String getPath() {
        return path;
    }

    public String getValue() {
        return value;
    }

    public String getSourceExcerpt() {
        return sourceExcerpt;
    }
}
