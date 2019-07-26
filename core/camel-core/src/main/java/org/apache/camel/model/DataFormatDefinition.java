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
package org.apache.camel.model;

import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.camel.model.IdentifiedType;
import org.apache.camel.model.OtherAttributesAware;
import org.apache.camel.model.dataformat.CustomDataFormat;
import org.apache.camel.model.dataformat.XStreamDataFormat;

public class DataFormatDefinition<Type extends DataFormatDefinition<Type>> extends IdentifiedType implements OtherAttributesAware {

    private Map<QName, Object> otherAttributes;

    @Override
    public String getShortName() {
        return "dataFormat";
    }

    public String getDataFormatName() {
        return getShortName();
    }

    @Override
    public Map<QName, Object> getOtherAttributes() {
        return otherAttributes;
    }

    @Override
    public void setOtherAttributes(Map<QName, Object> otherAttributes) {
        this.otherAttributes = otherAttributes;
    }

    /**
     * Whether the data format should set the Content-Type header with the type
     * from the data format if the data format is capable of doing so. For
     * example application/xml for data formats marshalling to XML, or
     * application/json for data formats marshalling to JSon etc.
     * This property is of type <code>boolean</code>.
     */
    public Type contentTypeHeader(boolean contentTypeHeader) {
        doSetProperty("contentTypeHeader", contentTypeHeader);
        return (Type) this;
    }

    /**
     * Whether the data format should set the Content-Type header with the type
     * from the data format if the data format is capable of doing so. For
     * example application/xml for data formats marshalling to XML, or
     * application/json for data formats marshalling to JSon etc.
     * This property is of type <code>boolean</code>.
     */
    public Type contentTypeHeader(String contentTypeHeader) {
        doSetProperty("contentTypeHeader", contentTypeHeader);
        return (Type) this;
    }

    /**
     * Whether the data format should set the Content-Type header with the type
     * from the data format if the data format is capable of doing so. For
     * example application/xml for data formats marshalling to XML, or
     * application/json for data formats marshalling to JSon etc.
     * This property is of type <code>boolean</code>.
     */
    public void setContentTypeHeader(boolean contentTypeHeader) {
        doSetProperty("contentTypeHeader", contentTypeHeader);
    }

    /**
     * Whether the data format should set the Content-Type header with the type
     * from the data format if the data format is capable of doing so. For
     * example application/xml for data formats marshalling to XML, or
     * application/json for data formats marshalling to JSon etc.
     * This property is of type <code>boolean</code>.
     */
    public Object getContentTypeHeader() {
        return doGetProperty("contentTypeHeader");
    }

}
