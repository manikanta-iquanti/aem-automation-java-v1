package com.aem.bulkauthoring.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class BlueprintComponent {

    private String name;

    private String resourceType;

    private String path;

    private Map<String, String> properties = new LinkedHashMap<>();

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

    public Map<String, String> getProperties() {
        return properties;
    }

}