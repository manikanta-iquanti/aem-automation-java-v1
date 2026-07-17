package com.aem.bulkauthoring.model;

import java.util.ArrayList;
import java.util.List;

public class Blueprint {

    private final List<BlueprintComponent> components = new ArrayList<>();

    public List<BlueprintComponent> getComponents() {
        return components;
    }

    public void addComponent(BlueprintComponent component) {
        components.add(component);
    }
}