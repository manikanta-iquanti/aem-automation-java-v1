package com.aem.bulkauthoring.updater;

import com.aem.bulkauthoring.model.document.DocumentBlock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies document blocks onto a JSON blueprint.
 * Block ids are path markers from the DOCX template, e.g.
 * {@code /jcr:content/root/container/mig_hero/title}.
 */
public class BlueprintUpdater {

    private final ObjectMapper mapper = new ObjectMapper();

    public void update(File originalBlueprint,
                       List<DocumentBlock> blocks,
                       File outputFile) {

        try {

            JsonNode root = mapper.readTree(originalBlueprint);
            ObjectNode copy = (ObjectNode) root.deepCopy();

            Map<String, String> valuesByPath = indexBlocks(blocks);
            apply(copy, "", valuesByPath);
            syncPageTitle(copy, valuesByPath);

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
            String path = normalizePath(block.getId());
            values.put(path, block.getValue() == null ? "" : block.getValue());
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

    private void apply(ObjectNode node,
                       String path,
                       Map<String, String> valuesByPath) {

        List<String> keys = new ArrayList<>();
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            keys.add(names.next());
        }

        for (String key : keys) {
            String childPath = path + "/" + key;
            JsonNode child = node.get(key);

            if (valuesByPath.containsKey(childPath)) {
                String value = valuesByPath.get(childPath);

                if (child != null && child.isObject()) {
                    updateListItems((ObjectNode) child, value);
                } else {
                    node.put(key, transformPropertyValue(key, value));
                }
            }

            if (child != null && child.isObject()) {
                apply((ObjectNode) child, childPath, valuesByPath);
            }
        }
    }

    private String transformPropertyValue(String property, String value) {
        if ("body".equals(property) && !looksLikeHtml(value)) {
            return "<p>" + escapeXml(value) + "</p>";
        }
        return value;
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

    private void syncPageTitle(ObjectNode root, Map<String, String> valuesByPath) {
        String heroTitle = valuesByPath.get(
                "/jcr:content/root/container/mig_hero/title");
        if (heroTitle == null || heroTitle.isBlank()) {
            return;
        }

        JsonNode content = root.get("jcr:content");
        if (content != null && content.isObject()) {
            ((ObjectNode) content).put("jcr:title", heroTitle);
        }
    }

}
