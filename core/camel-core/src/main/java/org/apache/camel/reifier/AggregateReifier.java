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
import java.util.concurrent.ScheduledExecutorService;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.model.AggregateDefinition;
import org.apache.camel.model.OptimisticLockRetryPolicyDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.processor.CamelInternalProcessor;
import org.apache.camel.processor.aggregate.AggregateProcessor;
import org.apache.camel.processor.aggregate.OptimisticLockRetryPolicy;
import org.apache.camel.spi.AggregateController;
import org.apache.camel.spi.AggregationRepository;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.util.concurrent.SynchronousExecutorService;

public class AggregateReifier<Type extends ProcessorDefinition<Type>> extends ProcessorReifier<AggregateDefinition<Type>> {

    @SuppressWarnings("unchecked")
    AggregateReifier(ProcessorDefinition<?> definition) {
        super((AggregateDefinition) definition);
    }

    @Override
    public Processor createProcessor(RouteContext routeContext) throws Exception {
        return createAggregator(routeContext);
    }

    protected AggregateProcessor createAggregator(RouteContext routeContext) throws Exception {
        Processor childProcessor = this.createChildProcessor(routeContext, true);

        // wrap the aggregate route in a unit of work processor
        CamelInternalProcessor internal = new CamelInternalProcessor(childProcessor);
        internal.addAdvice(new CamelInternalProcessor.UnitOfWorkProcessorAdvice(routeContext));

        Expression correlation = asExpression(routeContext, definition.getCorrelationExpression());
        AggregationStrategy strategy = createAggregationStrategy(routeContext);

        boolean parallel = asBoolean(routeContext, definition.getParallelProcessing(), false);
        boolean shutdownThreadPool = willCreateNewThreadPool(routeContext, definition.getExecutorService(), parallel);
        ExecutorService threadPool = getConfiguredExecutorService(routeContext, "Aggregator", definition.getExecutorService(), parallel);
        if (threadPool == null && !parallel) {
            // executor service is mandatory for the Aggregator
            // we do not run in parallel mode, but use a synchronous executor, so we run in current thread
            threadPool = new SynchronousExecutorService();
            shutdownThreadPool = true;
        }

        AggregateProcessor answer = new AggregateProcessor(routeContext.getCamelContext(), internal,
                correlation, strategy, threadPool, shutdownThreadPool);

        if (definition.getAggregationRepository() != null) {
            AggregationRepository repository = resolve(routeContext, AggregationRepository.class, definition.getAggregationRepository());
            answer.setAggregationRepository(repository);
        }

        // this EIP supports using a shared timeout checker thread pool or fallback to create a new thread pool
        if (definition.getTimeoutCheckerExecutorService() instanceof ScheduledExecutorService) {
            answer.setTimeoutCheckerExecutorService((ScheduledExecutorService) definition.getTimeoutCheckerExecutorService());
        } else if (definition.getTimeoutCheckerExecutorService() instanceof String) {
            String ref = routeContext.getCamelContext().resolvePropertyPlaceholders((String) definition.getTimeoutCheckerExecutorService());
            // lookup existing thread pool
            ScheduledExecutorService timeoutThreadPool = routeContext.lookup(ref, ScheduledExecutorService.class);
            if (timeoutThreadPool == null) {
                // then create a thread pool assuming the ref is a thread pool profile id
                timeoutThreadPool = routeContext.getCamelContext().getExecutorServiceManager().newScheduledThreadPool(this,
                        AggregateProcessor.AGGREGATE_TIMEOUT_CHECKER, ref);
                if (timeoutThreadPool == null) {
                    throw new IllegalArgumentException("ExecutorServiceRef " + ref
                            + " not found in registry (as an ScheduledExecutorService instance) or as a thread pool profile.");
                }
                answer.setShutdownTimeoutCheckerExecutorService(true);
            }
            answer.setTimeoutCheckerExecutorService(timeoutThreadPool);
        }

        // set other options
        answer.setParallelProcessing(parallel);
        if (definition.getOptimisticLocking() != null) {
            answer.setOptimisticLocking(asBoolean(routeContext, definition.getOptimisticLocking()));
        }
        if (definition.getCompletionPredicate() != null) {
            Predicate predicate = asPredicate(routeContext, definition.getCompletionPredicate());
            answer.setCompletionPredicate(predicate);
        } else if (strategy instanceof Predicate) {
            // if aggregation strategy implements predicate and was not configured then use as fallback
            log.debug("Using AggregationStrategy as completion predicate: {}", strategy);
            answer.setCompletionPredicate((Predicate) strategy);
        }
        Object completionTimeout = definition.getCompletionTimeout();
        if (completionTimeout != null) {
            if (completionTimeout instanceof Long) {
                answer.setCompletionTimeout((Long) completionTimeout);
            } else if (completionTimeout instanceof ExpressionDefinition) {
                answer.setCompletionTimeoutExpression(((ExpressionDefinition) completionTimeout).createExpression(routeContext));
            } else if (completionTimeout instanceof Expression) {
                answer.setCompletionTimeoutExpression((Expression) completionTimeout);
            } else if (completionTimeout instanceof String) {
                String val = routeContext.getCamelContext().resolvePropertyPlaceholders((String) completionTimeout);
                try {
                    answer.setCompletionTimeout(Long.parseLong(val));
                } catch (NumberFormatException e) {
                    answer.setCompletionTimeoutExpression(ExpressionBuilder.simpleExpression(val));
                }
            } else {
                throw new IllegalArgumentException("Cannot convert completionTimeout '" + completionTimeout + "' to an Expression");
            }
        }
        if (definition.getCompletionInterval() != null) {
            answer.setCompletionInterval(asLong(routeContext, definition.getCompletionInterval()));
        }
        Object completionSize = definition.getCompletionSize();
        if (completionSize != null) {
            if (completionSize instanceof Integer) {
                answer.setCompletionSize((Integer) completionSize);
            } else if (completionSize instanceof ExpressionDefinition) {
                answer.setCompletionSizeExpression(((ExpressionDefinition) completionSize).createExpression(routeContext));
            } else if (completionSize instanceof Expression) {
                answer.setCompletionSizeExpression((Expression) completionSize);
            } else if (completionSize instanceof String) {
                String val = routeContext.getCamelContext().resolvePropertyPlaceholders((String) completionSize);
                try {
                    answer.setCompletionSize(Integer.parseInt(val));
                } catch (NumberFormatException e) {
                    answer.setCompletionSizeExpression(ExpressionBuilder.simpleExpression(val));
                }
            } else {
                throw new IllegalArgumentException("Cannot convert completionSize '" + completionSize + "' to an Expression");
            }
        }
        if (definition.getCompletionFromBatchConsumer() != null) {
            answer.setCompletionFromBatchConsumer(asBoolean(routeContext, definition.getCompletionFromBatchConsumer()));
        }
        if (definition.getCompletionOnNewCorrelationGroup() != null) {
            answer.setCompletionOnNewCorrelationGroup(asBoolean(routeContext, definition.getCompletionOnNewCorrelationGroup()));
        }
        if (definition.getEagerCheckCompletion() != null) {
            answer.setEagerCheckCompletion(asBoolean(routeContext, definition.getEagerCheckCompletion()));
        }
        if (definition.getIgnoreInvalidCorrelationKeys() != null) {
            answer.setIgnoreInvalidCorrelationKeys(asBoolean(routeContext, definition.getIgnoreInvalidCorrelationKeys()));
        }
        if (definition.getCloseCorrelationKeyOnCompletion() != null) {
            answer.setCloseCorrelationKeyOnCompletion(asInt(routeContext, definition.getCloseCorrelationKeyOnCompletion()));
        }
        if (definition.getDiscardOnCompletionTimeout() != null) {
            answer.setDiscardOnCompletionTimeout(asBoolean(routeContext, definition.getDiscardOnCompletionTimeout()));
        }
        if (definition.getForceCompletionOnStop() != null) {
            answer.setForceCompletionOnStop(asBoolean(routeContext, definition.getForceCompletionOnStop()));
        }
        if (definition.getCompleteAllOnStop() != null) {
            answer.setCompleteAllOnStop(asBoolean(routeContext, definition.getCompleteAllOnStop()));
        }
        if (definition.getOptimisticLockRetryPolicy() != null) {
            answer.setOptimisticLockRetryPolicy(createOptimisticLockRetryPolicy(routeContext, definition.getOptimisticLockRetryPolicy()));
        }
        if (definition.getAggregationController() != null) {
            AggregateController controller = resolve(routeContext, AggregateController.class, definition.getAggregationController());
            answer.setAggregateController(controller);
        }
        if (definition.getCompletionTimeoutCheckerInterval() != null) {
            answer.setCompletionTimeoutCheckerInterval(asLong(routeContext, definition.getCompletionTimeoutCheckerInterval()));
        }
        return answer;
    }

