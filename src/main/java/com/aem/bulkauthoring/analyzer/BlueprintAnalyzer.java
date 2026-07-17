package com.aem.bulkauthoring.analyzer;

import com.aem.bulkauthoring.model.Blueprint;
import com.aem.bulkauthoring.model.BlueprintComponent;
import com.aem.bulkauthoring.model.BlueprintProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class BlueprintAnalyzer {

    private final ObjectMapper mapper = new ObjectMapper();

    public Blueprint analyze(File jsonFile) {

        try {

            JsonNode root = mapper.readTree(jsonFile);

            Blueprint blueprint = new Blueprint();

            walk(root, "", blueprint);

            return blueprint;

        } catch (IOException e) {
            throw new RuntimeException("Unable to parse JSON blueprint.", e);
        }
    }

    private void walk(JsonNode node,
                      String currentPath,
                      Blueprint blueprint) {

        if (!node.isObject()) {
            return;
        }

        if (node.has("sling:resourceType")) {

            BlueprintComponent component = new BlueprintComponent();

            component.setName(lastSegment(currentPath));

            component.setPath(currentPath);

            component.setResourceType(
                    node.get("sling:resourceType").asText());

            Iterator<Map.Entry<String, JsonNode>> fields =
                    node.fields();

            while (fields.hasNext()) {

                Map.Entry<String, JsonNode> field = fields.next();

                String key = field.getKey();

                if (shouldIgnore(key))
                    continue;

                JsonNode value = field.getValue();

                if (value.isValueNode()) {

                    component.addProperty(
                            new BlueprintProperty(
                                    key,
                                    value.asText()
                            ));
                }
            }

            blueprint.addComponent(component);
        }

        Iterator<Map.Entry<String, JsonNode>> children =
                node.fields();

        while (children.hasNext()) {

            Map.Entry<String, JsonNode> child =
                    children.next();

            if (child.getValue().isObject()) {

                walk(
                        child.getValue(),
                        currentPath + "/" + child.getKey(),
                        blueprint
                );
            }
        }

    }

    private boolean shouldIgnore(String key) {

        return key.startsWith("jcr:")
                || key.startsWith("cq:")
                || key.equals("sling:resourceType");
    }

    private String lastSegment(String path) {

        if (path.isEmpty())
            return "";

        int i = path.lastIndexOf('/');

        return path.substring(i + 1);
    }

}