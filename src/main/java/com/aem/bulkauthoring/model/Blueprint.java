package com.aem.bulkauthoring.model;

import java.util.ArrayList;
import java.util.List;

public class Blueprint {

    private BlueprintComponent rootComponent;

    private final List<BlueprintComponent> components = new ArrayList<>();

    public BlueprintComponent getRootComponent() {
        return rootComponent;
    }

    public void setRootComponent(BlueprintComponent rootComponent) {
        this.rootComponent = rootComponent;
    }

    public List<BlueprintComponent> getComponents() {
        return components;
    }

    public void addComponent(BlueprintComponent component) {
        components.add(component);
    }
}