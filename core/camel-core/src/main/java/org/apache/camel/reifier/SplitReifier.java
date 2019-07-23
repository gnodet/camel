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
import java.util.function.Supplier;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ProcessorDefinitionHelper;
import org.apache.camel.model.SplitDefinition;
import org.apache.camel.processor.Splitter;
import org.apache.camel.processor.aggregate.AggregationStrategyBeanAdapter;
import org.apache.camel.processor.aggregate.ShareUnitOfWorkAggregationStrategy;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.support.CamelContextHelper;

public class SplitReifier<Type extends ProcessorDefinition<Type>> extends ExpressionReifier<SplitDefinition<Type>> {

    @SuppressWarnings("unchecked")
    SplitReifier(ProcessorDefinition<?> definition) {
        super((SplitDefinition) definition);
    }

    @Override
    public Processor createProcessor(RouteContext routeContext) throws Exception {
        Processor childProcessor = this.createChildProcessor(routeContext, true);

        boolean isParallelProcessing = definition.getParallelProcessing() != null && asBoolean(routeContext, definition.getParallelProcessing());
        boolean isStreaming = definition.getStreaming() != null && asBoolean(routeContext, definition.getStreaming());
        boolean isShareUnitOfWork = definition.getShareUnitOfWork() != null && asBoolean(routeContext, definition.getShareUnitOfWork());
        boolean isParallelAggregate = definition.getParallelAggregate() != null && asBoolean(routeContext, definition.getParallelAggregate());
        boolean isStopOnException = definition.getStopOnException() != null && asBoolean(routeContext, definition.getStopOnException());
        boolean isStopOnAggregateException = definition.getStopOnAggregateException() != null && asBoolean(routeContext, definition.getStopOnAggregateException());
        boolean shutdownThreadPool = willCreateNewThreadPool(routeContext, definition.getExecutorService(), isParallelProcessing);
        ExecutorService threadPool = getConfiguredExecutorService(routeContext, "Split", definition.getExecutorService(), isParallelProcessing);

        long timeout = definition.getTimeout() != null ? asLong(routeContext, definition.getTimeout()) : 0;
        if (timeout > 0 && !isParallelProcessing) {
            throw new IllegalArgumentException("Timeout is used but ParallelProcessing has not been enabled.");
        }
        Processor onPrepare = resolveProcessor(routeContext, definition.getOnPrepare());

        Expression exp = definition.getExpression().createExpression(routeContext);

        AggregationStrategy strategy = resolveAggregationStrategy(routeContext, definition.getAggregationStrategy(),
                definition.getStrategyMethodName(), definition.getStrategyMethodAllowNull(), () -> null);

        Splitter answer = new Splitter(routeContext.getCamelContext(), exp, childProcessor, strategy,
                isParallelProcessing, threadPool, shutdownThreadPool, isStreaming, isStopOnException,
                timeout, onPrepare, isShareUnitOfWork, isParallelAggregate, isStopOnAggregateException);
        return answer;
    }

    @Override
    protected AggregationStrategy resolveAggregationStrategy(RouteContext routeContext, Object value, Object method, Object allowNull, Supplier<AggregationStrategy> defaultStrategy) {
        AggregationStrategy strategy = super.resolveAggregationStrategy(routeContext, value, method, allowNull, defaultStrategy);
        if (strategy != null && definition.getShareUnitOfWork() != null && asBoolean(routeContext, definition.getShareUnitOfWork())) {
            // wrap strategy in share unit of work
            strategy = new ShareUnitOfWorkAggregationStrategy(strategy);
        }
        return strategy;
    }

}
