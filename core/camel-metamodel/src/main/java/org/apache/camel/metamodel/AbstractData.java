package org.apache.camel.metamodel;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public abstract class AbstractData {
    String name;
    String display;
    String label;
    String description;
    String maven;
    String since;
    String extend;
    List<Property> properties;
    boolean isAbstract;
    String javaType;
    boolean generate = true;
    String param;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMaven() {
        return maven;
    }

    public void setMaven(String maven) {
        this.maven = maven;
    }

    public String getSince() {
        return since;
    }

    public void setSince(String since) {
        this.since = since;
    }

    public String getExtends() {
        return extend;
    }

    public void setExtends(String extend) {
        this.extend = extend;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean anAbstract) {
        isAbstract = anAbstract;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    public List<Property> getProperties() {
        return properties;
    }

    @JacksonXmlProperty(localName = "property")
    @JacksonXmlElementWrapper(useWrapping = false)
    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public boolean isGenerate() {
        return generate;
    }

    public void setGenerate(boolean generate) {
        this.generate = generate;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }
}
