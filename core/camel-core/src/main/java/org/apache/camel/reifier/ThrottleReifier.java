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

import java.util.concurrent.ScheduledExecutorService;

import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ProcessorDefinitionHelper;
import org.apache.camel.model.ThrottleDefinition;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.processor.Throttler;
import org.apache.camel.spi.RouteContext;

public class ThrottleReifier<Type extends ProcessorDefinition<Type>> extends ExpressionReifier<ThrottleDefinition<Type>> {

    @SuppressWarnings("unchecked")
    ThrottleReifier(ProcessorDefinition<?> definition) {
        super((ThrottleDefinition) definition);
    }

    @Override
    public Processor createProcessor(RouteContext routeContext) throws Exception {
        boolean async = asBoolean(routeContext, definition.getAsyncDelayed(), false);
        boolean shutdownThreadPool = willCreateNewThreadPool(routeContext, definition.getExecutorService(), true);
        ScheduledExecutorService threadPool = getConfiguredScheduledExecutorService(routeContext, "Throttle", definition.getExecutorService(), true);

        // should be default 1000 millis
        long period = asLong(routeContext, definition.getTimePeriodMillis(), 1000L);

        // max requests per period is mandatory
        Expression maxRequestsExpression = createMaxRequestsPerPeriodExpression(routeContext);
        if (maxRequestsExpression == null) {
            throw new IllegalArgumentException("MaxRequestsPerPeriod expression must be provided on " + this);
        }

        Expression correlation = null;
        if (definition.getCorrelationExpression() != null) {
            correlation = definition.getCorrelationExpression().createExpression(routeContext);
        }

        boolean reject = asBoolean(routeContext, definition.getRejectExecution(), false);
        Throttler answer = new Throttler(routeContext.getCamelContext(), maxRequestsExpression, period, threadPool, shutdownThreadPool, reject, correlation);

        answer.setAsyncDelayed(async);
        // should be true by default
        answer.setCallerRunsWhenRejected(asBoolean(routeContext, definition.getCallerRunsWhenRejected(), true));

        return answer;
    }

    private Expression createMaxRequestsPerPeriodExpression(RouteContext routeContext) {
        ExpressionDefinition expr = definition.getExpression();
        if (expr != null) {
            return expr.createExpression(routeContext);
        }
        return null;
    }

}
