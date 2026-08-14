package com.aem.bulkauthoring.docadapt.bind;

import com.aem.bulkauthoring.docadapt.catalog.SlotCatalog;
import com.aem.bulkauthoring.docadapt.catalog.SlotGroup;
import com.aem.bulkauthoring.docadapt.content.ContentDocument;
import com.aem.bulkauthoring.docadapt.content.ContentUnit;
import com.aem.bulkauthoring.docadapt.content.DocTexts;
import com.aem.bulkauthoring.docadapt.mapping.SlotBinding;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Binds a canonical ContentDocument onto discovered template slots. */
public final class BindingEngine {

    public BoundSlots bind(ContentDocument content,
                           SlotCatalog catalog,
                           List<SlotBinding> overrides) {
        if (overrides != null && !overrides.isEmpty()) {
            return applyOverrides(content, catalog, overrides);
        }
        if (catalog.isDumpShape()) {
            return dump(content, catalog);
        }
        return sequential(content, catalog);
    }

    private BoundSlots dump(ContentDocument content, SlotCatalog catalog) {
        Map<String, String> values = emptyValues(catalog);
        Map<String, String> units = new LinkedHashMap<>();
        List<SlotBinding> bindings = new ArrayList<>();

        for (SlotGroup g : catalog.getGroups()) {
            if (g.getRole() == SlotGroup.Role.PAGE_TITLE && g.getContentPath() != null) {
                put(values, units, bindings, g.getContentPath(), "title", content.getTitle());
                fillSatellites(g, null, values);
            } else if ((g.getRole() == SlotGroup.Role.RICH_TEXT
                    || g.getRole() == SlotGroup.Role.CALLOUT)
                    && g.getContentPath() != null) {
                put(values, units, bindings, g.getContentPath(), "blocksHtml", content.blocksHtml());
                fillSatellites(g, null, values);
            }
        }
        return new BoundSlots(values, units, bindings, "dump");
    }

