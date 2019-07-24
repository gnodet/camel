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
import org.apache.camel.model.transformer.CustomTransformerDefinition;
import org.apache.camel.model.transformer.TransformerDefinition;
import org.apache.camel.spi.DataType;
import org.apache.camel.spi.Transformer;

public class CustomTransformeReifier extends TransformerReifier<CustomTransformerDefinition> {

    CustomTransformeReifier(TransformerDefinition definition) {
        super((CustomTransformerDefinition) definition);
    }

    @Override
    protected Transformer doCreateTransformer(CamelContext context) throws Exception {
        if (definition.getTransformer() == null) {
            throw new IllegalArgumentException("'transformer' must be specified for customTransformer");
        }
        Transformer transformer;
        Object value = definition.getTransformer();
        if (value instanceof Transformer) {
            transformer = (Transformer) value;
        } else if (value instanceof Class) {
            Class<?> transformerClass = (Class) value;
            if (!Transformer.class.isAssignableFrom(transformerClass)) {
                throw new IllegalArgumentException("Illegal transformer class: " + value);
            }
            transformer = context.getInjector().newInstance((Class<Transformer>) transformerClass, false);
        } else if (value instanceof String) {
            String str = (String) value;
            if (str.startsWith("#bean:")) {
                transformer = resolve(context, Transformer.class, str);
            } else if (str.startsWith("#class:")) {
                String name = str.substring("#class:".length());
                Class<?> transformerClass = context.getClassResolver().resolveMandatoryClass(name, Transformer.class);
                if (!Transformer.class.isAssignableFrom(transformerClass)) {
                    throw new IllegalArgumentException("Illegal transformer class: " + value);
                }
                transformer = context.getInjector().newInstance((Class<Transformer>) transformerClass, false);
            } else {
                throw new IllegalArgumentException("Unsupported value: " + value);
            }
        } else {
            throw new IllegalArgumentException("Unsupported value: " + value);
        }
        if (transformer.getModel() != null || transformer.getFrom() != null || transformer.getTo() != null) {
            throw new IllegalArgumentException(String.format("Transformer '%s' is already in use. " +
                            "Please check if duplicate transformer exists.", value));
        }
        transformer.setCamelContext(context);
        return transformer.setModel(definition.getScheme())
                .setFrom(resolve(context, DataType.class, definition.getFromType()))
                .setTo(resolve(context, DataType.class, definition.getToType()));
    }

}
