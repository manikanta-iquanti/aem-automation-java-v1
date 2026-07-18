package com.aem.bulkauthoring.generator;

import com.aem.bulkauthoring.model.Blueprint;
import com.aem.bulkauthoring.model.BlueprintComponent;
import com.aem.bulkauthoring.model.BlueprintProperty;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;

public class DocumentTemplateGenerator {

    public void generate(Blueprint blueprint,
                         String outputFile) {

        try (XWPFDocument document = new XWPFDocument()) {

            for (BlueprintComponent component : blueprint.getComponents()) {

                writeComponent(document, component);
            }

            FileOutputStream out =
                    new FileOutputStream(outputFile);

            document.write(out);

            out.close();

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

    private void writeComponent(XWPFDocument document,
                                BlueprintComponent component) {

        String type = component.getResourceType();

        switch (type) {

            case "my-aem-site53/components/meridian-article-hero":

                addHeading(document,
                        get(component, "title"),
                        "TITLE");

                addParagraph(document,
                        get(component, "dek"),
                        "DEK");

                break;

            case "my-aem-site53/components/meridian-article-heading":

                addHeading(document,
                        get(component, "text"),
                        "H2");

                break;

            case "my-aem-site53/components/meridian-article-paragraph":

                addParagraph(document,
                        stripHtml(get(component, "body")),
                        "PARAGRAPH");

                break;

            case "my-aem-site53/components/meridian-article-list":

                addParagraph(document,
                        "[LIST]",
                        "LIST");

                break;

            case "my-aem-site53/components/meridian-pull-quote":

                addParagraph(document,
                        get(component, "quote"),
                        "QUOTE");

                break;

            case "my-aem-site53/components/meridian-article-callout":

                addParagraph(document,
                        stripHtml(get(component, "body")),
                        "CALLOUT");

                break;

            default:
                break;
        }

    }

    private void addHeading(XWPFDocument doc,
                            String value,
                            String label) {

        XWPFParagraph p = doc.createParagraph();

        p.setStyle("Heading1");

        XWPFRun run = p.createRun();

        run.setBold(true);

        run.setText(label + ": " + value);

    }

    private void addParagraph(XWPFDocument doc,
                              String value,
                              String label) {

        XWPFParagraph p = doc.createParagraph();

        XWPFRun run = p.createRun();

        run.setText(label + ": " + value);

    }

    private String get(BlueprintComponent component,
                       String propertyName) {

        for (BlueprintProperty property : component.getProperties()) {

            if (property.getName().equals(propertyName)) {

                return property.getValue();

            }

        }

        return "";

    }

    private String stripHtml(String html) {

        return html.replaceAll("<[^>]*>", "");

    }

}