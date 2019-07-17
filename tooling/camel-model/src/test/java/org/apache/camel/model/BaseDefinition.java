package org.apache.camel.model;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseDefinition {

    protected final Map<String, Object> properties = new HashMap<>();

    protected void doSetProperty(String name, Object value) {
        properties.put(name, value);
    }

    protected Object doGetProperty(String name) {
        return properties.get(name);
    }

    protected Map<String, Object> doGetProperties() {
        return properties;
    }

    public abstract String getShortName();

}
