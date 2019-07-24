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
package org.apache.camel.reifier.transformer;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.transformer.DataFormatTransformer;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.transformer.DataFormatTransformerDefinition;
import org.apache.camel.model.transformer.TransformerDefinition;
import org.apache.camel.reifier.dataformat.DataFormatReifier;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.DataType;
import org.apache.camel.spi.Transformer;

public class DataFormatTransformeReifier extends TransformerReifier<DataFormatTransformerDefinition> {

    DataFormatTransformeReifier(TransformerDefinition definition) {
        super((DataFormatTransformerDefinition) definition);
    }

    @Override
    protected Transformer doCreateTransformer(CamelContext context) {
        DataFormat dataFormat = DataFormatReifier.getDataFormat(context,
                as(DataFormatDefinition.class, definition.getDataFormat()),
                as(String.class, definition.getDataFormat()));
        return new DataFormatTransformer(context)
                .setDataFormat(dataFormat)
                .setModel(definition.getScheme())
                .setFrom(resolve(context, DataType.class, definition.getFromType()))
                .setTo(resolve(context, DataType.class, definition.getToType()));
    }

    private static <T> T as(Class<T> clazz, Object v) {
        return clazz.isInstance(v) ? clazz.cast(v) : null;
    }

}
