package com.aem.bulkauthoring.docadapt.catalog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Markers that belong to one AEM component (or the page node). */
public final class SlotGroup {

    public enum Role {
        PAGE_TITLE,
        HEADING,
        RICH_TEXT,
        LIST,
        CALLOUT,
        UNKNOWN
    }

    private final String parentPath;
    private final Role role;
    private final String contentPath;
    private final Map<String, String> satellitePaths;

    public SlotGroup(String parentPath,
                     Role role,
                     String contentPath,
                     Map<String, String> satellitePaths) {
        this.parentPath = parentPath;
        this.role = role;
        this.contentPath = contentPath;
        this.satellitePaths = Collections.unmodifiableMap(new LinkedHashMap<>(
                satellitePaths == null ? Map.of() : satellitePaths));
    }

    public String getParentPath() {
        return parentPath;
    }

    public Role getRole() {
        return role;
    }

    public String getContentPath() {
        return contentPath;
    }

    public Map<String, String> getSatellitePaths() {
        return satellitePaths;
    }
}
