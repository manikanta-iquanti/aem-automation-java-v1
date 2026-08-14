package com.aem.bulkauthoring.docadapt.review;

import com.aem.bulkauthoring.docadapt.content.SelectableUnit;
import com.aem.bulkauthoring.docadapt.mapping.SlotBinding;

import java.util.ArrayList;
import java.util.List;

/** Side-by-side review payload for one adapted source file. */
public final class AdaptReviewModel {

    private String sourceFile;
    private String adaptedFile;
    private String sourcePlainText;
    private String recipeId;
    private String bindStrategy;
    private List<SlotReview> slots = new ArrayList<>();
    private List<SelectableUnit> units = new ArrayList<>();
    private List<SlotBinding> bindings = new ArrayList<>();

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

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getBindStrategy() {
        return bindStrategy;
    }

    public void setBindStrategy(String bindStrategy) {
        this.bindStrategy = bindStrategy;
    }

    public List<SlotReview> getSlots() {
        return slots;
    }

    public void setSlots(List<SlotReview> slots) {
        this.slots = slots == null ? new ArrayList<>() : slots;
    }

    public List<SelectableUnit> getUnits() {
        return units;
    }

    public void setUnits(List<SelectableUnit> units) {
        this.units = units == null ? new ArrayList<>() : units;
    }

    public List<SlotBinding> getBindings() {
        return bindings;
    }

    public void setBindings(List<SlotBinding> bindings) {
        this.bindings = bindings == null ? new ArrayList<>() : bindings;
    }

    public static final class SlotReview {
        private String path;
        private String value;
        private String sourceExcerpt;
        private String unitId;

        public SlotReview() {
        }

        public SlotReview(String path, String value, String sourceExcerpt) {
            this(path, value, sourceExcerpt, null);
        }

        public SlotReview(String path, String value, String sourceExcerpt, String unitId) {
            this.path = path;
            this.value = value;
            this.sourceExcerpt = sourceExcerpt;
            this.unitId = unitId;
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

        public String getUnitId() {
            return unitId;
        }

        public void setUnitId(String unitId) {
            this.unitId = unitId;
        }
    }
}
