package com.aem.bulkauthoring.generator;

import com.aem.bulkauthoring.blueprint.BlueprintTemplateProfile;
import com.aem.bulkauthoring.blueprint.EditableField;
import com.aem.bulkauthoring.blueprint.FieldFormat;
import com.aem.bulkauthoring.model.Blueprint;
import com.aem.bulkauthoring.model.BlueprintComponent;
import com.aem.bulkauthoring.model.BlueprintProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic DOCX template writer. Blueprint-specific field lists come from
 * {@link BlueprintTemplateProfile}.
 */
public class DocumentTemplateGenerator {

    private final ObjectMapper mapper = new ObjectMapper();

    public void generate(Blueprint blueprint,
                         BlueprintTemplateProfile profile,
                         File blueprintJson,
                         String outputFile) {

        try (XWPFDocument document = new XWPFDocument()) {

            JsonNode root = mapper.readTree(blueprintJson);

            for (EditableField field : profile.pageFields()) {
                String marker = field.getProperty();
                String value = formatValue(
                        field.getFormat(),
                        resolveAbsolute(root, marker));
                addEditableBlock(document, marker, value);
            }

            for (BlueprintComponent component : blueprint.getComponents()) {
                List<EditableField> fields =
                        profile.fieldsFor(component.getResourceType());

                for (EditableField field : fields) {
                    String marker = field.isAbsolute()
                            ? field.getProperty()
                            : component.getPath() + "/" + field.getProperty();

                    String raw = field.isAbsolute()
                            ? resolveAbsolute(root, field.getProperty())
                            : resolveComponentValue(root, component, field);

                    String value = formatValue(field.getFormat(), raw);
                    addEditableBlock(document, marker, value);
                }
            }

            File outFile = new File(outputFile);
            if (outFile.getParentFile() != null) {
                outFile.getParentFile().mkdirs();
            }

            try (FileOutputStream out = new FileOutputStream(outFile)) {
                document.write(out);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String resolveComponentValue(JsonNode root,
                                         BlueprintComponent component,
                                         EditableField field) {
        if (field.getFormat() == FieldFormat.LIST) {
            return resolveListSeed(
                    root,
                    component.getPath() + "/" + field.getProperty());
        }
        return get(component, field.getProperty());
    }

    private String resolveListSeed(JsonNode root, String absolutePath) {
        JsonNode itemsNode = resolveNode(root, absolutePath);
        if (itemsNode != null && itemsNode.isObject()) {
            List<String> lines = new ArrayList<>();
            for (int i = 0; ; i++) {
                JsonNode item = itemsNode.get("item" + i);
                if (item == null || !item.isObject()) {
                    break;
                }
                if (item.has("text")) {
                    lines.add("- " + item.get("text").asText());
                }
            }
            if (!lines.isEmpty()) {
                return String.join("\n", lines);
            }
        }
        return "- Item 1\n- Item 2\n- Item 3";
    }

    private String resolveAbsolute(JsonNode root, String absolutePath) {
        JsonNode node = resolveNode(root, absolutePath);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isValueNode()) {
            return node.asText();
        }
        return "";
    }

    private JsonNode resolveNode(JsonNode root, String absolutePath) {
        if (root == null || absolutePath == null || absolutePath.isEmpty()) {
            return null;
        }

        String path = absolutePath.startsWith("/")
                ? absolutePath.substring(1)
                : absolutePath;

        JsonNode node = root;
        for (String segment : path.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            if (node == null || !node.has(segment)) {
                return null;
            }
            node = node.get(segment);
        }
        return node;
    }

    private String formatValue(FieldFormat format, String raw) {
        if (raw == null) {
            raw = "";
        }
        switch (format) {
            case HTML:
                return stripHtml(raw);
            case LIST:
                if (raw.isBlank()) {
                    return "- Item 1\n- Item 2\n- Item 3";
                }
                return raw;
            case PLAIN:
            default:
                return raw;
        }
    }

    private String get(BlueprintComponent component, String propertyName) {
        for (BlueprintProperty property : component.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return property.getValue();
            }
        }
        return "";
    }

    private String stripHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p\\s*>", "\n")
                .replaceAll("(?i)</li\\s*>", "\n")
                .replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private void addEditableBlock(XWPFDocument doc,
                                  String id,
                                  String value) {

        XWPFParagraph markerPara = doc.createParagraph();
        XWPFRun markerRun = markerPara.createRun();
        markerRun.setBold(true);
        markerRun.setColor("808080");
        markerRun.setText("[[" + id + "]]");

        XWPFParagraph valuePara = doc.createParagraph();
        if (value != null && value.contains("\n")) {
            String[] lines = value.split("\\R", -1);
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) {
                    valuePara.createRun().addBreak();
                }
                valuePara.createRun().setText(lines[i]);
            }
        } else {
            valuePara.createRun().setText(value == null ? "" : value);
        }

        doc.createParagraph();
    }
}
