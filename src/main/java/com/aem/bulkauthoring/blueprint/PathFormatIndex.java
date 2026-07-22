package com.aem.bulkauthoring.blueprint;

import com.aem.bulkauthoring.model.Blueprint;
import com.aem.bulkauthoring.model.BlueprintComponent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps marker paths (as written in the DOCX) to {@link FieldFormat} using the
 * same profile field allow-list used for template generation.
 */
public final class PathFormatIndex {

    private PathFormatIndex() {
    }

    public static Map<String, FieldFormat> build(Blueprint blueprint,
                                                 BlueprintTemplateProfile profile) {
        Map<String, FieldFormat> formats = new LinkedHashMap<>();

        for (EditableField field : profile.pageFields()) {
            formats.put(normalize(field.getProperty()), field.getFormat());
        }

        for (BlueprintComponent component : blueprint.getComponents()) {
            List<EditableField> fields = profile.fieldsFor(
                    component.getResourceType(), component.getPath());
            for (EditableField field : fields) {
                String path = field.isAbsolute()
                        ? field.getProperty()
                        : component.getPath() + "/" + field.getProperty();
                formats.put(normalize(path), field.getFormat());
            }
        }

        return Collections.unmodifiableMap(formats);
    }

    private static String normalize(String path) {
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
