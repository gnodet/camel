/*
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
package org.apache.camel.reifier.dataformat;

import org.apache.camel.CamelContext;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.JsonDataFormat.JsonLibrary;
import org.apache.camel.spi.DataFormat;

public class JsonDataFormatReifier extends DataFormatReifier<JsonDataFormat> {

    public JsonDataFormatReifier(DataFormatDefinition definition) {
        super((JsonDataFormat) definition);
    }

    @Override
    protected String getDataFormatName(CamelContext camelContext) {
        JsonLibrary library = resolve(camelContext, JsonLibrary.class, definition.getLibrary());
        switch (library) {
            case XStream:
                return "json-xstream";
            case Jackson:
                return "json-jackson";
            case Gson:
                return "json-gson";
            case Fastjson:
                return "json-fastjson";
            default:
                return "json-johnzon";
        }
    }

    @Override
    protected void configureDataFormat(DataFormat dataFormat, CamelContext camelContext) {
        if (definition.getObjectMapper() != null) {
            // must be a reference value
            String ref = definition.getObjectMapper().startsWith("#") ? definition.getObjectMapper() : "#" + definition.getObjectMapper();
            setProperty(camelContext, dataFormat, "objectMapper", ref);
        }
        if (definition.getUseDefaultObjectMapper() != null) {
            setProperty(camelContext, dataFormat, "useDefaultObjectMapper", definition.getUseDefaultObjectMapper());
        }
        if (definition.getUnmarshalType() != null) {
            setProperty(camelContext, dataFormat, "unmarshalType", definition.getUnmarshalType());
        }
        if (definition.getPrettyPrint() != null) {
            setProperty(camelContext, dataFormat, "prettyPrint", definition.getPrettyPrint());
        }
        if (definition.getJsonView() != null) {
            setProperty(camelContext, dataFormat, "jsonView", definition.getJsonView());
        }
        if (definition.getInclude() != null) {
            setProperty(camelContext, dataFormat, "include", definition.getInclude());
        }
        if (definition.getAllowJmsType() != null) {
            setProperty(camelContext, dataFormat, "allowJmsType", definition.getAllowJmsType());
        }
        if (definition.getCollectionTypeName() != null) {
            setProperty(camelContext, dataFormat, "collectionType", definition.getCollectionTypeName());
        }
        if (definition.getUseList() != null) {
            setProperty(camelContext, dataFormat, "useList", definition.getUseList());
        }
        if (definition.getEnableJaxbAnnotationModule() != null) {
            setProperty(camelContext, dataFormat, "enableJaxbAnnotationModule", definition.getEnableJaxbAnnotationModule());
        }
        if (definition.getModuleClassNames() != null) {
            setProperty(camelContext, dataFormat, "moduleClassNames", definition.getModuleClassNames());
        }
        if (definition.getModuleRefs() != null) {
            setProperty(camelContext, dataFormat, "moduleRefs", definition.getModuleRefs());
        }
        if (definition.getEnableFeatures() != null) {
            setProperty(camelContext, dataFormat, "enableFeatures", definition.getEnableFeatures());
        }
        if (definition.getDisableFeatures() != null) {
            setProperty(camelContext, dataFormat, "disableFeatures", definition.getDisableFeatures());
        }
        if (definition.getPermissions() != null) {
            setProperty(camelContext, dataFormat, "permissions", definition.getPermissions());
        }
        if (definition.getAllowUnmarshallType() != null) {
            setProperty(camelContext, dataFormat, "allowUnmarshallType", definition.getAllowUnmarshallType());
        }
        // if we have the unmarshal type, but no permission set, then use it to
        // be allowed
        if (definition.getPermissions() == null && definition.getUnmarshalType() != null) {
            String allow = "+" + definition.getUnmarshalType().toString();
            setProperty(camelContext, dataFormat, "permissions", allow);
        }
    }

}
