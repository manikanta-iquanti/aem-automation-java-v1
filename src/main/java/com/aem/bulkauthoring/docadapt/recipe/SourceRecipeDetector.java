package com.aem.bulkauthoring.docadapt.recipe;

import com.aem.bulkauthoring.docadapt.model.NormalizedDocument;

import java.util.List;
import java.util.Locale;

public final class SourceRecipeDetector {

    private final List<SourceRecipe> recipes;

    public SourceRecipeDetector() {
        this(List.of(new UsbTableFormRecipe(), new WordOutlineRecipe()));
    }

    SourceRecipeDetector(List<SourceRecipe> recipes) {
        this.recipes = recipes;
    }

    public List<SourceRecipe> recipes() {
        return recipes;
    }

    public SourceRecipe resolve(String id, NormalizedDocument doc) {
        String key = id == null ? "auto" : id.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty() || "auto".equals(key)) {
            return pickBest(doc);
        }
        for (SourceRecipe recipe : recipes) {
            if (recipe.id().equals(key)) {
                return recipe;
            }
        }
        throw new IllegalArgumentException("Unknown source recipe: " + id);
    }

    public SourceRecipe pickBest(NormalizedDocument doc) {
        SourceRecipe best = recipes.get(recipes.size() - 1);
        int bestScore = -1;
        for (SourceRecipe recipe : recipes) {
            int score = recipe.score(doc);
            if (score > bestScore) {
                bestScore = score;
                best = recipe;
            }
        }
        return best;
    }

    public List<String> recipeIds() {
        List<String> ids = new java.util.ArrayList<>();
        ids.add("auto");
        for (SourceRecipe recipe : recipes) {
            ids.add(recipe.id());
        }
        return ids;
    }
}
