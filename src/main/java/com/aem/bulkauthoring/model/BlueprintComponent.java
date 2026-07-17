package com.aem.bulkauthoring.model;

import java.util.ArrayList;
import java.util.List;

public class BlueprintComponent {

    private String name;

    private String resourceType;

    private String path;

    private final List<BlueprintProperty> properties = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<BlueprintProperty> getProperties() {
        return properties;
    }

    public void addProperty(BlueprintProperty property) {
        properties.add(property);
    }
}