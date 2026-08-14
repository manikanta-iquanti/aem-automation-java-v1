package com.aem.bulkauthoring.docadapt.catalog;

import com.aem.bulkauthoring.docadapt.content.DocTexts;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Groups discovered [[path]] markers and infers roles from AEM property names. */
public final class SlotCatalog {

    private final List<String> paths;
    private final List<SlotGroup> groups;

    private SlotCatalog(List<String> paths, List<SlotGroup> groups) {
        this.paths = List.copyOf(paths);
        this.groups = List.copyOf(groups);
    }

    public static SlotCatalog fromPaths(List<String> markerPaths) {
        List<String> paths = markerPaths == null ? List.of() : new ArrayList<>(markerPaths);
        Map<String, List<String>> byParent = new LinkedHashMap<>();
        for (String path : paths) {
            if (path == null || path.isBlank()) {
                continue;
            }
            byParent.computeIfAbsent(DocTexts.parentPath(path), k -> new ArrayList<>()).add(path);
        }
        List<SlotGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : byParent.entrySet()) {
            groups.add(buildGroup(e.getKey(), e.getValue()));
        }
        return new SlotCatalog(paths, groups);
    }

    public List<String> getPaths() {
        return paths;
    }

    public List<SlotGroup> getGroups() {
        return groups;
    }

    public boolean isDumpShape() {
        int rich = 0;
        int typed = 0;
        for (SlotGroup g : groups) {
            if (g.getRole() == SlotGroup.Role.RICH_TEXT || g.getRole() == SlotGroup.Role.CALLOUT) {
                rich++;
            } else if (g.getRole() == SlotGroup.Role.HEADING || g.getRole() == SlotGroup.Role.LIST) {
                typed++;
            }
        }
        return typed == 0 && rich == 1;
    }

    public SlotGroup groupForPath(String path) {
        for (SlotGroup g : groups) {
            if (path != null && path.equals(g.getContentPath())) {
                return g;
            }
            if (g.getSatellitePaths().containsValue(path)) {
                return g;
            }
        }
        return null;
    }

    private static SlotGroup buildGroup(String parent, List<String> groupPaths) {
        Map<String, String> byProp = new LinkedHashMap<>();
        for (String path : groupPaths) {
            byProp.put(DocTexts.lastSegment(path).toLowerCase(Locale.ROOT), path);
        }
        String contentPath = pickContent(byProp);
        Map<String, String> satellites = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : byProp.entrySet()) {
            if (contentPath != null && contentPath.equals(e.getValue())) {
                continue;
            }
            satellites.put(originalProp(e.getValue()), e.getValue());
        }
        return new SlotGroup(parent, inferRole(byProp), contentPath, satellites);
    }

    private static String pickContent(Map<String, String> byProp) {
        if (byProp.containsKey("jcr:title")) {
            return byProp.get("jcr:title");
        }
        if (byProp.containsKey("body")) {
            return byProp.get("body");
        }
        if (byProp.containsKey("text")) {
            return byProp.get("text");
        }
        return null;
    }

    private static SlotGroup.Role inferRole(Map<String, String> byProp) {
        if (byProp.containsKey("jcr:title")) {
            return SlotGroup.Role.PAGE_TITLE;
        }
        if (byProp.containsKey("level") || byProp.containsKey("anchorid")) {
            return SlotGroup.Role.HEADING;
        }
        if (byProp.containsKey("listtype")
                && !byProp.containsKey("text")
                && !byProp.containsKey("body")) {
            return SlotGroup.Role.LIST;
        }
        if (byProp.containsKey("eyebrow")) {
            return SlotGroup.Role.CALLOUT;
        }
        if (byProp.containsKey("text") || byProp.containsKey("body")) {
            return SlotGroup.Role.RICH_TEXT;
        }
        return SlotGroup.Role.UNKNOWN;
    }

    private static String originalProp(String path) {
        return DocTexts.lastSegment(path);
    }
}
