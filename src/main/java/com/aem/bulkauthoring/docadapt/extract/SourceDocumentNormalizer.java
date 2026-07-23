package com.aem.bulkauthoring.docadapt.extract;

import com.aem.bulkauthoring.docadapt.model.DocBlock;
import com.aem.bulkauthoring.docadapt.model.NormalizedDocument;
import com.aem.bulkauthoring.docadapt.model.ParagraphBlock;
import com.aem.bulkauthoring.docadapt.model.TableBlock;
import com.aem.bulkauthoring.docadapt.model.TableCell;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** POI → generic ordered blocks. No source-family logic. */
public final class SourceDocumentNormalizer {

    public NormalizedDocument normalize(File docx) throws IOException {
        try (FileInputStream in = new FileInputStream(docx);
             XWPFDocument document = new XWPFDocument(in)) {
            List<DocBlock> blocks = new ArrayList<>();
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph) {
                    XWPFParagraph p = (XWPFParagraph) element;
                    String text = p.getText();
                    if (text == null || text.isBlank()) {
                        continue;
                    }
                    String style = p.getStyle() == null ? "" : p.getStyle();
                    blocks.add(new ParagraphBlock(text.trim(), style));
                } else if (element instanceof XWPFTable) {
                    blocks.add(readTable((XWPFTable) element));
                }
            }
            return new NormalizedDocument(docx.getName(), blocks);
        }
    }

    private static TableBlock readTable(XWPFTable table) {
        List<List<TableCell>> rows = new ArrayList<>();
        for (XWPFTableRow row : table.getRows()) {
            List<TableCell> cells = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                List<String> paras = new ArrayList<>();
                for (XWPFParagraph p : cell.getParagraphs()) {
                    String t = p.getText();
                    if (t != null && !t.isBlank()) {
                        paras.add(t.trim());
                    }
                }
                cells.add(new TableCell(paras));
            }
            rows.add(cells);
        }
        return new TableBlock(rows);
    }
}
