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
package org.apache.camel.reifier;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import org.apache.camel.Processor;
import org.apache.camel.model.ConvertBodyDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.processor.ConvertBodyProcessor;
import org.apache.camel.spi.RouteContext;

public class ConvertBodyReifier<Type extends ProcessorDefinition<Type>> extends ProcessorReifier<ConvertBodyDefinition<Type>> {

    @SuppressWarnings("unchecked")
    ConvertBodyReifier(ProcessorDefinition<?> definition) {
        super((ConvertBodyDefinition) definition);
    }

    @Override
    public Processor createProcessor(RouteContext routeContext) throws Exception {
        Class<?> type = resolve(routeContext, Class.class, definition.getType());

        // validate charset
        String charset = null;
        if (definition.getCharset() != null) {
            charset = resolve(routeContext, Charset.class, definition.getCharset()).toString();
            validateCharset(charset);
        }

        return new ConvertBodyProcessor(type, charset);
    }

    public static void validateCharset(String charset) throws UnsupportedCharsetException {
        if (charset != null) {
            if (Charset.isSupported(charset)) {
                Charset.forName(charset);
                return;
            }
        }
        throw new UnsupportedCharsetException(charset);
    }

}
