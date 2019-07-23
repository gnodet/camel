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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Processor;
import org.apache.camel.model.MulticastDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ProcessorDefinitionHelper;
import org.apache.camel.processor.MulticastProcessor;
import org.apache.camel.processor.aggregate.AggregationStrategyBeanAdapter;
import org.apache.camel.processor.aggregate.ShareUnitOfWorkAggregationStrategy;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;
import org.apache.camel.spi.AggregationRepository;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.support.CamelContextHelper;

public class MulticastReifier<Type extends ProcessorDefinition<Type>> extends ProcessorReifier<MulticastDefinition<Type>> {

    @SuppressWarnings("unchecked")
    MulticastReifier(ProcessorDefinition<?> definition) {
        super((MulticastDefinition) definition);
    }

    @Override
    public Processor createProcessor(RouteContext routeContext) throws Exception {
        Processor answer = this.createChildProcessor(routeContext, true);

        // force the answer as a multicast processor even if there is only one child processor in the multicast
        if (!(answer instanceof MulticastProcessor)) {
            List<Processor> list = new ArrayList<>(1);
            list.add(answer);
            answer = createCompositeProcessor(routeContext, list);
        }
        return answer;
    }

    protected Processor createCompositeProcessor(RouteContext routeContext, List<Processor> list) throws Exception {
        final AggregationStrategy strategy = createAggregationStrategy(routeContext);

        boolean isParallelProcessing = definition.getParallelProcessing() != null && asBoolean(routeContext, definition.getParallelProcessing());
        boolean isShareUnitOfWork = definition.getShareUnitOfWork() != null && asBoolean(routeContext, definition.getShareUnitOfWork());
        boolean isStreaming = definition.getStreaming() != null && asBoolean(routeContext, definition.getStreaming());
        boolean isStopOnException = definition.getStopOnException() != null && asBoolean(routeContext, definition.getStopOnException());
        boolean isParallelAggregate = definition.getParallelAggregate() != null && asBoolean(routeContext, definition.getParallelAggregate());
        boolean isStopOnAggregateException = definition.getStopOnAggregateException() != null && asBoolean(routeContext, definition.getStopOnAggregateException());

        boolean shutdownThreadPool = willCreateNewThreadPool(routeContext, definition.getExecutorService(), isParallelProcessing);
        ExecutorService threadPool = getConfiguredExecutorService(routeContext, "Multicast", definition.getExecutorService(), isParallelProcessing);

        long timeout = definition.getTimeout() != null ? asLong(routeContext, definition.getTimeout()) : 0;
        if (timeout > 0 && !isParallelProcessing) {
            throw new IllegalArgumentException("Timeout is used but ParallelProcessing has not been enabled.");
        }
        Processor onPrepare = resolveProcessor(routeContext, definition.getOnPrepare());

        MulticastProcessor answer = new MulticastProcessor(routeContext.getCamelContext(), list, strategy, isParallelProcessing,
                threadPool, shutdownThreadPool, isStreaming, isStopOnException, timeout, onPrepare, isShareUnitOfWork, isParallelAggregate, isStopOnAggregateException);
        return answer;
    }

    private AggregationStrategy createAggregationStrategy(RouteContext routeContext) {
        AggregationStrategy strategy = resolveAggregationStrategy(routeContext,
                definition.getAggregationStrategy(), definition.getStrategyMethodName(), definition.getStrategyMethodAllowNull(),
                UseLatestAggregationStrategy::new);

        if (definition.getShareUnitOfWork() != null && asBoolean(routeContext, definition.getShareUnitOfWork())) {
            // wrap strategy in share unit of work
            strategy = new ShareUnitOfWorkAggregationStrategy(strategy);
        }

        return strategy;
    }

}
