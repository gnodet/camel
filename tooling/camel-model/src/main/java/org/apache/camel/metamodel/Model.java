package org.apache.camel.metamodel;

import java.util.List;

public class Model {

    List<Language> languages;
    List<DataFormat> dataFormats;
    List<LoadBalancer> loadBalancers;
    List<Definition> definitions;
    List<Verb> verbs;
    List<Endpoint> endpoints;
    List<Processor> processors;

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

    public List<Definition> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(List<Definition> definitions) {
        this.definitions = definitions;
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

}
