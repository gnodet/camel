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
package org.apache.camel.reifier.dataformat;

import org.apache.camel.CamelContext;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.dataformat.XStreamDataFormat;
import org.apache.camel.reifier.DataFormatReifier;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.support.CamelContextHelper;
import org.apache.camel.util.ObjectHelper;

/**
 * XSTream data format is used for unmarshal a XML payload to POJO or to marshal POJO back to XML payload.
 */
public class XStreamDataFormatReifier extends DataFormatReifier<XStreamDataFormat> {

    public XStreamDataFormatReifier(DataFormatDefinition definition) {
        super(XStreamDataFormat.class.cast(definition));
    }

    @Override
    protected DataFormat createDataFormat(RouteContext routeContext) {
        if ("json".equals(definition.getDriver())) {
            setProperty(routeContext.getCamelContext(), this, "dataFormatName", "json-xstream");
        }
        DataFormat answer = super.createDataFormat(routeContext);
        // need to lookup the reference for the xstreamDriver
        if (ObjectHelper.isNotEmpty(definition.getDriverRef())) {
            setProperty(routeContext.getCamelContext(), answer, "xstreamDriver", CamelContextHelper.mandatoryLookup(routeContext.getCamelContext(), definition.getDriverRef()));
        }
        return answer;
    }

    @Override
    protected void configureDataFormat(DataFormat dataFormat, CamelContext camelContext) {
        if (definition.getPermissions() != null) {
            setProperty(camelContext, dataFormat, "permissions", definition.getPermissions());
        }
        if (definition.getEncoding() != null) {
            setProperty(camelContext, dataFormat, "encoding", definition.getEncoding());
        }
        if (definition.getConverters() != null) {
            setProperty(camelContext, dataFormat, "converters", definition.getConverters());
        }
        if (definition.getAliases() != null) {
            setProperty(camelContext, dataFormat, "aliases", definition.getAliases());
        }
        if (definition.getOmitFields() != null) {
            setProperty(camelContext, dataFormat, "omitFields", definition.getOmitFields());
        }
        if (definition.getImplicitCollections() != null) {
            setProperty(camelContext, dataFormat, "implicitCollections", definition.getImplicitCollections());
        }
        if (definition.getMode() != null) {
            setProperty(camelContext, dataFormat, "mode", definition.getMode());
        }
    }
    
}