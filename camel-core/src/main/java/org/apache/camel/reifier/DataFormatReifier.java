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
package org.apache.camel.reifier;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.camel.CamelContext;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.ProcessorDefinitionHelper;
import org.apache.camel.model.dataformat.ASN1DataFormat;
import org.apache.camel.model.dataformat.AvroDataFormat;
import org.apache.camel.model.dataformat.BarcodeDataFormat;
import org.apache.camel.model.dataformat.Base64DataFormat;
import org.apache.camel.model.dataformat.BeanioDataFormat;
import org.apache.camel.model.dataformat.BindyDataFormat;
import org.apache.camel.model.dataformat.BoonDataFormat;
import org.apache.camel.model.dataformat.CryptoDataFormat;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.apache.camel.model.dataformat.CustomDataFormat;
import org.apache.camel.model.dataformat.FhirJsonDataFormat;
import org.apache.camel.model.dataformat.FhirXmlDataFormat;
import org.apache.camel.model.dataformat.FlatpackDataFormat;
import org.apache.camel.model.dataformat.GzipDataFormat;
import org.apache.camel.model.dataformat.HL7DataFormat;
import org.apache.camel.model.dataformat.IcalDataFormat;
import org.apache.camel.model.dataformat.JacksonXMLDataFormat;
import org.apache.camel.model.dataformat.JaxbDataFormat;
import org.apache.camel.model.dataformat.JibxDataFormat;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.LZFDataFormat;
import org.apache.camel.model.dataformat.MimeMultipartDataFormat;
import org.apache.camel.model.dataformat.PGPDataFormat;
import org.apache.camel.model.dataformat.ProtobufDataFormat;
import org.apache.camel.model.dataformat.RssDataFormat;
import org.apache.camel.model.dataformat.SerializationDataFormat;
import org.apache.camel.model.dataformat.SoapJaxbDataFormat;
import org.apache.camel.model.dataformat.StringDataFormat;
import org.apache.camel.model.dataformat.SyslogDataFormat;
import org.apache.camel.model.dataformat.TarFileDataFormat;
import org.apache.camel.model.dataformat.ThriftDataFormat;
import org.apache.camel.model.dataformat.TidyMarkupDataFormat;
import org.apache.camel.model.dataformat.UniVocityCsvDataFormat;
import org.apache.camel.model.dataformat.UniVocityFixedWidthDataFormat;
import org.apache.camel.model.dataformat.UniVocityTsvDataFormat;
import org.apache.camel.model.dataformat.XMLSecurityDataFormat;
import org.apache.camel.model.dataformat.XStreamDataFormat;
import org.apache.camel.model.dataformat.XmlRpcDataFormat;
import org.apache.camel.model.dataformat.YAMLDataFormat;
import org.apache.camel.model.dataformat.ZipDataFormat;
import org.apache.camel.model.dataformat.ZipFileDataFormat;
import org.apache.camel.reifier.dataformat.ASN1DataFormatReifier;
import org.apache.camel.reifier.dataformat.AvroDataFormatReifier;
import org.apache.camel.reifier.dataformat.BarcodeDataFormatReifier;
import org.apache.camel.reifier.dataformat.Base64DataFormatReifier;
import org.apache.camel.reifier.dataformat.BeanioDataFormatReifier;
import org.apache.camel.reifier.dataformat.BindyDataFormatReifier;
import org.apache.camel.reifier.dataformat.BoonDataFormatReifier;
import org.apache.camel.reifier.dataformat.CryptoDataFormatReifier;
import org.apache.camel.reifier.dataformat.CsvDataFormatReifier;
import org.apache.camel.reifier.dataformat.CustomDataFormatReifier;
import org.apache.camel.reifier.dataformat.FhirJsonDataFormatReifier;
import org.apache.camel.reifier.dataformat.FhirXmlDataFormatReifier;
import org.apache.camel.reifier.dataformat.FlatpackDataFormatReifier;
import org.apache.camel.reifier.dataformat.GzipDataFormatReifier;
import org.apache.camel.reifier.dataformat.HL7DataFormatReifier;
import org.apache.camel.reifier.dataformat.IcalDataFormatReifier;
import org.apache.camel.reifier.dataformat.JacksonXMLDataFormatReifier;
import org.apache.camel.reifier.dataformat.JaxbDataFormatReifier;
import org.apache.camel.reifier.dataformat.JibxDataFormatReifier;
import org.apache.camel.reifier.dataformat.JsonDataFormatReifier;
import org.apache.camel.reifier.dataformat.LZFDataFormatReifier;
import org.apache.camel.reifier.dataformat.MimeMultipartDataFormatReifier;
import org.apache.camel.reifier.dataformat.PGPDataFormatReifier;
import org.apache.camel.reifier.dataformat.ProtobufDataFormatReifier;
import org.apache.camel.reifier.dataformat.RssDataFormatReifier;
import org.apache.camel.reifier.dataformat.SerializationDataFormatReifier;
import org.apache.camel.reifier.dataformat.SoapJaxbDataFormatReifier;
import org.apache.camel.reifier.dataformat.StringDataFormatReifier;
import org.apache.camel.reifier.dataformat.SyslogDataFormatReifier;
import org.apache.camel.reifier.dataformat.TarFileDataFormatReifier;
import org.apache.camel.reifier.dataformat.ThriftDataFormatReifier;
import org.apache.camel.reifier.dataformat.TidyMarkupDataFormatReifier;
import org.apache.camel.reifier.dataformat.UniVocityCsvDataFormatReifier;
import org.apache.camel.reifier.dataformat.UniVocityFixedWidthDataFormatReifier;
import org.apache.camel.reifier.dataformat.UniVocityTsvDataFormatReifier;
import org.apache.camel.reifier.dataformat.XMLSecurityDataFormatReifier;
import org.apache.camel.reifier.dataformat.XStreamDataFormatReifier;
import org.apache.camel.reifier.dataformat.XmlRpcDataFormatReifier;
import org.apache.camel.reifier.dataformat.YAMLDataFormatReifier;
import org.apache.camel.reifier.dataformat.ZipDataFormatReifier;
import org.apache.camel.reifier.dataformat.ZipFileDataFormatReifier;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.support.IntrospectionSupport;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.support.EndpointHelper.isReferenceParameter;

