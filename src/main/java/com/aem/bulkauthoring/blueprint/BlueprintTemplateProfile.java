package com.aem.bulkauthoring.blueprint;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Blueprint-specific allow-list of editable fields for DOCX template generation.
 *
 * <p>Prefer data-driven profiles under {@code input/profiles/*.json}
 * (see {@link JsonBlueprintProfile}). Studio and CLI both load from the registry.
 */
public interface BlueprintTemplateProfile {

    /** Registry key, e.g. {@code normal-page}, {@code meridian-article}. */
    String id();

    /** Path to the blueprint infinity JSON used for analyze / generate / update. */
    File blueprintJson();

    /**
     * Editable fields for a component {@code sling:resourceType}.
     * Return an empty list to skip the component (containers, chrome, etc.).
     */
    List<EditableField> fieldsFor(String resourceType);

    /**
     * Editable fields for one component instance. Default ignores path
     * (resource-type allow-list). JSON profiles may scope by path.
     */
    default List<EditableField> fieldsFor(String resourceType, String componentPath) {
        return fieldsFor(resourceType);
    }

    /** Page-level fields (absolute paths), e.g. {@code /jcr:content/jcr:title}. */
    default List<EditableField> pageFields() {
        return Collections.emptyList();
    }

    /**
     * FileVault scaffold and install paths for Phase 2 package generation.
     */
    BlueprintPackageConfig packageConfig();
}
