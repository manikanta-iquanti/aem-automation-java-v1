package com.aem.bulkauthoring.docadapt.mapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class SlotBinding {

    private String unit;
    private String path;

    public SlotBinding() {
    }

    public SlotBinding(String unit, String path) {
        this.unit = unit;
        this.path = path;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
