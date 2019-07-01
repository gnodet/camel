package model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Model {
    List<Definition> definitions;
    List<Language> languages;
    List<DataFormat> dataFormats;
    List<LoadBalancer> loadBalancers;
    List<Eip> eips;
    List<Verb> verbs;
    List<Endpoint> endpoints;
    List<Processor> processors;

    public List<Definition> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(List<Definition> definitions) {
        this.definitions = definitions;
    }

    public List<Language> getLanguages() {
        return languages;
    }

    public void setLanguages(List<Language> languages) {
        this.languages = languages;
    }

    public List<DataFormat> getDataFormats() {
        return dataFormats;
    }

    public void setDataFormats(List<DataFormat> dataFormats) {
        this.dataFormats = dataFormats;
    }

    public List<LoadBalancer> getLoadBalancers() {
        return loadBalancers;
    }

    public void setLoadBalancers(List<LoadBalancer> loadBalancers) {
        this.loadBalancers = loadBalancers;
    }

    public List<Eip> getEips() {
        return eips;
    }

    public void setEips(List<Eip> eips) {
        this.eips = eips;
    }

    public List<Verb> getVerbs() {
        return verbs;
    }

    public void setVerbs(List<Verb> verbs) {
        this.verbs = verbs;
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public List<Processor> getProcessors() {
        return processors;
    }

    public void setProcessors(List<Processor> processors) {
        this.processors = processors;
    }

    public static abstract class AbstractData {
        String name;
        String display;
        String label;
        String description;
        String maven;
        String since;
        String extend;
        List<Property> properties;
        boolean isAbstract;

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

        public List<Property> getProperties() {
            return properties;
        }

        @JacksonXmlProperty(localName = "property")
        @JacksonXmlElementWrapper(useWrapping = false)
        public void setProperties(List<Property> properties) {
            this.properties = properties;
        }

        public boolean isAbstract() {
            return isAbstract;
        }

        public void setAbstract(boolean anAbstract) {
            isAbstract = anAbstract;
        }
    }

    public static class Property {
        String name;
        String type;
        String display;
        String description;
        String label;
        String kind;
        boolean required;
        boolean secret;
        boolean deprecated;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDisplay() {
            return display;
        }

        public void setDisplay(String display) {
            this.display = display;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public boolean isSecret() {
            return secret;
        }

        public void setSecret(boolean secret) {
            this.secret = secret;
        }

        public boolean isDeprecated() {
            return deprecated;
        }

        public void setDeprecated(boolean deprecated) {
            this.deprecated = deprecated;
        }
    }

    public static class Definition extends AbstractData {

    }
    public static class Language extends AbstractData {

    }
    public static class DataFormat extends AbstractData {

    }
    public static class Eip extends AbstractData {

    }
    public static class Verb extends AbstractData {

    }
    public static class LoadBalancer extends AbstractData {

    }
    public static class Processor extends AbstractData {

    }
    public static class Endpoint extends AbstractData {
        String javaType;
        boolean async;
        boolean consumerOnly;
        boolean producerOnly;
        boolean lenient;

        public String getJavaType() {
            return javaType;
        }

        public void setJavaType(String javaType) {
            this.javaType = javaType;
        }

        public boolean isAsync() {
            return async;
        }

        public void setAsync(boolean async) {
            this.async = async;
        }

        public boolean isConsumerOnly() {
            return consumerOnly;
        }

        public void setConsumerOnly(boolean consumerOnly) {
            this.consumerOnly = consumerOnly;
        }

        public boolean isProducerOnly() {
            return producerOnly;
        }

        public void setProducerOnly(boolean producerOnly) {
            this.producerOnly = producerOnly;
        }

        public boolean isLenient() {
            return lenient;
        }

        public void setLenient(boolean lenient) {
            this.lenient = lenient;
        }
    }
}
