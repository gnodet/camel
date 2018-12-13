/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.maven.packaging.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.camel.maven.packaging.StringHelper;

import static org.apache.camel.maven.packaging.StringHelper.cutLastZeroDigit;

public class ComponentModel {

    private final boolean coreOnly;

    private String kind;
    private String scheme;
    private String extendsScheme;
    private String syntax;
    private String alternativeSyntax;
    private String alternativeSchemes;
    private String title;
    private String description;
    private String firstVersion;
    private String label;
    private String verifiers;
    private boolean deprecated;
    private String deprecationNote;
    private boolean consumerOnly;
    private boolean producerOnly;
    private String javaType;
    private String groupId;
    private String artifactId;
    private String version;
    private boolean lenientProperties;
    private boolean async;
    private final List<ComponentOptionModel> componentOptions = new ArrayList<>();
    private final List<EndpointOptionModel> endpointPathOptions = new ArrayList<>();
    private final List<EndpointOptionModel> endpointOptions = new ArrayList<>();

    public ComponentModel(boolean coreOnly) {
        this.coreOnly = coreOnly;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getExtendsScheme() {
        return extendsScheme;
    }

    public void setExtendsScheme(String extendsScheme) {
        this.extendsScheme = extendsScheme;
    }

    public String getSyntax() {
        return syntax;
    }

    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }

    public String getAlternativeSyntax() {
        return alternativeSyntax;
    }

    public void setAlternativeSyntax(String alternativeSyntax) {
        this.alternativeSyntax = alternativeSyntax;
    }

    public String getAlternativeSchemes() {
        return alternativeSchemes;
    }

    public void setAlternativeSchemes(String alternativeSchemes) {
        this.alternativeSchemes = alternativeSchemes;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFirstVersion() {
        return firstVersion;
    }

    public void setFirstVersion(String firstVersion) {
        this.firstVersion = firstVersion;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getVerifiers() {
        return verifiers;
    }

    public void setVerifiers(String verifiers) {
        this.verifiers = verifiers;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getDeprecationNote() {
        return deprecationNote;
    }

    public void setDeprecationNote(String deprecationNote) {
        this.deprecationNote = deprecationNote;
    }

    public boolean isConsumerOnly() {
        return consumerOnly;
    }

    public void setConsumerOnly(Boolean consumerOnly) {
        this.consumerOnly = consumerOnly;
    }

    public boolean isProducerOnly() {
        return producerOnly;
    }

    public void setProducerOnly(boolean producerOnly) {
        this.producerOnly = producerOnly;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isLenientProperties() {
        return lenientProperties;
    }

    public void setLenientProperties(boolean lenientProperties) {
        this.lenientProperties = lenientProperties;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public List<ComponentOptionModel> getComponentOptions() {
        return componentOptions;
    }

    public void addComponentOption(ComponentOptionModel option) {
        if (componentOptions.stream().noneMatch(o -> Objects.equals(o.getName(), option.getName()))) {
            componentOptions.add(option);
        }
    }

    public List<EndpointOptionModel> getEndpointOptions() {
        return endpointOptions;
    }

    public List<EndpointOptionModel> getEndpointPathOptions() {
        return endpointPathOptions;
    }

    public void addEndpointOption(EndpointOptionModel option) {
        if (endpointOptions.stream().noneMatch(o -> Objects.equals(o.getName(), option.getName()))) {
            endpointOptions.add(option);
        }
    }

    public void addEndpointPathOption(EndpointOptionModel option) {
        if (endpointPathOptions.stream().noneMatch(o -> Objects.equals(o.getName(), option.getName()))) {
            endpointPathOptions.add(option);
        }
    }

    public String getShortJavaType() {
        return StringHelper.getClassShortName(javaType);
    }

    public String getDocLink() {
        // special for these components
        if ("camel-as2".equals(artifactId)) {
            return "camel-as2/camel-as2-component/src/main/docs";
        } else if ("camel-box".equals(artifactId)) {
            return "camel-box/camel-box-component/src/main/docs";
        } else if ("camel-fhir".equals(artifactId)) {
            return "camel-fhir/camel-fhir-component/src/main/docs";
        } else if ("camel-linkedin".equals(artifactId)) {
            return "camel-linkedin/camel-linkedin-component/src/main/docs";
        } else if ("camel-olingo2".equals(artifactId)) {
            return "camel-olingo2/camel-olingo2-component/src/main/docs";
        } else if ("camel-olingo4".equals(artifactId)) {
            return "camel-olingo4/camel-olingo4-component/src/main/docs";
        } else if ("camel-salesforce".equals(artifactId)) {
            return "camel-salesforce/camel-salesforce-component/src/main/docs";
        } else if ("camel-servicenow".equals(artifactId)) {
            return "camel-servicenow/camel-servicenow-component/src/main/docs";
        }

        if ("camel-core".equals(artifactId)) {
            return coreOnly ? "src/main/docs" : "../camel-core/src/main/docs";
        } else {
            return artifactId + "/src/main/docs";
        }
    }

    public String getFirstVersionShort() {
        return cutLastZeroDigit(firstVersion);
    }
}
