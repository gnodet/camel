package org.apache.camel.reifier.rest;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.model.rest.RestConfigurationDefinition;
import org.apache.camel.model.rest.RestPropertyDefinition;
import org.apache.camel.reifier.AbstractReifier;
import org.apache.camel.spi.RestConfiguration;
import org.apache.camel.support.CamelContextHelper;

public class RestConfigurationReifier extends AbstractReifier<RestConfigurationDefinition> {

    public RestConfigurationReifier(RestConfigurationDefinition definition) {
        super(definition);
    }

    /**
     * Creates a {@link org.apache.camel.spi.RestConfiguration} instance based on the definition
     *
     * @param context     the camel context
     * @return the configuration
     * @throws Exception is thrown if error creating the configuration
     */
    public RestConfiguration asRestConfiguration(CamelContext context) throws Exception {
        RestConfiguration answer = new RestConfiguration();
        if (definition.getComponent() != null) {
            answer.setComponent(CamelContextHelper.parseText(context, definition.getComponent()));
        }
        if (definition.getApiComponent() != null) {
            answer.setApiComponent(CamelContextHelper.parseText(context, definition.getApiComponent()));
        }
        if (definition.getProducerComponent() != null) {
            answer.setProducerComponent(CamelContextHelper.parseText(context, definition.getProducerComponent()));
        }
        if (definition.getScheme() != null) {
            answer.setScheme(CamelContextHelper.parseText(context, definition.getScheme()));
        }
        if (definition.getHost() != null) {
            answer.setHost(CamelContextHelper.parseText(context, definition.getHost()));
        }
        if (definition.getApiHost() != null) {
            answer.setApiHost(CamelContextHelper.parseText(context, definition.getApiHost()));
        }
        if (definition.getPort() != null) {
            answer.setPort(AbstractReifier.asInt(context, definition.getPort()));
        }
        if (definition.getProducerApiDoc() != null) {
            answer.setProducerApiDoc(CamelContextHelper.parseText(context, definition.getProducerApiDoc()));
        }
        if (definition.getApiContextPath() != null) {
            answer.setApiContextPath(CamelContextHelper.parseText(context, definition.getApiContextPath()));
        }
        if (definition.getApiContextRouteId() != null) {
            answer.setApiContextRouteId(CamelContextHelper.parseText(context, definition.getApiContextRouteId()));
        }
        if (definition.getApiContextIdPattern() != null) {
            // special to allow #name# to refer to itself
            if ("#name#".equals(definition.getApiComponent())) {
                answer.setApiContextIdPattern(context.getName());
            } else {
                answer.setApiContextIdPattern(CamelContextHelper.parseText(context, definition.getApiContextIdPattern()));
            }
        }
        if (definition.getApiContextListing() != null) {
            answer.setApiContextListing(asBoolean(context, definition.getApiContextListing()));
        }
        if (definition.getApiVendorExtension() != null) {
            answer.setApiVendorExtension(asBoolean(context, definition.getApiVendorExtension()));
        }
        if (definition.getContextPath() != null) {
            answer.setContextPath(CamelContextHelper.parseText(context, definition.getContextPath()));
        }
        if (definition.getHostNameResolver() != null) {
            answer.setHostNameResolver(asString(context, definition.getHostNameResolver()));
        }
        if (definition.getBindingMode() != null) {
            answer.setBindingMode(asString(context, definition.getBindingMode()));
        }
        if (definition.getSkipBindingOnErrorCode() != null) {
            answer.setSkipBindingOnErrorCode(asBoolean(context, definition.getSkipBindingOnErrorCode()));
        }
        if (definition.getEnableCORS() != null) {
            answer.setEnableCORS(asBoolean(context, definition.getEnableCORS()));
        }
        if (definition.getJsonDataFormat() != null) {
            answer.setJsonDataFormat(definition.getJsonDataFormat());
        }
        if (definition.getXmlDataFormat() != null) {
            answer.setXmlDataFormat(definition.getXmlDataFormat());
        }
        if (!definition.getComponentProperties().isEmpty()) {
            Map<String, Object> props = new HashMap<String, Object>();
            for (RestPropertyDefinition prop : definition.getComponentProperties()) {
                String key = prop.getKey();
                String value = CamelContextHelper.parseText(context, prop.getValue());
                props.put(key, value);
            }
            answer.setComponentProperties(props);
        }
        if (!definition.getEndpointProperties().isEmpty()) {
            Map<String, Object> props = new HashMap<String, Object>();
            for (RestPropertyDefinition prop : definition.getEndpointProperties()) {
                String key = prop.getKey();
                String value = CamelContextHelper.parseText(context, prop.getValue());
                props.put(key, value);
            }
            answer.setEndpointProperties(props);
        }
        if (!definition.getConsumerProperties().isEmpty()) {
            Map<String, Object> props = new HashMap<String, Object>();
            for (RestPropertyDefinition prop : definition.getConsumerProperties()) {
                String key = prop.getKey();
                String value = CamelContextHelper.parseText(context, prop.getValue());
                props.put(key, value);
            }
            answer.setConsumerProperties(props);
        }
        if (!definition.getDataFormatProperties().isEmpty()) {
            Map<String, Object> props = new HashMap<String, Object>();
            for (RestPropertyDefinition prop : definition.getDataFormatProperties()) {
                String key = prop.getKey();
                String value = CamelContextHelper.parseText(context, prop.getValue());
                props.put(key, value);
            }
            answer.setDataFormatProperties(props);
        }
        if (!definition.getApiProperties().isEmpty()) {
            Map<String, Object> props = new HashMap<String, Object>();
            for (RestPropertyDefinition prop : definition.getApiProperties()) {
                String key = prop.getKey();
                String value = CamelContextHelper.parseText(context, prop.getValue());
                props.put(key, value);
            }
            answer.setApiProperties(props);
        }
        if (!definition.getCorsHeaders().isEmpty()) {
            Map<String, String> props = new HashMap<String, String>();
            for (RestPropertyDefinition prop : definition.getCorsHeaders()) {
                String key = prop.getKey();
                String value = CamelContextHelper.parseText(context, prop.getValue());
                props.put(key, value);
            }
            answer.setCorsHeaders(props);
        }
        return answer;
    }

}