    public OptimisticLockRetryPolicy createOptimisticLockRetryPolicy(RouteContext routeContext, OptimisticLockRetryPolicyDefinition definition) {
        OptimisticLockRetryPolicy policy = new OptimisticLockRetryPolicy();
        if (definition.getMaximumRetries() != null) {
            policy.setMaximumRetries(asInt(routeContext, definition.getMaximumRetries()));
        }
        if (definition.getRetryDelay() != null) {
            policy.setRetryDelay(asLong(routeContext, definition.getRetryDelay()));
        }
        if (definition.getMaximumRetryDelay() != null) {
            policy.setMaximumRetryDelay(asLong(routeContext, definition.getMaximumRetryDelay()));
        }
        if (definition.getExponentialBackOff() != null) {
            policy.setExponentialBackOff(asBoolean(routeContext, definition.getExponentialBackOff()));
        }
        if (definition.getRandomBackOff() != null) {
            policy.setRandomBackOff(asBoolean(routeContext, definition.getRandomBackOff()));
        }
        return policy;
    }

    private AggregationStrategy createAggregationStrategy(RouteContext routeContext) {
        Object value = definition.getAggregationStrategy();
        Object method = definition.getStrategyMethodName();
        Object allowNull = definition.getStrategyMethodAllowNull();
        return resolveAggregationStrategy(routeContext, value, method, allowNull, () -> {
            throw new IllegalArgumentException("AggregationStrategy must be set on " + definition);
        });
    }

}
