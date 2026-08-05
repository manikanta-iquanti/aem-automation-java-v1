package com.aem.bulkauthoring.docadapt.review;

import java.util.ArrayList;
import java.util.List;

/** Side-by-side review payload for one adapted source file. */
public final class AdaptReviewModel {

    private String sourceFile;
    private String adaptedFile;
    private String sourcePlainText;
    private List<SlotReview> slots = new ArrayList<>();

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getAdaptedFile() {
        return adaptedFile;
    }

    public void setAdaptedFile(String adaptedFile) {
        this.adaptedFile = adaptedFile;
    }

    public String getSourcePlainText() {
        return sourcePlainText;
    }

    public void setSourcePlainText(String sourcePlainText) {
        this.sourcePlainText = sourcePlainText;
    }

    public List<SlotReview> getSlots() {
        return slots;
    }

    public void setSlots(List<SlotReview> slots) {
        this.slots = slots == null ? new ArrayList<>() : slots;
    }

    public static final class SlotReview {
        private String path;
        private String value;
        private String sourceExcerpt;

        public SlotReview() {
        }

        public SlotReview(String path, String value, String sourceExcerpt) {
            this.path = path;
            this.value = value;
            this.sourceExcerpt = sourceExcerpt;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getSourceExcerpt() {
            return sourceExcerpt;
        }

        public void setSourceExcerpt(String sourceExcerpt) {
            this.sourceExcerpt = sourceExcerpt;
        }
    }
}
