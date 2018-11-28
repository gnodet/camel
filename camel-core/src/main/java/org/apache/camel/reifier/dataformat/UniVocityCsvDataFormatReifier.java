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
import org.apache.camel.model.dataformat.UniVocityCsvDataFormat;
import org.apache.camel.spi.DataFormat;

/**
 * The uniVocity CSV data format is used for working with CSV (Comma Separated Values) flat payloads.
 */
public class UniVocityCsvDataFormatReifier extends UniVocityAbstractDataFormatReifier<UniVocityCsvDataFormat> {

    public UniVocityCsvDataFormatReifier(DataFormatDefinition definition) {
        super(UniVocityCsvDataFormat.class.cast(definition));
    }

    @Override
    protected void configureDataFormat(DataFormat dataFormat, CamelContext camelContext) {
        super.configureDataFormat(dataFormat, camelContext);

        if (definition.getQuoteAllFields() != null) {
            setProperty(camelContext, dataFormat, "quoteAllFields", definition.getQuoteAllFields());
        }
        if (definition.getQuote() != null) {
            setProperty(camelContext, dataFormat, "quote", singleCharOf("quote", definition.getQuote()));
        }
        if (definition.getQuoteEscape() != null) {
            setProperty(camelContext, dataFormat, "quoteEscape", singleCharOf("quoteEscape", definition.getQuoteEscape()));
        }
        if (definition.getDelimiter() != null) {
            setProperty(camelContext, dataFormat, "delimiter", singleCharOf("delimiter", definition.getDelimiter()));
        }
    }
}
