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

import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.model.OnCompletionDefinition;
import org.apache.camel.model.OnCompletionDefinition.OnCompletionMode;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.processor.CamelInternalProcessor;
import org.apache.camel.processor.OnCompletionProcessor;
import org.apache.camel.spi.RouteContext;

public class OnCompletionReifier<Type extends ProcessorDefinition<Type>> extends ProcessorReifier<OnCompletionDefinition<Type>> {

    @SuppressWarnings("unchecked")
    OnCompletionReifier(ProcessorDefinition<?> definition) {
        super((OnCompletionDefinition) definition);
    }

    @Override
    public Processor createProcessor(RouteContext routeContext) throws Exception {
        boolean isOnCompleteOnly = definition.getOnCompleteOnly() != null && asBoolean(routeContext, definition.getOnCompleteOnly());
        boolean isOnFailureOnly = definition.getOnFailureOnly() != null && asBoolean(routeContext, definition.getOnFailureOnly());
        boolean isParallelProcessing = definition.getParallelProcessing() != null && asBoolean(routeContext, definition.getParallelProcessing());
        boolean original = definition.getUseOriginalMessage() != null && asBoolean(routeContext, definition.getUseOriginalMessage());

        if (isOnCompleteOnly && isOnFailureOnly) {
            throw new IllegalArgumentException("Both onCompleteOnly and onFailureOnly cannot be true. Only one of them can be true. On node: " + this);
        }
        if (original) {
            // ensure allow original is turned on
            routeContext.setAllowUseOriginalMessage(true);
        }

        Processor childProcessor = this.createChildProcessor(routeContext, true);

        // wrap the on completion route in a unit of work processor
        CamelInternalProcessor internal = new CamelInternalProcessor(childProcessor);
        internal.addAdvice(new CamelInternalProcessor.UnitOfWorkProcessorAdvice(routeContext));

        routeContext.setOnCompletion(getId(definition, routeContext), internal);

        Predicate when = null;
        if (definition.getOnWhen() != null) {
            when = definition.getOnWhen().getExpression().createPredicate(routeContext);
        }

        boolean shutdownThreadPool = willCreateNewThreadPool(routeContext, definition.getExecutorService(), isParallelProcessing);
        ExecutorService threadPool = getConfiguredExecutorService(routeContext, "OnCompletion", definition.getExecutorService(), isParallelProcessing);

        // should be after consumer by default
        boolean afterConsumer = definition.getMode() == null || definition.getMode() == OnCompletionMode.AfterConsumer;

        OnCompletionProcessor answer = new OnCompletionProcessor(routeContext.getCamelContext(), internal,
                threadPool, shutdownThreadPool, isOnCompleteOnly, isOnFailureOnly, when, original, afterConsumer);
        return answer;
    }


}
