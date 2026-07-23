package com.aem.bulkauthoring.docadapt.mapping;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ExtractRule {

    private String type;
    private String sectionContains;
    private String labelContains;
    private Integer valueColumn;
    private String startAfterSectionContains;
    private String stopBeforeSectionContains;
    private Integer elementColumn;
    private Integer contentColumn;
    private Map<String, String> tagMap = new LinkedHashMap<>();
    private String heading;
    private Integer index;
    private String value;
    private String join;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSectionContains() {
        return sectionContains;
    }

    public void setSectionContains(String sectionContains) {
        this.sectionContains = sectionContains;
    }

    public String getLabelContains() {
        return labelContains;
    }

    public void setLabelContains(String labelContains) {
        this.labelContains = labelContains;
    }

    public Integer getValueColumn() {
        return valueColumn;
    }

    public void setValueColumn(Integer valueColumn) {
        this.valueColumn = valueColumn;
    }

    public String getStartAfterSectionContains() {
        return startAfterSectionContains;
    }

    public void setStartAfterSectionContains(String startAfterSectionContains) {
        this.startAfterSectionContains = startAfterSectionContains;
    }

    public String getStopBeforeSectionContains() {
        return stopBeforeSectionContains;
    }

    public void setStopBeforeSectionContains(String stopBeforeSectionContains) {
        this.stopBeforeSectionContains = stopBeforeSectionContains;
    }

    public Integer getElementColumn() {
        return elementColumn;
    }

    public void setElementColumn(Integer elementColumn) {
        this.elementColumn = elementColumn;
    }

    public Integer getContentColumn() {
        return contentColumn;
    }

    public void setContentColumn(Integer contentColumn) {
        this.contentColumn = contentColumn;
    }

    public Map<String, String> getTagMap() {
        return tagMap;
    }

    public void setTagMap(Map<String, String> tagMap) {
        this.tagMap = tagMap == null ? new LinkedHashMap<>() : tagMap;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getJoin() {
        return join;
    }

    public void setJoin(String join) {
        this.join = join;
    }
}