    private BoundSlots sequential(ContentDocument content, SlotCatalog catalog) {
        Map<String, String> values = emptyValues(catalog);
        Map<String, String> units = new LinkedHashMap<>();
        List<SlotBinding> bindings = new ArrayList<>();

        Deque<SlotGroup> headings = new ArrayDeque<>();
        Deque<SlotGroup> rich = new ArrayDeque<>();
        Deque<SlotGroup> lists = new ArrayDeque<>();
        SlotGroup lastRich = null;

        for (SlotGroup g : catalog.getGroups()) {
            if (g.getRole() == SlotGroup.Role.PAGE_TITLE && g.getContentPath() != null) {
                put(values, units, bindings, g.getContentPath(), "title", content.getTitle());
            } else if (g.getRole() == SlotGroup.Role.HEADING) {
                headings.add(g);
            } else if (g.getRole() == SlotGroup.Role.RICH_TEXT
                    || g.getRole() == SlotGroup.Role.CALLOUT) {
                rich.add(g);
            } else if (g.getRole() == SlotGroup.Role.LIST) {
                lists.add(g);
            }
        }

        List<ContentUnit> blocks = content.getBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            ContentUnit block = blocks.get(i);
            String unitId = "block:" + i;
            switch (block.getRole()) {
                case HEADING:
                    SlotGroup head = headings.pollFirst();
                    if (head != null) {
                        fillHeading(head, block, unitId, values, units, bindings);
                    } else {
                        lastRich = appendToRich(lastRich, rich, block.toHtml(), unitId,
                                values, units, bindings);
                    }
                    break;
                case LIST:
                    SlotGroup listGroup = lists.pollFirst();
                    if (listGroup != null) {
                        String listTypePath = listGroup.getSatellitePaths().get("listType");
                        if (listTypePath != null) {
                            put(values, units, bindings, listTypePath, unitId,
                                    block.isOrdered() ? "ol" : "ul");
                        }
                        if (listGroup.getContentPath() != null) {
                            put(values, units, bindings, listGroup.getContentPath(), unitId,
                                    block.toHtml());
                            lastRich = listGroup;
                        } else {
                            lastRich = takeRich(rich, lastRich, block.toHtml(), unitId,
                                    values, units, bindings);
                        }
                    } else {
                        lastRich = takeRich(rich, lastRich, block.toHtml(), unitId,
                                values, units, bindings);
                    }
                    break;
                case PARAGRAPH:
                default:
                    lastRich = takeRich(rich, lastRich, block.toHtml(), unitId,
                            values, units, bindings);
                    break;
            }
        }
        return new BoundSlots(values, units, bindings, "sequential");
    }

    private BoundSlots applyOverrides(ContentDocument content,
                                      SlotCatalog catalog,
                                      List<SlotBinding> overrides) {
        Map<String, String> values = emptyValues(catalog);
        Map<String, String> units = new LinkedHashMap<>();
        List<SlotBinding> bindings = new ArrayList<>();
        for (SlotBinding b : overrides) {
            if (b == null || b.getPath() == null || b.getPath().isBlank()) {
                continue;
            }
            if (b.getUnit() == null || b.getUnit().isBlank() || "__empty__".equals(b.getUnit())) {
                bindings.add(new SlotBinding(b.getUnit(), b.getPath()));
                continue;
            }
            String value = valueFor(content, b.getUnit(), b.getPath());
            put(values, units, bindings, b.getPath(), b.getUnit(), value);
            SlotGroup g = catalog.groupForPath(b.getPath());
            if (g != null) {
                fillSatellites(g, content.blockAt(blockIndex(b.getUnit())), values);
            }
        }
        return new BoundSlots(values, units, bindings, "manual");
    }

    private static Map<String, String> emptyValues(SlotCatalog catalog) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String path : catalog.getPaths()) {
            values.put(path, "");
        }
        return values;
    }

    private static void fillHeading(SlotGroup g,
                                    ContentUnit block,
                                    String unitId,
                                    Map<String, String> values,
                                    Map<String, String> units,
                                    List<SlotBinding> bindings) {
        if (g.getContentPath() != null) {
            put(values, units, bindings, g.getContentPath(), unitId, block.getText());
        }
        fillSatellites(g, block, values);
    }

    private static void fillSatellites(SlotGroup g, ContentUnit block, Map<String, String> values) {
        for (Map.Entry<String, String> e : g.getSatellitePaths().entrySet()) {
            String prop = e.getKey().toLowerCase(Locale.ROOT);
            String path = e.getValue();
            if ("textisrich".equals(prop)) {
                values.put(path, "true");
            } else if ("level".equals(prop) && block != null && block.getRole() == ContentUnit.Role.HEADING) {
                values.put(path, String.valueOf(block.getLevel()));
            } else if ("anchorid".equals(prop) && block != null) {
                values.put(path, DocTexts.slug(block.getText()));
            } else if ("listtype".equals(prop) && block != null && block.getRole() == ContentUnit.Role.LIST) {
                values.put(path, block.isOrdered() ? "ol" : "ul");
            }
        }
    }

    private static SlotGroup takeRich(Deque<SlotGroup> rich,
                                      SlotGroup lastRich,
                                      String html,
                                      String unitId,
                                      Map<String, String> values,
                                      Map<String, String> units,
                                      List<SlotBinding> bindings) {
        SlotGroup g = rich.pollFirst();
        if (g != null && g.getContentPath() != null) {
            put(values, units, bindings, g.getContentPath(), unitId, html);
            fillSatellites(g, null, values);
            return g;
        }
        return appendToRich(lastRich, rich, html, unitId, values, units, bindings);
    }

    private static SlotGroup appendToRich(SlotGroup lastRich,
                                          Deque<SlotGroup> rich,
                                          String html,
                                          String unitId,
                                          Map<String, String> values,
                                          Map<String, String> units,
                                          List<SlotBinding> bindings) {
        SlotGroup target = lastRich;
        if (target == null || target.getContentPath() == null) {
            target = rich.pollFirst();
        }
        if (target == null || target.getContentPath() == null) {
            return lastRich;
        }
        put(values, units, bindings, target.getContentPath(), unitId, html);
        fillSatellites(target, null, values);
        return target;
    }

    private static void put(Map<String, String> values,
                            Map<String, String> units,
                            List<SlotBinding> bindings,
                            String path,
                            String unitId,
                            String value) {
        if (path == null) {
            return;
        }
        String existing = values.get(path);
        if (existing != null && !existing.isBlank() && value != null && !value.isBlank()) {
            values.put(path, existing + "\n" + value);
        } else {
            values.put(path, value == null ? "" : value);
        }
        units.put(path, unitId);
        bindings.add(new SlotBinding(unitId, path));
    }

    static String valueFor(ContentDocument content, String unitId, String path) {
        if (unitId == null) {
            return "";
        }
        if ("title".equals(unitId)) {
            return content.getTitle();
        }
        if ("blocksHtml".equals(unitId)) {
            return content.blocksHtml();
        }
        ContentUnit block = content.blockAt(blockIndex(unitId));
        if (block == null) {
            return "";
        }
        String prop = DocTexts.lastSegment(path).toLowerCase(Locale.ROOT);
        if ("listtype".equals(prop)) {
            return block.isOrdered() ? "ol" : "ul";
        }
        if ("level".equals(prop)) {
            return block.getRole() == ContentUnit.Role.HEADING
                    ? String.valueOf(block.getLevel()) : "";
        }
        if ("anchorid".equals(prop)) {
            return DocTexts.slug(block.getText());
        }
        if ("jcr:title".equals(prop) || (block.getRole() == ContentUnit.Role.HEADING
                && ("text".equals(prop)))) {
            return block.getText();
        }
        return block.toHtml();
    }

    private static int blockIndex(String unitId) {
        if (unitId == null || !unitId.startsWith("block:")) {
            return -1;
        }
        try {
            return Integer.parseInt(unitId.substring("block:".length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
