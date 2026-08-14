package com.aem.bulkauthoring.docadapt.bind;

import com.aem.bulkauthoring.docadapt.mapping.SlotBinding;
import com.aem.bulkauthoring.docadapt.strategy.ExtractedSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BoundSlots {

    private final Map<String, String> pathToValue;
    private final Map<String, String> pathToUnit;
    private final List<SlotBinding> bindings;
    private final String bindStrategy;

    public BoundSlots(Map<String, String> pathToValue,
                      Map<String, String> pathToUnit,
                      List<SlotBinding> bindings,
                      String bindStrategy) {
        this.pathToValue = Collections.unmodifiableMap(new LinkedHashMap<>(pathToValue));
        this.pathToUnit = Collections.unmodifiableMap(new LinkedHashMap<>(
                pathToUnit == null ? Map.of() : pathToUnit));
        this.bindings = Collections.unmodifiableList(new ArrayList<>(
                bindings == null ? List.of() : bindings));
        this.bindStrategy = bindStrategy == null ? "auto" : bindStrategy;
    }

    public Map<String, String> getPathToValue() {
        return pathToValue;
    }

    public Map<String, String> getPathToUnit() {
        return pathToUnit;
    }

    public List<SlotBinding> getBindings() {
        return bindings;
    }

    public String getBindStrategy() {
        return bindStrategy;
    }

    public List<ExtractedSlot> toExtractedSlots() {
        List<ExtractedSlot> out = new ArrayList<>();
        for (Map.Entry<String, String> e : pathToValue.entrySet()) {
            String value = e.getValue() == null ? "" : e.getValue();
            out.add(new ExtractedSlot(e.getKey(), value, truncate(value)));
        }
        return out;
    }

    private static String truncate(String s) {
        String t = s.trim();
        if (t.length() <= 500) {
            return t;
        }
        return t.substring(0, 500) + "…";
    }
}
