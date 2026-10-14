package com.aem.bulkauthoring.docadapt.content;

/** One assignable extracted unit for teach-once UI. */
public final class SelectableUnit {

    private final String id;
    private final String label;
    private final String value;

    public SelectableUnit(String id, String label, String value) {
        this.id = id;
        this.label = label;
        this.value = value == null ? "" : value;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }
}
