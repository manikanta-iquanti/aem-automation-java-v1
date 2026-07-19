package com.aem.bulkauthoring.blueprint.profiles;

import com.aem.bulkauthoring.blueprint.BlueprintPackageConfig;
import com.aem.bulkauthoring.blueprint.BlueprintTemplateProfile;
import com.aem.bulkauthoring.blueprint.EditableField;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Editable fields for {@code input/blueprint/page.json} (normal page-content template).
 */
public class NormalPageProfile implements BlueprintTemplateProfile {

    private static final String AUTHOR =
            "my-aem-site53/components/author";
    private static final String TEXT =
            "my-aem-site53/components/text";

    private final Map<String, List<EditableField>> byResourceType = new HashMap<>();

    public NormalPageProfile() {
        byResourceType.put(AUTHOR, Arrays.asList(
                EditableField.plain("fname"),
                EditableField.plain("lname"),
                EditableField.plain("professor")
        ));
        byResourceType.put(TEXT, Collections.singletonList(
                EditableField.html("text")
        ));
    }

    @Override
    public String id() {
        return "normal-page";
    }

    @Override
    public List<EditableField> fieldsFor(String resourceType) {
        return byResourceType.getOrDefault(resourceType, Collections.emptyList());
    }

    @Override
    public List<EditableField> pageFields() {
        return Collections.singletonList(
                EditableField.plain("/jcr:content/jcr:title")
        );
    }

    @Override
    public BlueprintPackageConfig packageConfig() {
        return new BlueprintPackageConfig(
                new File("input/blueprint-xml/normal-page1"),
                "/content/my-aem-site53/us/en/articles",
                "normal-page1",
                "bulk-normal-pages"
        );
    }
}
