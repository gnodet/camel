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
package org.apache.camel.component.atomix.client.map.springboot;

import javax.annotation.Generated;
import org.apache.camel.component.atomix.client.map.AtomixMap.Action;
import org.apache.camel.component.atomix.client.map.AtomixMapComponent;
import org.apache.camel.spring.boot.ComponentConfigurationPropertiesCommon;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The atomix-map component is used to access Atomix's distributed map.
 * 
 * Generated by camel-package-maven-plugin - do not edit this file!
 */
@Generated("org.apache.camel.maven.packaging.SpringBootAutoConfigurationMojo")
@ConfigurationProperties(prefix = "camel.component.atomix-map")
public class AtomixMapComponentConfiguration
        extends
            ComponentConfigurationPropertiesCommon {

    /**
     * Whether to enable auto configuration of the atomix-map component. This is
     * enabled by default.
     */
    private Boolean enabled;
    /**
     * The shared component configuration
     */
    private AtomixMapConfigurationNestedConfiguration configuration;
    /**
     * The shared AtomixClient instance. The option is a io.atomix.AtomixClient
     * type.
     */
    private String atomix;
    /**
     * The nodes the AtomixClient should connect to
     */
    private String nodes;
    /**
     * The path to the AtomixClient configuration
     */
    private String configurationUri;
    /**
     * Whether the component should resolve property placeholders on itself when
     * starting. Only properties which are of String type can use property
     * placeholders.
     */
    private Boolean resolvePropertyPlaceholders = true;

    public AtomixMapConfigurationNestedConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(
            AtomixMapConfigurationNestedConfiguration configuration) {
        this.configuration = configuration;
    }

    public String getAtomix() {
        return atomix;
    }

    public void setAtomix(String atomix) {
        this.atomix = atomix;
    }

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

    public String getConfigurationUri() {
        return configurationUri;
    }

    public void setConfigurationUri(String configurationUri) {
        this.configurationUri = configurationUri;
    }

    public Boolean getResolvePropertyPlaceholders() {
        return resolvePropertyPlaceholders;
    }

    public void setResolvePropertyPlaceholders(
            Boolean resolvePropertyPlaceholders) {
        this.resolvePropertyPlaceholders = resolvePropertyPlaceholders;
    }

    public static class AtomixMapConfigurationNestedConfiguration {
        public static final Class CAMEL_NESTED_CLASS = org.apache.camel.component.atomix.client.map.AtomixMapConfiguration.class;
        /**
         * The default action.
         */
        private Action defaultAction = Action.PUT;
        /**
         * The key to use if none is set in the header or to listen for events
         * for a specific key.
         */
        private Object key;
        /**
         * The resource ttl.
         */
        private Long ttl;
        /**
         * The header that wil carry the result.
         */
        private String resultHeader;

        public Action getDefaultAction() {
            return defaultAction;
        }

        public void setDefaultAction(Action defaultAction) {
            this.defaultAction = defaultAction;
        }

        public Object getKey() {
            return key;
        }

        public void setKey(Object key) {
            this.key = key;
        }

        public Long getTtl() {
            return ttl;
        }

        public void setTtl(Long ttl) {
            this.ttl = ttl;
        }

        public String getResultHeader() {
            return resultHeader;
        }

        public void setResultHeader(String resultHeader) {
            this.resultHeader = resultHeader;
        }
    }
}