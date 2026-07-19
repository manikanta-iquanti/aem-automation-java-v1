package com.aem.bulkauthoring.packagebuilder;

import com.fasterxml.jackson.databind.JsonNode;

public class PageArtifact {

    private final String pageName;
    private final JsonNode pageRoot;

    public PageArtifact(String pageName, JsonNode pageRoot) {
        this.pageName = pageName;
        this.pageRoot = pageRoot;
    }

    public String getPageName() {
        return pageName;
    }

    public JsonNode getPageRoot() {
        return pageRoot;
    }
}
