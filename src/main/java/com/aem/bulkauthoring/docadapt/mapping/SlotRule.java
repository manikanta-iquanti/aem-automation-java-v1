package com.aem.bulkauthoring.docadapt.mapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class SlotRule {

    private String path;
    private ExtractRule extract;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ExtractRule getExtract() {
        return extract;
    }

    public void setExtract(ExtractRule extract) {
        this.extract = extract;
    }
}
