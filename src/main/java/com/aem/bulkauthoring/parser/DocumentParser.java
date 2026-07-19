package com.aem.bulkauthoring.parser;

import com.aem.bulkauthoring.model.document.DocumentBlock;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

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

                String text = p.getText().trim();

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
}