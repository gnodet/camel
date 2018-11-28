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
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.dataformat.BoonDataFormat;
import org.apache.camel.reifier.DataFormatReifier;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.RouteContext;

/**
 * Boon data format is used for unmarshal a JSon payload to POJO or to marshal POJO back to JSon payload.
 */
public class BoonDataFormatReifier extends DataFormatReifier<BoonDataFormat> {

    public BoonDataFormatReifier(DataFormatDefinition definition) {
        super(BoonDataFormat.class.cast(definition));
    }

    @Override
    protected DataFormat createDataFormat(RouteContext routeContext) {
        if (definition.getUnmarshalType() == null && definition.getUnmarshalTypeName() != null) {
            try {
                definition.setUnmarshalType(routeContext.getCamelContext().getClassResolver().resolveMandatoryClass(definition.getUnmarshalTypeName()));
            } catch (ClassNotFoundException e) {
                throw RuntimeCamelException.wrapRuntimeCamelException(e);
            }
        }
        return super.createDataFormat(routeContext);
    }

    @Override
    protected void configureDataFormat(DataFormat dataFormat, CamelContext camelContext) {
        if (definition.getUnmarshalType() != null) {
            setProperty(camelContext, dataFormat, "unmarshalType", definition.getUnmarshalType());
        }
        if (definition.getUseList() != null) {
            setProperty(camelContext, dataFormat, "useList", definition.getUseList());
        }
    }
}
