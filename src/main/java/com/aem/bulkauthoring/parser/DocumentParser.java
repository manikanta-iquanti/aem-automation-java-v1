package com.aem.bulkauthoring.parser;

import com.aem.bulkauthoring.model.document.DocumentBlock;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;

import javax.xml.namespace.QName;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DocumentParser {

    public List<DocumentBlock> parse(String file) {

        List<DocumentBlock> blocks = new ArrayList<>();

        try (FileInputStream in = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(in)) {

            String currentId = null;
            StringBuilder value = new StringBuilder();
            int sequence = 1;

            for (XWPFParagraph p : document.getParagraphs()) {

                String text = paragraphText(p).trim();

                if (text.isEmpty()) {
                    continue;
                }

                if (text.startsWith("[[") && text.endsWith("]]")) {

                    if (currentId != null) {

                        DocumentBlock block = new DocumentBlock();
                        block.setSequence(sequence++);
                        block.setId(currentId);
                        block.setValue(value.toString().trim());

                        blocks.add(block);
                    }

                    currentId = text.substring(2, text.length() - 2);

                    value.setLength(0);

                } else {

                    if (value.length() > 0) {
                        value.append("\n");
                    }

                    value.append(text);
                }
            }

            if (currentId != null) {

                DocumentBlock block = new DocumentBlock();
                block.setSequence(sequence);
                block.setId(currentId);
                block.setValue(value.toString().trim());

                blocks.add(block);
            }

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

        return blocks;
    }

    /**
     * Like {@link XWPFParagraph#getText()} but preserves soft breaks as {@code \n}.
     */
    private String paragraphText(XWPFParagraph paragraph) {
        StringBuilder sb = new StringBuilder();

        for (XWPFRun run : paragraph.getRuns()) {
            CTR ctr = run.getCTR();
            if (ctr == null) {
                String text = run.text();
                if (text != null) {
                    sb.append(text);
                }
                continue;
            }

            try (XmlCursor cursor = ctr.newCursor()) {
                if (!cursor.toFirstChild()) {
                    continue;
                }
                do {
                    QName name = cursor.getName();
                    if (name == null) {
                        continue;
                    }
                    String local = name.getLocalPart();
                    if ("t".equals(local)) {
                        String text = cursor.getTextValue();
                        if (text != null) {
                            sb.append(text);
                        }
                    } else if ("br".equals(local) || "cr".equals(local)) {
                        sb.append('\n');
                    } else if ("tab".equals(local)) {
                        sb.append('\t');
                    }
                } while (cursor.toNextSibling());
            }
        }

        if (sb.length() == 0) {
            String fallback = paragraph.getText();
            return fallback == null ? "" : fallback;
        }

        return sb.toString();
    }
}
