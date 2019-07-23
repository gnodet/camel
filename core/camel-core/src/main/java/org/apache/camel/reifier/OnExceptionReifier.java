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

import java.util.List;

import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.ErrorHandlerBuilder;
import org.apache.camel.model.OnExceptionDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.processor.CatchProcessor;
import org.apache.camel.processor.FatalFallbackErrorHandler;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.util.ObjectHelper;

public class OnExceptionReifier<Type extends ProcessorDefinition<Type>> extends ProcessorReifier<OnExceptionDefinition<Type>> {

    @SuppressWarnings("unchecked")
    OnExceptionReifier(ProcessorDefinition<?> definition) {
        super((OnExceptionDefinition) definition);
    }

    @Override
    public void addRoutes(RouteContext routeContext) throws Exception {
        // must validate configuration before creating processor
        validateConfiguration(routeContext);

        if (asBoolean(routeContext, definition.getUseOriginalMessage(), false)) {
            // ensure allow original is turned on
            routeContext.setAllowUseOriginalMessage(true);
        }

        // lets attach this on exception to the route error handler
        Processor child = createOutputsProcessor(routeContext);
        if (child != null) {
            // wrap in our special safe fallback error handler if OnException have child output
            Processor errorHandler = new FatalFallbackErrorHandler(child);
            String id = getId(definition, routeContext);
            routeContext.setOnException(id, errorHandler);
        }
        // lookup the error handler builder
        ErrorHandlerBuilder builder = (ErrorHandlerBuilder) routeContext.getErrorHandlerFactory();
        // and add this as error handlers
        routeContext.addErrorHandler(builder, definition);
    }

    @Override
    public CatchProcessor createProcessor(RouteContext routeContext) throws Exception {
        // load exception classes
        List<Class<? extends Throwable>> exceptions = resolveExceptions(routeContext, definition.getExceptions());

        // must validate configuration before creating processor
        validateConfiguration(routeContext);

        if (asBoolean(routeContext, definition.getUseOriginalMessage(), false)) {
            // ensure allow original is turned on
            routeContext.setAllowUseOriginalMessage(true);
        }

        Processor childProcessor = this.createChildProcessor(routeContext, false);

        Predicate when = null;
        if (definition.getOnWhen() != null) {
            when = asPredicate(routeContext, definition.getOnWhen());
        }

        Predicate handle = null;
        if (definition.getHandled() != null) {
            handle = asPredicate(routeContext, definition.getHandled());
        }

        return new CatchProcessor(exceptions, childProcessor, when, handle);
    }

    protected void validateConfiguration(RouteContext routeContext) {
        if (definition.isInheritErrorHandler() != null && definition.isInheritErrorHandler()) {
            throw new IllegalArgumentException(this + " cannot have the inheritErrorHandler option set to true");
        }

        List<Class<? extends Throwable>> exceptions = resolveExceptions(routeContext, definition.getExceptions());
        if (exceptions == null || exceptions.isEmpty()) {
            throw new IllegalArgumentException("At least one exception must be configured on " + this);
        }

        // only one of handled or continued is allowed
        if (definition.getHandled() != null && definition.getContinued() != null) {
            throw new IllegalArgumentException("Only one of handled or continued is allowed to be configured on: " + this);
        }

        // validate that at least some option is set as you cannot just have onException(Exception.class);
        if (definition.getOutputs() == null || definition.getOutputs().isEmpty()) {
            // no outputs so there should be some sort of configuration
            ObjectHelper.firstNotNull(
                    definition.getHandled(),
                    definition.getContinued(),
                    definition.getRetryWhile(),
                    definition.getRedeliveryPolicy(),
                    definition.getUseOriginalMessage(),
                    definition.getOnRedelivery(),
                    definition.getOnExceptionOccurred())
                    .orElseThrow(() -> new IllegalArgumentException(this + " is not configured."));
        }
    }

}
