package com.aem.bulkauthoring.docadapt.mapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class SourceMapping {

    private String id;
    private String strategy = "rule-engine";
    private String targetTemplate;
    private String sourceDir = "input/raw-source-docs";
    private String outputDir = "output/adapted-articles";
    private String sourceRecipe;
    private String bindStrategy;
    private List<SlotBinding> bindings = new ArrayList<>();
    private List<SlotRule> slots = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getTargetTemplate() {
        return targetTemplate;
    }

    public void setTargetTemplate(String targetTemplate) {
        this.targetTemplate = targetTemplate;
    }

    public String getSourceDir() {
        return sourceDir;
    }

    public void setSourceDir(String sourceDir) {
        this.sourceDir = sourceDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getSourceRecipe() {
        return sourceRecipe;
    }

    public void setSourceRecipe(String sourceRecipe) {
        this.sourceRecipe = sourceRecipe;
    }

    public String getBindStrategy() {
        return bindStrategy;
    }

    public void setBindStrategy(String bindStrategy) {
        this.bindStrategy = bindStrategy;
    }

    public List<SlotBinding> getBindings() {
        return bindings;
    }

    public void setBindings(List<SlotBinding> bindings) {
        this.bindings = bindings == null ? new ArrayList<>() : bindings;
    }

    public List<SlotRule> getSlots() {
        return slots;
    }

    public void setSlots(List<SlotRule> slots) {
        this.slots = slots == null ? new ArrayList<>() : slots;
    }

    public boolean hasLegacySlots() {
        return slots != null && !slots.isEmpty();
    }
}
