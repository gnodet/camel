package org.apache.camel.model;

import org.apache.camel.model.dataformats.DataFormatDefinition;

public class ProcessorDefinition<Type> {

    public Type unmarshal(DataFormatDefinition dataFormat) {
        return (Type) this;
    }

    public Type marshal(DataFormatDefinition dataFormat) {
        return (Type) this;
    }
}
