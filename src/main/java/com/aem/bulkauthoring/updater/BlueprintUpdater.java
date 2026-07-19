package com.aem.bulkauthoring.updater;

import com.aem.bulkauthoring.model.document.DocumentBlock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BlueprintUpdater {

    private final ObjectMapper mapper = new ObjectMapper();

    private int currentIndex = 0;

    public void update(File originalBlueprint,
                       List<DocumentBlock> blocks,
                       File outputFile) {

        try {

            JsonNode root = mapper.readTree(originalBlueprint);

            JsonNode copy = root.deepCopy();

            walk(copy, blocks);

            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(outputFile, copy);

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

    private void walk(JsonNode node,
                      List<DocumentBlock> blocks) {

        if (!node.isObject()) {
            return;
        }

        if (node.has("sling:resourceType")) {

            updateComponent((ObjectNode) node, blocks);

        }

        Iterator<Map.Entry<String, JsonNode>> iterator =
                node.fields();

        while (iterator.hasNext()) {

            JsonNode child = iterator.next().getValue();

            if (child.isObject()) {

                walk(child, blocks);

            }

        }

    }

    private void updateComponent(ObjectNode component,
                                 List<DocumentBlock> blocks) {

        String type =
                component.get("sling:resourceType").asText();

        if (type.contains("hero")) {

            replace(component, "title", "TITLE", blocks);
            replace(component, "dek", "DEK", blocks);

        }

        else if (type.contains("article-heading")) {

            replace(component, "text", "H2", blocks);

        }

        else if (type.contains("article-paragraph")) {

            replace(component, "body", "PARAGRAPH", blocks);

        }

        else if (type.contains("article-callout")) {

            replace(component, "body", "CALLOUT", blocks);

        }

    }

    private void replace(ObjectNode component,
                         String property,
                         String expectedType,
                         List<DocumentBlock> blocks) {

        while (currentIndex < blocks.size()) {

            DocumentBlock block = blocks.get(currentIndex);

            currentIndex++;

            if (!block.getId().equals(expectedType)) {
                continue;
            }

            if (component.has(property)) {

                component.put(property, block.getValue());

            }

            return;
        }

    }

}