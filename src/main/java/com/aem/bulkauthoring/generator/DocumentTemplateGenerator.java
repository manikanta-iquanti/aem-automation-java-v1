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

        String rt = component.getResourceType();

        switch (rt) {

            case "my-aem-site53/components/meridian-article-hero":

                addEditableBlock(
                        document,
                        component.getPath() + "/title",
                        get(component, "title"));

                addEditableBlock(
                        document,
                        component.getPath() + "/dek",
                        get(component, "dek"));

                break;

            case "my-aem-site53/components/meridian-article-heading":

                addEditableBlock(
                        document,
                        component.getPath() + "/text",
                        get(component, "text"));

                break;

            case "my-aem-site53/components/meridian-article-paragraph":

                addEditableBlock(
                        document,
                        component.getPath() + "/body",
                        stripHtml(get(component, "body")));

                break;

            case "my-aem-site53/components/meridian-article-callout":

                addEditableBlock(
                        document,
                        component.getPath() + "/body",
                        stripHtml(get(component, "body")));

                break;

            case "my-aem-site53/components/meridian-article-list":

                addEditableBlock(
                        document,
                        component.getPath() + "/items",
                        "- Item 1\n- Item 2\n- Item 3");

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

    private void addListBlock(XWPFDocument doc) {

        XWPFParagraph p;

        p = doc.createParagraph();
        p.createRun().setText("LIST:");

        p = doc.createParagraph();
        p.createRun().setText("- Item 1");

        p = doc.createParagraph();
        p.createRun().setText("- Item 2");

        p = doc.createParagraph();
        p.createRun().setText("- Item 3");

        p = doc.createParagraph();
        p.createRun().setText("END_LIST");
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

    private void addEditableBlock(XWPFDocument doc,
                                  String id,
                                  String value) {

        XWPFParagraph p;

        // Marker (do not edit)
        p = doc.createParagraph();

        XWPFRun run = p.createRun();
        run.setBold(true);
        run.setColor("808080");
        run.setText("[[" + id + "]]");

        // Editable content
        p = doc.createParagraph();
        p.createRun().setText(value);

        // Blank line
        doc.createParagraph();
    }

}