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

import org.apache.camel.Processor;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ThrowExceptionDefinition;
import org.apache.camel.processor.ThrowExceptionProcessor;
import org.apache.camel.spi.RouteContext;

public class ThrowExceptionReifier<Type extends ProcessorDefinition<Type>> extends ProcessorReifier<ThrowExceptionDefinition<Type>> {

    @SuppressWarnings("unchecked")
    ThrowExceptionReifier(ProcessorDefinition<?> definition) {
        super((ThrowExceptionDefinition) definition);
    }

    @Override
    public Processor createProcessor(RouteContext routeContext) {
        Exception exception = resolve(routeContext, Exception.class, definition.getException());
        Class<?> type = asClass(routeContext, definition.getExceptionType());
        String message = asString(routeContext, definition.getMessage());

        if (exception == null && type == null) {
            throw new IllegalArgumentException("exception or exceptionType must be configured on: " + this);
        }
        if (type != null && !Exception.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("exceptionType does not inherit " + Exception.class.getName() + " on: " + this);
        }
        return new ThrowExceptionProcessor(exception, (Class<? extends Exception>) type, message);
    }

}
