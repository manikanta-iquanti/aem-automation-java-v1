package com.aem.bulkauthoring.model;

public class BlueprintProperty {

    private String name;

    private String value;

    public BlueprintProperty() {
    }

    public BlueprintProperty(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}