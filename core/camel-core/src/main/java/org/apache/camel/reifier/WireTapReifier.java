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

import java.util.concurrent.ExecutorService;

import org.apache.camel.ExchangePattern;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.builder.EndpointProducerBuilder;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.SetHeaderDefinition;
import org.apache.camel.model.WireTapDefinition;
import org.apache.camel.processor.CamelInternalProcessor;
import org.apache.camel.processor.SendDynamicProcessor;
import org.apache.camel.processor.WireTapProcessor;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.support.CamelContextHelper;
import org.apache.camel.util.StringHelper;

public class WireTapReifier<Type extends ProcessorDefinition<Type>> extends ProcessorReifier<WireTapDefinition<Type>> {

    @SuppressWarnings("unchecked")
    WireTapReifier(ProcessorDefinition<?> definition) {
        super((WireTapDefinition) definition);
    }

    @Override
    public Processor createProcessor(RouteContext routeContext) throws Exception {
        // executor service is mandatory for wire tap
        boolean shutdownThreadPool = willCreateNewThreadPool(routeContext, definition.getExecutorService(), true);
        ExecutorService threadPool = getConfiguredExecutorService(routeContext, "WireTap", definition.getExecutorService(), true);

        // create the send dynamic producer to send to the wire tapped endpoint
        String uri;
        Expression exp;
        boolean dynamicUri = asBoolean(routeContext, definition.getDynamicUri(), true);
        if (definition.getUri() instanceof EndpointProducerBuilder) {
            EndpointProducerBuilder epb = (EndpointProducerBuilder) definition.getUri();
            uri = epb.getUri();
            exp = dynamicUri ? epb.expr() : ExpressionBuilder.constantExpression(uri);
        } else if (definition.getUri() instanceof String) {
            uri = StringHelper.notEmpty((String) definition.getUri(), "uri", this);
            exp = dynamicUri ? ToDynamicReifier.createExpression(routeContext, uri)
                             : ExpressionBuilder.constantExpression(uri);
        } else {
            throw new IllegalArgumentException("Unsupported uri type: " + definition.getUri());
        }

        SendDynamicProcessor processor = new SendDynamicProcessor(uri, exp);
        processor.setCamelContext(routeContext.getCamelContext());
        processor.setPattern(resolve(routeContext, ExchangePattern.class, definition.getPattern()));
        if (definition.getCacheSize() != null) {
            processor.setCacheSize(asInt(routeContext, definition.getCacheSize()));
        }
        processor.setIgnoreInvalidEndpoint(asBoolean(routeContext, definition.getIgnoreInvalidEndpoint(), false));

        // create error handler we need to use for processing the wire tapped
        Processor target = wrapInErrorHandler(routeContext, processor);

        // and wrap in unit of work
        CamelInternalProcessor internal = new CamelInternalProcessor(target);
        internal.addAdvice(new CamelInternalProcessor.UnitOfWorkProcessorAdvice(routeContext));

        // is true by default
        boolean isCopy = asBoolean(routeContext, definition.getCopy(), true);

        WireTapProcessor answer = new WireTapProcessor(processor, internal, ExchangePattern.InOnly, threadPool, shutdownThreadPool, dynamicUri);
        answer.setCopy(isCopy);

        Processor newExchangeProcessor = resolveProcessor(routeContext, definition.getNewExchange());
        if (newExchangeProcessor != null) {
            answer.addNewExchangeProcessor(newExchangeProcessor);
        }
        if (definition.getBody() != null) {
            answer.setNewExchangeExpression(asExpression(routeContext, definition.getBody()));
        }
        if (definition.getHeaders() != null && !definition.getHeaders().isEmpty()) {
            for (SetHeaderDefinition header : definition.getHeaders()) {
                answer.addNewExchangeProcessor(createProcessor(routeContext, header));
            }
        }
        if (definition.getOnPrepare() != null) {
            answer.setOnPrepare(resolveProcessor(routeContext, definition.getOnPrepare()));
        }

        return answer;
    }

}
