package com.aem.bulkauthoring.updater;

import com.aem.bulkauthoring.blueprint.FieldFormat;
import com.aem.bulkauthoring.model.document.DocumentBlock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies document blocks onto a JSON blueprint using path markers and
 * {@link FieldFormat} from the blueprint profile.
 */
public class BlueprintUpdater {

    private static final String SECTION_SEPARATOR = "#";

    private static final List<String> INSTANCE_PROPERTIES = List.of(
            "jcr:uuid",
            "jcr:created",
            "jcr:createdBy",
            "jcr:lastModified",
            "jcr:lastModifiedBy",
            "cq:lastModified",
            "cq:lastModifiedBy");

    private final ObjectMapper mapper = new ObjectMapper();

    public void update(File originalBlueprint,
                       List<DocumentBlock> blocks,
                       File outputFile,
                       Map<String, FieldFormat> formats) {
        update(originalBlueprint, blocks, outputFile, formats, List.of());
    }

    public void update(File originalBlueprint,
                       List<DocumentBlock> blocks,
                       File outputFile,
                       Map<String, FieldFormat> formats,
                       List<String> excludedPaths) {

        try {

            JsonNode root = mapper.readTree(originalBlueprint);
            ObjectNode copy = (ObjectNode) root.deepCopy();

            Map<String, String> valuesByPath = indexBlocks(blocks);
            Map<String, FieldFormat> formatMap =
                    formats == null ? Collections.emptyMap() : formats;

            Map<String, List<String>> sectionCopies = new HashMap<>();
            for (Map.Entry<String, String> entry : valuesByPath.entrySet()) {
                String path = entry.getKey();
                int section = sectionNumber(path);
                if (section > 1) {
                    String basePath = path.substring(0, path.lastIndexOf(SECTION_SEPARATOR));
                    FieldFormat format = formatMap.getOrDefault(basePath, FieldFormat.PLAIN);
                    String target = sectionTarget(copy, basePath, section, sectionCopies);
                    if (target != null) {
                        applyPath(copy, target, entry.getValue(), format);
                    }
                    continue;
                }
                FieldFormat format = formatMap.getOrDefault(path, FieldFormat.PLAIN);
                applyPath(copy, path, entry.getValue(), format);
            }

            if (excludedPaths != null) {
                for (String excluded : excludedPaths) {
                    removePath(copy, excluded);
                }
            }

            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }

            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(outputFile, copy);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> indexBlocks(List<DocumentBlock> blocks) {
        Map<String, String> values = new LinkedHashMap<>();
        for (DocumentBlock block : blocks) {
            if (block.getId() == null) {
                continue;
            }
            values.put(normalizePath(block.getId()),
                    block.getValue() == null ? "" : block.getValue());
        }
        return values;
    }

    private String normalizePath(String path) {
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private void applyPath(ObjectNode root,
                           String path,
                           String value,
                           FieldFormat format) {

        String[] segments = path.startsWith("/")
                ? path.substring(1).split("/")
                : path.split("/");

        if (segments.length == 0) {
            return;
        }

        ObjectNode current = root;
        for (int i = 0; i < segments.length - 1; i++) {
            String segment = segments[i];
            JsonNode child = current.get(segment);
            if (child == null || !child.isObject()) {
                return;
            }
            current = (ObjectNode) child;
        }

        String last = segments[segments.length - 1];

        if (format == FieldFormat.LIST) {
            ObjectNode listNode;
            if (current.has(last) && current.get(last).isObject()) {
                listNode = (ObjectNode) current.get(last);
            } else {
                listNode = current.putObject(last);
                listNode.put("jcr:primaryType", "nt:unstructured");
            }
            updateListItems(listNode, value);
        } else {
            current.put(last, transformValue(format, value));
        }
    }

    /** 1-based section from a {@code path#n} marker; 1 when there is no numeric suffix. */
    static int sectionNumber(String path) {
        int hash = path.lastIndexOf(SECTION_SEPARATOR);
        if (hash <= 0 || hash == path.length() - 1) {
            return 1;
        }
        try {
            return Integer.parseInt(path.substring(hash + 1));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * Property path inside the clone of the base path's component for section {@code n}.
     * Clones are inserted as siblings directly after the previous section's component.
     */
    private String sectionTarget(ObjectNode root,
                                 String basePath,
                                 int section,
                                 Map<String, List<String>> sectionCopies) {
        String[] segments = basePath.substring(1).split("/");
        if (segments.length < 3) {
            return null;
        }
        ObjectNode container = root;
        for (int i = 0; i < segments.length - 2; i++) {
            JsonNode child = container.get(segments[i]);
            if (child == null || !child.isObject()) {
                return null;
            }
            container = (ObjectNode) child;
        }
        String baseName = segments[segments.length - 2];
        String property = segments[segments.length - 1];
        JsonNode base = container.get(baseName);
        if (base == null || !base.isObject()) {
            return null;
        }

        String componentPath = basePath.substring(0, basePath.lastIndexOf('/'));
        List<String> names = sectionCopies.computeIfAbsent(componentPath, k -> {
            List<String> list = new ArrayList<>();
            list.add(baseName);
            return list;
        });
        while (names.size() < section) {
            int n = names.size() + 1;
            String name = freeName(container, baseName + "_" + n);
            ObjectNode clone = ((ObjectNode) base).deepCopy();
            stripInstanceProperties(clone);
            insertAfter(container, names.get(names.size() - 1), name, clone);
            names.add(name);
        }
        return componentPath.substring(0, componentPath.lastIndexOf('/') + 1)
                + names.get(section - 1) + "/" + property;
    }

    private static String freeName(ObjectNode container, String candidate) {
        if (!container.has(candidate)) {
            return candidate;
        }
        int i = 1;
        while (container.has(candidate + "_" + i)) {
            i++;
        }
        return candidate + "_" + i;
    }

    private static void insertAfter(ObjectNode container, String afterKey, String key, JsonNode value) {
        Map<String, JsonNode> ordered = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = container.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            ordered.put(field.getKey(), field.getValue());
            if (field.getKey().equals(afterKey)) {
                ordered.put(key, value);
            }
        }
        if (!ordered.containsKey(key)) {
            ordered.put(key, value);
        }
        container.removeAll();
        container.setAll(ordered);
    }

    private static void stripInstanceProperties(ObjectNode node) {
        node.remove(INSTANCE_PROPERTIES);
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            JsonNode child = fields.next().getValue();
            if (child.isObject()) {
                stripInstanceProperties((ObjectNode) child);
            }
        }
    }

    private void removePath(ObjectNode root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return;
        }
        String normalized = normalizePath(path);
        String[] segments = normalized.substring(1).split("/");
        if (segments.length == 0 || segments[0].isEmpty()) {
            return;
        }
        ObjectNode current = root;
        for (int i = 0; i < segments.length - 1; i++) {
            JsonNode child = current.get(segments[i]);
            if (child == null || !child.isObject()) {
                return;
            }
            current = (ObjectNode) child;
        }
        current.remove(segments[segments.length - 1]);
    }

    private String transformValue(FieldFormat format, String value) {
        if (value == null) {
            value = "";
        }
        if (format == FieldFormat.HTML) {
            return toHtml(value);
        }
        return value;
    }

    /** Visible for tests. Converts Word/plain text into RTE HTML, including bullet lists. */
    String toHtml(String value) {
        if (looksLikeHtml(value)) {
            return value;
        }

        String[] lines = value.split("\\R", -1);
        StringBuilder html = new StringBuilder();
        List<String> paragraph = new ArrayList<>();
        List<String> list = new ArrayList<>();

        for (String raw : lines) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                flushList(html, list);
                flushParagraph(html, paragraph);
                continue;
            }
            if (trimmed.matches("^[-*]\\s+.+")) {
                flushParagraph(html, paragraph);
                list.add(trimmed.replaceFirst("^[-*]\\s+", ""));
            } else {
                flushList(html, list);
                paragraph.add(trimmed);
            }
        }
        flushList(html, list);
        flushParagraph(html, paragraph);

