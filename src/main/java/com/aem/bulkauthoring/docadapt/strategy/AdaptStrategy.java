package com.aem.bulkauthoring.docadapt.strategy;

import com.aem.bulkauthoring.docadapt.mapping.SourceMapping;
import com.aem.bulkauthoring.docadapt.model.NormalizedDocument;

import java.util.List;

public interface AdaptStrategy {

    List<ExtractedSlot> apply(NormalizedDocument doc, SourceMapping mapping);
}
