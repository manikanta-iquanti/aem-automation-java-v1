package com.aem.bulkauthoring.blueprint;

import java.util.Collections;
import java.util.List;

/**
 * Blueprint-specific allow-list of editable fields for DOCX template generation.
 *
 * <p>When adding a new blueprint:
 * <ol>
 *   <li>Drop JSON under {@code input/blueprint/}</li>
 *   <li>Create {@code profiles/MyBlueprintProfile.java} listing resource types + fields</li>
 *   <li>Register it in {@link BlueprintProfileRegistry}</li>
 *   <li>Set {@code BLUEPRINT_KEY} / blueprint file in {@code Main}, run generate-template</li>
 * </ol>
 */
public interface BlueprintTemplateProfile {

    /** Registry key, e.g. {@code normal-page}, {@code meridian-article}. */
    String id();

    /**
     * Editable fields for a component {@code sling:resourceType}.
     * Return an empty list to skip the component (containers, chrome, etc.).
     */
    List<EditableField> fieldsFor(String resourceType);

    /** Page-level fields (absolute paths), e.g. {@code /jcr:content/jcr:title}. */
    default List<EditableField> pageFields() {
        return Collections.emptyList();
    }
}
