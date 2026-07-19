package com.aem.bulkauthoring.packagebuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Serializes a cq:Page JSON blueprint into FileVault DocView .content.xml.
 */
public class DocViewXmlWriter {

    private static final Set<String> SKIP_PROPERTIES = Set.of(
            "jcr:created",
            "jcr:createdBy",
            "jcr:lastModified",
            "jcr:lastModifiedBy",
            "cq:lastModified",
            "cq:lastModifiedBy",
            "jcr:versionHistory",
            "jcr:predecessors",
            "jcr:baseVersion"
    );

    private static final String NAMESPACES =
            "xmlns:sling=\"http://sling.apache.org/jcr/sling/1.0\" "
                    + "xmlns:cq=\"http://www.day.com/jcr/cq/1.0\" "
                    + "xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" "
                    + "xmlns:mix=\"http://www.jcp.org/jcr/mix/1.0\" "
                    + "xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\"";

    public void write(JsonNode pageRoot, Writer writer) throws IOException {
        if (pageRoot == null || !pageRoot.isObject()) {
            throw new IllegalArgumentException("Page root must be a JSON object");
        }

        ObjectNode copy = pageRoot.deepCopy();
        assignFreshUuid(copy);

        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writeElement(writer, "jcr:root", copy, true, 0);
        writer.write("\n");
    }

    private void assignFreshUuid(ObjectNode page) {
        JsonNode content = page.get("jcr:content");
        if (content != null && content.isObject()) {
            ((ObjectNode) content).put("jcr:uuid", UUID.randomUUID().toString());
        }
    }

    private void writeElement(Writer writer,
                              String name,
                              ObjectNode node,
                              boolean isRoot,
                              int indent) throws IOException {

        Map<String, String> attributes = new LinkedHashMap<>();
        List<Map.Entry<String, ObjectNode>> children = new ArrayList<>();

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            if (SKIP_PROPERTIES.contains(key)) {
                continue;
            }

            if (value.isObject()) {
                children.add(Map.entry(key, (ObjectNode) value));
            } else if (value.isArray()) {
                String formatted = formatArray(value);
                if (formatted != null) {
                    attributes.put(key, formatted);
                }
            } else if (!value.isNull()) {
                attributes.put(key, formatScalar(value));
            }
        }

        writeIndent(writer, indent);
        writer.write("<");
        writer.write(name);

        if (isRoot) {
            writer.write(" ");
            writer.write(NAMESPACES);
        }

        if (attributes.isEmpty() && children.isEmpty()) {
            writer.write("/>");
            return;
        }

        for (Map.Entry<String, String> attr : attributes.entrySet()) {
            writer.write("\n");
            writeIndent(writer, indent + 1);
            writer.write(attr.getKey());
            writer.write("=\"");
            writer.write(escapeAttribute(attr.getValue()));
            writer.write("\"");
        }

        if (children.isEmpty()) {
            writer.write("/>");
            return;
        }

        writer.write(">");

        for (Map.Entry<String, ObjectNode> child : children) {
            writer.write("\n");
            writeElement(writer, child.getKey(), child.getValue(), false, indent + 1);
        }

        writer.write("\n");
        writeIndent(writer, indent);
        writer.write("</");
        writer.write(name);
        writer.write(">");
    }

    private String formatScalar(JsonNode value) {
        if (value.isBoolean()) {
            return "{Boolean}" + value.asBoolean();
        }
        return value.asText();
    }

    private String formatArray(JsonNode array) {
        if (array.size() == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.size(); i++) {
            JsonNode item = array.get(i);
            if (item.isObject() || item.isArray()) {
                return null;
            }
            if (i > 0) {
                sb.append(',');
            }
            sb.append(item.asText());
        }
        sb.append(']');
        return sb.toString();
    }

    private String escapeAttribute(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\r':
                    break;
                case '\n':
                    sb.append("&#10;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private void writeIndent(Writer writer, int indent) throws IOException {
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }
}