        if (html.length() == 0) {
            return "<p></p>";
        }
        return html.toString().trim();
    }

    private void flushParagraph(StringBuilder html, List<String> paragraph) {
        if (paragraph.isEmpty()) {
            return;
        }
        String joined = String.join("\n", paragraph);
        html.append("<p>").append(escapeXml(joined).replace("\n", "<br/>")).append("</p>\n");
        paragraph.clear();
    }

    private void flushList(StringBuilder html, List<String> list) {
        if (list.isEmpty()) {
            return;
        }
        html.append("<ul>\n");
        for (String item : list) {
            html.append("<li>").append(escapeXml(item)).append("</li>\n");
        }
        html.append("</ul>\n");
        list.clear();
    }

    private boolean looksLikeHtml(String value) {
        String trimmed = value.trim();
        return trimmed.startsWith("<") && trimmed.contains(">");
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void updateListItems(ObjectNode itemsNode, String value) {
        List<String> items = parseListItems(value);
        if (items.isEmpty()) {
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            String itemKey = "item" + i;
            ObjectNode itemNode;
            if (itemsNode.has(itemKey) && itemsNode.get(itemKey).isObject()) {
                itemNode = (ObjectNode) itemsNode.get(itemKey);
            } else {
                itemNode = itemsNode.putObject(itemKey);
                itemNode.put("jcr:primaryType", "nt:unstructured");
            }
            itemNode.put("text", items.get(i));
        }
    }

    private List<String> parseListItems(String value) {
        List<String> items = new ArrayList<>();
        String[] lines = value.split("\\R");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            trimmed = trimmed.replaceFirst("^[-*]\\s+", "");
            items.add(trimmed);
        }

        if (items.size() == 1 && items.get(0).contains(" - ")) {
            String[] parts = items.get(0).split("\\s+-\\s+");
            if (parts.length > 1) {
                items.clear();
                for (String part : parts) {
                    String trimmed = part.trim().replaceFirst("^[-*]\\s+", "");
                    if (!trimmed.isEmpty()) {
                        items.add(trimmed);
                    }
                }
            }
        }

        return items;
    }
}
