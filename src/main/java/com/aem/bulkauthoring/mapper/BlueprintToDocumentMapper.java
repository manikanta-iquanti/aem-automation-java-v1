package com.aem.bulkauthoring.mapper;

import com.aem.bulkauthoring.model.Blueprint;
import com.aem.bulkauthoring.model.BlueprintComponent;
import com.aem.bulkauthoring.model.BlueprintProperty;
import com.aem.bulkauthoring.model.document.DocumentBlock;

import java.util.ArrayList;
import java.util.List;

public class BlueprintToDocumentMapper {

    public List<DocumentBlock> map(Blueprint blueprint) {

        List<DocumentBlock> blocks = new ArrayList<>();

        int sequence = 1;

        for (BlueprintComponent component : blueprint.getComponents()) {

            String resourceType = component.getResourceType();

            switch (resourceType) {

                case "my-aem-site53/components/meridian-article-hero":

                    blocks.add(create(sequence++, "TITLE",
                            property(component, "title")));

                    blocks.add(create(sequence++, "DEK",
                            property(component, "dek")));

                    break;

                case "my-aem-site53/components/meridian-article-heading":

                    blocks.add(create(sequence++, "H2",
                            property(component, "text")));

                    break;

                case "my-aem-site53/components/meridian-article-paragraph":

                    blocks.add(create(sequence++, "PARAGRAPH",
                            stripHtml(property(component, "body"))));

                    break;

                case "my-aem-site53/components/meridian-article-list":

                    blocks.add(create(sequence++, "LIST",
                            "[LIST]"));

                    break;

                case "my-aem-site53/components/meridian-article-callout":

                    blocks.add(create(sequence++, "CALLOUT",
                            stripHtml(property(component, "body"))));

                    break;

                default:
                    break;
            }
        }

        return blocks;
    }

    private DocumentBlock create(int seq, String type, String value) {

        DocumentBlock block = new DocumentBlock();

        block.setSequence(seq);
        block.setId(type);
        block.setValue(value);

        return block;
    }

    private String property(BlueprintComponent component, String name) {

        for (BlueprintProperty property : component.getProperties()) {

            if (property.getName().equals(name)) {
                return property.getValue();
            }
        }

        return "";
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]*>", "");
    }
}