public class DataFormatReifier<T extends DataFormatDefinition> {

    private static final Map<Class<?>, Function<DataFormatDefinition, DataFormatReifier<? extends DataFormatDefinition>>> DATAFORMATS;
    static {
        Map<Class<?>, Function<DataFormatDefinition, DataFormatReifier<? extends DataFormatDefinition>>> map = new ConcurrentHashMap<>();
        map.put(ASN1DataFormat.class, ASN1DataFormatReifier::new);
        map.put(AvroDataFormat.class, AvroDataFormatReifier::new);
        map.put(BarcodeDataFormat.class, BarcodeDataFormatReifier::new);
        map.put(Base64DataFormat.class, Base64DataFormatReifier::new);
        map.put(BeanioDataFormat.class, BeanioDataFormatReifier::new);
        map.put(BindyDataFormat.class, BindyDataFormatReifier::new);
        map.put(BoonDataFormat.class, BoonDataFormatReifier::new);
        map.put(CryptoDataFormat.class, CryptoDataFormatReifier::new);
        map.put(CsvDataFormat.class, CsvDataFormatReifier::new);
        map.put(CustomDataFormat.class, CustomDataFormatReifier::new);
        map.put(FhirJsonDataFormat.class, FhirJsonDataFormatReifier::new);
        map.put(FhirXmlDataFormat.class, FhirXmlDataFormatReifier::new);
        map.put(FlatpackDataFormat.class, FlatpackDataFormatReifier::new);
        map.put(GzipDataFormat.class, GzipDataFormatReifier::new);
        map.put(HL7DataFormat.class, HL7DataFormatReifier::new);
        map.put(IcalDataFormat.class, IcalDataFormatReifier::new);
        map.put(JacksonXMLDataFormat.class, JacksonXMLDataFormatReifier::new);
        map.put(JaxbDataFormat.class, JaxbDataFormatReifier::new);
        map.put(JibxDataFormat.class, JibxDataFormatReifier::new);
        map.put(JsonDataFormat.class, JsonDataFormatReifier::new);
        map.put(LZFDataFormat.class, LZFDataFormatReifier::new);
        map.put(MimeMultipartDataFormat.class, MimeMultipartDataFormatReifier::new);
        map.put(PGPDataFormat.class, PGPDataFormatReifier::new);
        map.put(ProtobufDataFormat.class, ProtobufDataFormatReifier::new);
        map.put(RssDataFormat.class, RssDataFormatReifier::new);
        map.put(SerializationDataFormat.class, SerializationDataFormatReifier::new);
        map.put(SoapJaxbDataFormat.class, SoapJaxbDataFormatReifier::new);
        map.put(StringDataFormat.class, StringDataFormatReifier::new);
        map.put(SyslogDataFormat.class, SyslogDataFormatReifier::new);
        map.put(TarFileDataFormat.class, TarFileDataFormatReifier::new);
        map.put(ThriftDataFormat.class, ThriftDataFormatReifier::new);
        map.put(TidyMarkupDataFormat.class, TidyMarkupDataFormatReifier::new);
        map.put(UniVocityCsvDataFormat.class, UniVocityCsvDataFormatReifier::new);
        map.put(UniVocityFixedWidthDataFormat.class, UniVocityFixedWidthDataFormatReifier::new);
        map.put(UniVocityTsvDataFormat.class, UniVocityTsvDataFormatReifier::new);
        map.put(XmlRpcDataFormat.class, XmlRpcDataFormatReifier::new);
        map.put(XMLSecurityDataFormat.class, XMLSecurityDataFormatReifier::new);
        map.put(XStreamDataFormat.class, XStreamDataFormatReifier::new);
        map.put(YAMLDataFormat.class, YAMLDataFormatReifier::new);
        map.put(ZipDataFormat.class, ZipDataFormatReifier::new);
        map.put(ZipFileDataFormat.class, ZipFileDataFormatReifier::new);
        DATAFORMATS = map;
    }

