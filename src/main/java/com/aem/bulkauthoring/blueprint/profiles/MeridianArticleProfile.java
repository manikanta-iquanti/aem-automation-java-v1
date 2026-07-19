package com.aem.bulkauthoring.blueprint.profiles;

import com.aem.bulkauthoring.blueprint.BlueprintTemplateProfile;
import com.aem.bulkauthoring.blueprint.EditableField;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Editable fields for {@code input/blueprint/page1.json} (Meridian article template).
 */
public class MeridianArticleProfile implements BlueprintTemplateProfile {

    private static final String HERO =
            "my-aem-site53/components/meridian-article-hero";
    private static final String HEADING =
            "my-aem-site53/components/meridian-article-heading";
    private static final String PARAGRAPH =
            "my-aem-site53/components/meridian-article-paragraph";
    private static final String CALLOUT =
            "my-aem-site53/components/meridian-article-callout";
    private static final String LIST =
            "my-aem-site53/components/meridian-article-list";
    private static final String TAGS =
            "my-aem-site53/components/meridian-article-tags";
    private static final String AUTHOR_CARD =
            "my-aem-site53/components/meridian-author-card";

    private final Map<String, List<EditableField>> byResourceType = new HashMap<>();

    public MeridianArticleProfile() {
        byResourceType.put(HERO, Arrays.asList(
                EditableField.plain("title"),
                EditableField.plain("dek"),
                EditableField.plain("eyebrow"),
                EditableField.plain("readTime"),
                EditableField.plain("authorName"),
                EditableField.plain("publishDateText"),
                EditableField.plain("image"),
                EditableField.plain("imageAlt")
        ));
        byResourceType.put(HEADING, Arrays.asList(
                EditableField.plain("text"),
                EditableField.plain("anchorId")
        ));
        byResourceType.put(PARAGRAPH, Collections.singletonList(
                EditableField.html("body")
        ));
        byResourceType.put(CALLOUT, Arrays.asList(
                EditableField.html("body"),
                EditableField.plain("eyebrow"),
                EditableField.plain("initial")
        ));
        byResourceType.put(LIST, Collections.singletonList(
                EditableField.list("items")
        ));
        byResourceType.put(TAGS, Collections.singletonList(
                EditableField.list("tags")
        ));
        byResourceType.put(AUTHOR_CARD, Arrays.asList(
                EditableField.plain("authorName"),
                EditableField.plain("initials"),
                EditableField.plain("dropInitial"),
                EditableField.plain("label")
        ));
    }

    @Override
    public String id() {
        return "meridian-article";
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
}
