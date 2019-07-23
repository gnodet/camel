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

import org.apache.camel.Endpoint;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.SendDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.model.endpoints.EndpointProducerBuilder;
import org.apache.camel.processor.SendProcessor;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.support.CamelContextHelper;

public class ToReifier<Type extends ProcessorDefinition<Type>> extends ProcessorReifier<ToDefinition<Type>> {

    @SuppressWarnings("unchecked")
    ToReifier(ProcessorDefinition<?> definition) {
        super((ToDefinition) definition);
    }

    @Override
    public Processor createProcessor(RouteContext routeContext) throws Exception {
        Endpoint endpoint = resolveEndpoint(routeContext, definition.getEndpoint(), definition.getUri());
        ExchangePattern pattern = resolve(routeContext, ExchangePattern.class, definition.getPattern());
        return new SendProcessor(endpoint, pattern);
    }

    public Endpoint resolveEndpoint(RouteContext routeContext, Object endpoint, EndpointProducerBuilder uri) {
        if (endpoint instanceof Endpoint) {
            return (Endpoint) endpoint;
        }
        if (endpoint instanceof String) {
            Endpoint e = resolve(routeContext, Endpoint.class, endpoint);
            if (e == null) {
                throw new IllegalArgumentException("Could not find Endpoint with name " + endpoint);
            }
            return e;
        }
        if (definition.getUri() != null) {
            return definition.getUri().resolve(routeContext.getCamelContext());
        }
        throw new IllegalArgumentException("Endpoint or Uri must be set");
    }

}