    public static void register(Class<?> dataformat, Function<DataFormatDefinition, DataFormatReifier<? extends DataFormatDefinition>> reifier) {
        DATAFORMATS.put(dataformat, reifier);
    }

    public static void unregister(Class<?> dataformat) {
        DATAFORMATS.remove(dataformat);
    }

    /**
     * Factory method to create the data format
     *
     * @param routeContext route context
     * @param type         the data format type
     * @param ref          reference to lookup for a data format
     * @return the data format or null if not possible to create
     */
    public static DataFormat getDataFormat(RouteContext routeContext, DataFormatDefinition type, String ref) {
        if (type == null) {
            ObjectHelper.notNull(ref, "ref or type");

            // try to let resolver see if it can resolve it, its not always possible
            type = routeContext.getCamelContext().adapt(ModelCamelContext.class).resolveDataFormatDefinition(ref);

            if (type != null) {
                return reifier(type).getDataFormat(routeContext);
            }

            DataFormat dataFormat = routeContext.getCamelContext().resolveDataFormat(ref);
            if (dataFormat == null) {
                throw new IllegalArgumentException("Cannot find data format in registry with ref: " + ref);
            }

            return dataFormat;
        } else {
            return reifier(type).getDataFormat(routeContext);
        }
    }

    public static DataFormatReifier<? extends DataFormatDefinition> reifier(DataFormatDefinition definition) {
        Function<DataFormatDefinition, DataFormatReifier<? extends DataFormatDefinition>> reifier = DATAFORMATS.get(definition.getClass());
        if (reifier == null) {
            reifier = DataFormatReifier::new;
        }
        return reifier.apply(definition);
    }

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final T definition;

    public DataFormatReifier(T definition) {
        this.definition = definition;
    }

    public DataFormat getDataFormat(RouteContext routeContext) {
        DataFormat dataFormat = definition.getDataFormat();
        if (dataFormat == null) {
            Runnable propertyPlaceholdersChangeReverter = ProcessorDefinitionHelper.createPropertyPlaceholdersChangeReverter();

            // resolve properties before we create the data format
            try {
                ProcessorDefinitionHelper.resolvePropertyPlaceholders(routeContext.getCamelContext(), this);
            } catch (Exception e) {
                throw new IllegalArgumentException("Error resolving property placeholders on data format: " + this, e);
            }
            try {
                dataFormat = createDataFormat(routeContext);
                if (dataFormat != null) {
                    // is enabled by default so assume true if null
                    final boolean contentTypeHeader = definition.getContentTypeHeader() == null || definition.getContentTypeHeader();
                    try {
                        setProperty(routeContext.getCamelContext(), dataFormat, "contentTypeHeader", contentTypeHeader);
                    } catch (Exception e) {
                        // ignore as this option is optional and not all data formats support this
                    }
                    // configure the rest of the options
                    configureDataFormat(dataFormat, routeContext.getCamelContext());
                } else {
                    String dataFormatName = definition.getDataFormatName();
                    throw new IllegalArgumentException(
                            "Data format '" + (dataFormatName != null ? dataFormatName : "<null>") + "' could not be created. "
                                    + "Ensure that the data format is valid and the associated Camel component is present on the classpath");
                }
            } finally {
                propertyPlaceholdersChangeReverter.run();
            }
        }
        return dataFormat;
    }

    /**
     * Allows derived classes to customize the data format
     */
    protected void configureDataFormat(DataFormat dataFormat, CamelContext camelContext) {
    }

    /**
     * Sets a named property on the data format instance using introspection
     */
    protected void setProperty(CamelContext camelContext, Object bean, String name, Object value) {
        try {
            String ref = value instanceof String ? value.toString() : null;
            if (isReferenceParameter(ref) && camelContext != null) {
                IntrospectionSupport.setProperty(camelContext, camelContext.getTypeConverter(), bean, name, null, ref, true);
            } else {
                IntrospectionSupport.setProperty(bean, name, value);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to set property: " + name + " on: " + bean + ". Reason: " + e, e);
        }
    }

    /**
     * Factory method to create the data format instance
     */
    protected DataFormat createDataFormat(RouteContext routeContext) {
        // must use getDataFormatName() as we need special logic in json dataformat
        if (definition.getDataFormatName() != null) {
            return routeContext.getCamelContext().createDataFormat(definition.getDataFormatName());
        }
        return null;
    }

}
