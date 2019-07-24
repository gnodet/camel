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

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.model.EnrichDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.processor.Enricher;
import org.apache.camel.spi.RouteContext;

public class EnrichReifier<Type extends ProcessorDefinition<Type>> extends ExpressionReifier<EnrichDefinition<Type>> {

    @SuppressWarnings("unchecked")
    EnrichReifier(ProcessorDefinition<?> definition) {
        super((EnrichDefinition) definition);
    }

    @Override
    public Processor createProcessor(RouteContext routeContext) throws Exception {

        Expression exp = definition.getExpression().createExpression(routeContext);
        boolean isShareUnitOfWork = definition.getShareUnitOfWork() != null && asBoolean(routeContext, definition.getShareUnitOfWork());
        boolean isIgnoreInvalidEndpoint = definition.getIgnoreInvalidEndpoint() != null && asBoolean(routeContext, definition.getIgnoreInvalidEndpoint());

        Enricher enricher = new Enricher(exp);
        enricher.setShareUnitOfWork(isShareUnitOfWork);
        enricher.setIgnoreInvalidEndpoint(isIgnoreInvalidEndpoint);
        AggregationStrategy strategy = createAggregationStrategy(routeContext);
        if (strategy != null) {
            enricher.setAggregationStrategy(strategy);
        }
        if (definition.getAggregateOnException() != null) {
            enricher.setAggregateOnException(asBoolean(routeContext, definition.getAggregateOnException()));
        }
        return enricher;
    }

    private AggregationStrategy createAggregationStrategy(RouteContext routeContext) {
        return resolveAggregationStrategy(routeContext, definition.getAggregationStrategy(),
                definition.getStrategyMethodName(), definition.getStrategyMethodAllowNull(), () -> null);
    }

}
