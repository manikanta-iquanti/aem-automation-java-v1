package com.aem.bulkauthoring.blueprint;

/**
 * One editable DOCX block for a blueprint profile.
 * {@code property} is relative to the component path (e.g. {@code title})
 * or an absolute JCR path starting with {@code /} (e.g. {@code /jcr:content/jcr:title}).
 */
public class EditableField {

    private final String property;
    private final FieldFormat format;

    public EditableField(String property, FieldFormat format) {
        this.property = property;
        this.format = format;
    }

    public static EditableField plain(String property) {
        return new EditableField(property, FieldFormat.PLAIN);
    }

    public static EditableField html(String property) {
        return new EditableField(property, FieldFormat.HTML);
    }

    public static EditableField list(String property) {
        return new EditableField(property, FieldFormat.LIST);
    }

    public String getProperty() {
        return property;
    }

    public FieldFormat getFormat() {
        return format;
    }

    public boolean isAbsolute() {
        return property.startsWith("/");
    }
}
