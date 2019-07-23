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

import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ResequenceDefinition;
import org.apache.camel.model.config.BatchResequencerConfig;
import org.apache.camel.model.config.ResequencerConfig;
import org.apache.camel.model.config.StreamResequencerConfig;
import org.apache.camel.processor.CamelInternalProcessor;
import org.apache.camel.processor.Resequencer;
import org.apache.camel.processor.StreamResequencer;
import org.apache.camel.processor.resequencer.DefaultExchangeComparator;
import org.apache.camel.processor.resequencer.ExpressionResultComparator;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.support.CamelContextHelper;
import org.apache.camel.util.ObjectHelper;

public class ResequenceReifier<Type extends ProcessorDefinition<Type>> extends ProcessorReifier<ResequenceDefinition<Type>> {

    @SuppressWarnings("unchecked")
    ResequenceReifier(ProcessorDefinition<?> definition) {
        super((ResequenceDefinition) definition);
    }

    @Override
    public Processor createProcessor(RouteContext routeContext) throws Exception {
        ResequencerConfig config = definition.getResequencerConfig();
        if (config instanceof StreamResequencerConfig) {
            return createStreamResequencer(routeContext, (StreamResequencerConfig) config);
        } else if (config instanceof BatchResequencerConfig) {
            return createBatchResequencer(routeContext, (BatchResequencerConfig) config);
        } else {
            throw new IllegalArgumentException("Unsupported config type " + config.getClass().getName());
        }
    }

    /**
     * Creates a batch {@link Resequencer} instance applying the given <code>config</code>.
     *
     * @param routeContext route context.
     * @param config batch resequencer configuration.
     * @return the configured batch resequencer.
     * @throws Exception can be thrown
     */
    @SuppressWarnings("deprecation")
    protected Resequencer createBatchResequencer(RouteContext routeContext,
                                                 BatchResequencerConfig config) throws Exception {
        Processor processor = this.createChildProcessor(routeContext, true);
        Expression expression = definition.getExpression().createExpression(routeContext);

        // and wrap in unit of work
        CamelInternalProcessor internal = new CamelInternalProcessor(processor);
        internal.addAdvice(new CamelInternalProcessor.UnitOfWorkProcessorAdvice(routeContext));

        ObjectHelper.notNull(config, "config", this);
        ObjectHelper.notNull(expression, "expression", this);

        boolean isReverse = config.getReverse() != null && asBoolean(routeContext, config.getReverse());
        boolean isAllowDuplicates = config.getAllowDuplicates() != null && asBoolean(routeContext, config.getAllowDuplicates());

        Resequencer resequencer = new Resequencer(routeContext.getCamelContext(), internal, expression, isAllowDuplicates, isReverse);
        resequencer.setBatchSize(asInt(routeContext, config.getBatchSize()));
        resequencer.setBatchTimeout(asLong(routeContext, config.getBatchTimeout()));
        resequencer.setReverse(isReverse);
        resequencer.setAllowDuplicates(isAllowDuplicates);
        if (config.getIgnoreInvalidExchanges() != null) {
            resequencer.setIgnoreInvalidExchanges(asBoolean(routeContext, config.getIgnoreInvalidExchanges()));
        }
        return resequencer;
    }

    /**
     * Creates a {@link StreamResequencer} instance applying the given <code>config</code>.
     *
     * @param routeContext route context.
     * @param config stream resequencer configuration.
     * @return the configured stream resequencer.
     * @throws Exception can be thrwon
     */
    protected StreamResequencer createStreamResequencer(RouteContext routeContext,
                                                        StreamResequencerConfig config) throws Exception {
        Processor processor = this.createChildProcessor(routeContext, true);
        Expression expression = definition.getExpression().createExpression(routeContext);

        CamelInternalProcessor internal = new CamelInternalProcessor(processor);
        internal.addAdvice(new CamelInternalProcessor.UnitOfWorkProcessorAdvice(routeContext));

        ObjectHelper.notNull(config, "config", this);
        ObjectHelper.notNull(expression, "expression", this);

        ExpressionResultComparator comparator = resolve(routeContext, ExpressionResultComparator.class, config.getComparator());
        if (comparator == null) {
            comparator = new DefaultExchangeComparator();
        }
        comparator.setExpression(expression);

        StreamResequencer resequencer = new StreamResequencer(routeContext.getCamelContext(), internal, comparator, expression);
        resequencer.setTimeout(asLong(routeContext, config.getTimeout()));
        if (config.getDeliveryAttemptInterval() != null) {
            resequencer.setDeliveryAttemptInterval(asLong(routeContext, config.getDeliveryAttemptInterval()));
        }
        resequencer.setCapacity(asInt(routeContext, config.getCapacity()));
        resequencer.setRejectOld(asBoolean(routeContext, config.getRejectOld()));
        if (config.getIgnoreInvalidExchanges() != null) {
            resequencer.setIgnoreInvalidExchanges(asBoolean(routeContext, config.getIgnoreInvalidExchanges()));
        }
        return resequencer;
    }

}
