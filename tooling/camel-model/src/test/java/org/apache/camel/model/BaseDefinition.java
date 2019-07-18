package org.apache.camel.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class BaseDefinition {

    protected final Map<String, Object> properties = new HashMap<>();

    protected void doSetProperty(String name, Object value) {
        properties.put(name, value);
    }

    protected Object doGetProperty(String name) {
        return properties.get(name);
    }

    protected Object doGetProperty(String name, Function<String, ?> mapping) {
        return properties.computeIfAbsent(name, mapping);
    }

    protected Map<String, Object> doGetProperties() {
        return properties;
    }

    protected List<?> newList(String key) {
        return new ArrayList<>();
    }

    public abstract String getShortName();

}
