package org.apache.camel.model;

import java.util.Map;

public class StructDefinition {

    protected Map<String, Object> properties;

    protected void doSetProperty(String name, Object value) {
        properties.put(name, value);
    }

    protected Object doGetProperty(String name) {
        return properties.get(name);
    }

    protected Map<String, Object> doGetProperties() {
        return properties;
    }
}
