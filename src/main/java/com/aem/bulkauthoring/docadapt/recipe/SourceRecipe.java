package com.aem.bulkauthoring.docadapt.recipe;

import com.aem.bulkauthoring.docadapt.content.ContentDocument;
import com.aem.bulkauthoring.docadapt.model.NormalizedDocument;

/** One source-document family. Outputs the canonical content model. */
public interface SourceRecipe {

    String id();

    int score(NormalizedDocument doc);

    ContentDocument extract(NormalizedDocument doc);
}
