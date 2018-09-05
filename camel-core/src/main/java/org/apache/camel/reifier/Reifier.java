/**
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.ExpressionBuilder;
import static org.apache.camel.builder.ExpressionBuilder.bodyExpression;
import org.apache.camel.builder.ProcessorBuilder;
import org.apache.camel.model.AOPDefinition;
import org.apache.camel.model.AggregateDefinition;
import org.apache.camel.model.BeanDefinition;
import org.apache.camel.model.CatchDefinition;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.ClaimCheckDefinition;
import org.apache.camel.model.ConvertBodyDefinition;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.DelayDefinition;
import org.apache.camel.model.DynamicRouterDefinition;
import org.apache.camel.model.EnrichDefinition;
import org.apache.camel.model.FilterDefinition;
import org.apache.camel.model.FinallyDefinition;
import org.apache.camel.model.HystrixDefinition;
import org.apache.camel.model.IdempotentConsumerDefinition;
import org.apache.camel.model.InOnlyDefinition;
import org.apache.camel.model.InOutDefinition;
import org.apache.camel.model.InterceptDefinition;
import org.apache.camel.model.InterceptFromDefinition;
import org.apache.camel.model.InterceptSendToEndpointDefinition;
import org.apache.camel.model.LoadBalanceDefinition;
import org.apache.camel.model.LogDefinition;
import org.apache.camel.model.LoopDefinition;
import org.apache.camel.model.MarshalDefinition;
import org.apache.camel.model.MulticastDefinition;
import org.apache.camel.model.OnCompletionDefinition;
import org.apache.camel.model.OnExceptionDefinition;
import org.apache.camel.model.OnFallbackDefinition;
import org.apache.camel.model.OtherwiseDefinition;
import org.apache.camel.model.PipelineDefinition;
import org.apache.camel.model.PolicyDefinition;
import org.apache.camel.model.PollEnrichDefinition;
import org.apache.camel.model.ProcessDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RecipientListDefinition;
import org.apache.camel.model.RemoveHeaderDefinition;
import org.apache.camel.model.RemoveHeadersDefinition;
import org.apache.camel.model.RemovePropertiesDefinition;
import org.apache.camel.model.RemovePropertyDefinition;
import org.apache.camel.model.ResequenceDefinition;
import org.apache.camel.model.RollbackDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutingSlipDefinition;
import org.apache.camel.model.SagaDefinition;
import org.apache.camel.model.SamplingDefinition;
import org.apache.camel.model.ScriptDefinition;
import org.apache.camel.model.SetBodyDefinition;
import org.apache.camel.model.SetExchangePatternDefinition;
import org.apache.camel.model.SetFaultBodyDefinition;
import org.apache.camel.model.SetHeaderDefinition;
import org.apache.camel.model.SetOutHeaderDefinition;
import org.apache.camel.model.SetPropertyDefinition;
import org.apache.camel.model.SortDefinition;
import org.apache.camel.model.SplitDefinition;
import org.apache.camel.model.StopDefinition;
import org.apache.camel.model.ThreadsDefinition;
import org.apache.camel.model.ThrottleDefinition;
import org.apache.camel.model.ThrowExceptionDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.model.ToDynamicDefinition;
import org.apache.camel.model.TransactedDefinition;
import org.apache.camel.model.TransformDefinition;
import org.apache.camel.model.TryDefinition;
import org.apache.camel.model.UnmarshalDefinition;
import org.apache.camel.model.ValidateDefinition;
import org.apache.camel.model.WhenDefinition;
import org.apache.camel.model.WhenSkipSendToEndpointDefinition;
import org.apache.camel.model.WireTapDefinition;
import org.apache.camel.model.cloud.ServiceCallDefinition;
import org.apache.camel.model.transformer.CustomTransformerDefinition;
import org.apache.camel.model.transformer.DataFormatTransformerDefinition;
import org.apache.camel.model.transformer.EndpointTransformerDefinition;
import org.apache.camel.model.transformer.TransformerDefinition;
import org.apache.camel.processor.ExchangePatternProcessor;
import org.apache.camel.processor.FilterProcessor;
import org.apache.camel.processor.RemoveHeaderProcessor;
import org.apache.camel.processor.RemoveHeadersProcessor;
import org.apache.camel.processor.RemovePropertiesProcessor;
import org.apache.camel.processor.RemovePropertyProcessor;
import org.apache.camel.processor.SamplingThrottler;
import org.apache.camel.processor.ScriptProcessor;
import org.apache.camel.processor.SetBodyProcessor;
import org.apache.camel.processor.SetHeaderProcessor;
import org.apache.camel.processor.SetPropertyProcessor;
import org.apache.camel.processor.SortProcessor;
import org.apache.camel.processor.StopProcessor;
import org.apache.camel.processor.TransformProcessor;
import org.apache.camel.processor.UnmarshalProcessor;
import org.apache.camel.processor.validation.PredicateValidatingProcessor;
import org.apache.camel.reifier.transformer.CustomTransformeReifier;
import org.apache.camel.reifier.transformer.DataFormatTransformeReifier;
import org.apache.camel.reifier.transformer.EndpointTransformeReifier;
import org.apache.camel.reifier.transformer.TransformerReifier;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.util.ObjectHelper;

public class Reifier {

    private static final Map<Class<?>, Function<TransformerDefinition, TransformerReifier<? extends TransformerDefinition>>> TRANSFORMERS;
    static {
        Map<Class<?>, Function<TransformerDefinition, TransformerReifier<? extends TransformerDefinition>>> map = new HashMap<>();
        map.put(CustomTransformerDefinition.class, CustomTransformeReifier::new);
        map.put(DataFormatTransformerDefinition.class, DataFormatTransformeReifier::new);
        map.put(EndpointTransformerDefinition.class, EndpointTransformeReifier::new);
        TRANSFORMERS = map;
    }
    public static TransformerReifier<? extends TransformerDefinition> reifier(TransformerDefinition definition) {
        Function<TransformerDefinition, TransformerReifier<? extends TransformerDefinition>> reifier = TRANSFORMERS.get(definition.getClass());
        if (reifier != null) {
            return reifier.apply(definition);
        }
        throw new IllegalStateException("Unsupported definition: " + definition);
    }

    private static final Map<Class<?>, Function<ProcessorDefinition<?>, ProcessorReifier<? extends ProcessorDefinition<?>>>> PROCESSORS;
    static {
        Map<Class<?>, Function<ProcessorDefinition<?>, ProcessorReifier<? extends ProcessorDefinition<?>>>> map = new HashMap<>();
        map.put(AggregateDefinition.class, AggregateReifier::new);
        map.put(AOPDefinition.class, AOPReifier::new);
        map.put(BeanDefinition.class, BeanReifier::new);
        map.put(CatchDefinition.class, CatchReifier::new);
        map.put(ChoiceDefinition.class, ChoiceReifier::new);
        map.put(ClaimCheckDefinition.class, ClaimCheckReifier::new);
        map.put(ConvertBodyDefinition.class, ConvertBodyReifier::new);
        map.put(DelayDefinition.class, DelayReifier::new);
        map.put(DynamicRouterDefinition.class, DynamicRouterReifier::new);
        map.put(EnrichDefinition.class, EnrichReifier::new);
        map.put(FilterDefinition.class, FilterReifier::new);
        map.put(FinallyDefinition.class, FinallyReifier::new);
        map.put(HystrixDefinition.class, HystrixReifier::new);
        map.put(IdempotentConsumerDefinition.class, IdempotentConsumerReifier::new);
        map.put(InOnlyDefinition.class, SendReifier::new);
        map.put(InOutDefinition.class, SendReifier::new);
        map.put(InterceptDefinition.class, InterceptReifier::new);
        map.put(InterceptFromDefinition.class, InterceptFromReifier::new);
        map.put(InterceptSendToEndpointDefinition.class, InterceptSendToEndpointReifier::new);
        map.put(LoadBalanceDefinition.class, LoadBalanceReifier::new);
        map.put(LogDefinition.class, LogReifier::new);
        map.put(LoopDefinition.class, LoopReifier::new);
        map.put(MarshalDefinition.class, MarshalReifier::new);
        map.put(MulticastDefinition.class, MulticastReifier::new);
        map.put(OnCompletionDefinition.class, OnCompletionReifier::new);
        map.put(OnExceptionDefinition.class, OnExceptionReifier::new);
        map.put(OnFallbackDefinition.class, OnFallbackReifier::new);
        map.put(OtherwiseDefinition.class, OtherwiseReifier::new);
        map.put(PipelineDefinition.class, PipelineReifier::new);
        map.put(PolicyDefinition.class, PolicyReifier::new);
        map.put(PollEnrichDefinition.class, PollEnrichReifier::new);
        map.put(ProcessDefinition.class, ProcessReifier::new);
        map.put(RecipientListDefinition.class, RecipientListReifier::new);
        map.put(RemoveHeaderDefinition.class, RemoveHeaderReifier::new);
        map.put(RemoveHeadersDefinition.class, RemoveHeadersReifier::new);
        map.put(RemovePropertiesDefinition.class, RemovePropertiesReifier::new);
        map.put(RemovePropertyDefinition.class, RemovePropertyReifier::new);
        map.put(ResequenceDefinition.class, ResequenceReifier::new);
        map.put(RollbackDefinition.class, RollbackReifier::new);
        map.put(RouteDefinition.class, RouteReifier::new);
        map.put(RoutingSlipDefinition.class, RoutingSlipReifier::new);
        map.put(SagaDefinition.class, SagaReifier::new);
        map.put(SamplingDefinition.class, SamplingReifier::new);
        map.put(ScriptDefinition.class, ScriptReifier::new);
        map.put(ServiceCallDefinition.class, ServiceCallReifier::new);
        map.put(SetBodyDefinition.class, SetBodyReifier::new);
        map.put(SetExchangePatternDefinition.class, SetExchangePatternReifier::new);
        map.put(SetFaultBodyDefinition.class, SetFaultBodyReifier::new);
        map.put(SetHeaderDefinition.class, SetHeaderReifier::new);
        map.put(SetOutHeaderDefinition.class, SetOutHeaderReifier::new);
        map.put(SetPropertyDefinition.class, SetPropertyReifier::new);
        map.put(SortDefinition.class, SortReifier::new);
        map.put(SplitDefinition.class, SplitReifier::new);
        map.put(StopDefinition.class, StopReifier::new);
        map.put(ThreadsDefinition.class, ThreadsReifier::new);
        map.put(ThrottleDefinition.class, ThrottleReifier::new);
        map.put(ThrowExceptionDefinition.class, ThrowExceptionReifier::new);
        map.put(ToDefinition.class, SendReifier::new);
        map.put(ToDynamicDefinition.class, ToDynamicReifier::new);
        map.put(TransactedDefinition.class, TransactedReifier::new);
        map.put(TransformDefinition.class, TransformReifier::new);
        map.put(TryDefinition.class, TryReifier::new);
        map.put(UnmarshalDefinition.class, UnmarshalReifier::new);
        map.put(ValidateDefinition.class, ValidateReifier::new);
        map.put(WireTapDefinition.class, WireTapReifier::new);
        map.put(WhenSkipSendToEndpointDefinition.class, WhenSkipSendToEndpointReifier::new);
        map.put(WhenDefinition.class, WhenReifier::new);
        PROCESSORS = map;
    }
    public static ProcessorReifier<? extends ProcessorDefinition<?>> reifier(ProcessorDefinition<?> definition) {
        Function<ProcessorDefinition<?>, ProcessorReifier<? extends ProcessorDefinition<?>>> reifier = PROCESSORS.get(definition.getClass());
        if (reifier != null) {
            return reifier.apply(definition);
        }
        throw new IllegalStateException("Unsupported definition: " + definition);
    }

    private static class SortReifier<T extends SortDefinition<T>> extends ExpressionReifier<T> {

        public SortReifier(ProcessorDefinition<?> definition) {
            super((T) definition);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Processor createProcessor(RouteContext routeContext) throws Exception {
            // lookup in registry
            if (ObjectHelper.isNotEmpty(definition.getComparatorRef())) {
                definition.setComparator(routeContext.getCamelContext().getRegistry().lookupByNameAndType(definition.getComparatorRef(), Comparator.class));
            }

            // if no comparator then default on to string representation
            if (definition.getComparator() == null) {
                definition.setComparator(new Comparator<T>() {
                    public int compare(T o1, T o2) {
                        return ObjectHelper.compare(o1, o2);
                    }
                });
            }

            // if no expression provided then default to body expression
            Expression exp;
            if (definition.getExpression() == null) {
                exp = bodyExpression();
            } else {
                exp = definition.getExpression().createExpression(routeContext);
            }
            return new SortProcessor<T>(exp, definition.getComparator());
        }

    }

    private static class OtherwiseReifier extends ProcessorReifier<OtherwiseDefinition> {

        public OtherwiseReifier(ProcessorDefinition<?> definition) {
            super(OtherwiseDefinition.class.cast(definition));
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) throws Exception {
            return this.createChildProcessor(routeContext, false);
        }
    }

    private static class PipelineReifier extends ProcessorReifier<PipelineDefinition> {

        public PipelineReifier(ProcessorDefinition<?> definition) {
            super(PipelineDefinition.class.cast(definition));
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) throws Exception {
            return this.createChildProcessor(routeContext, true);
        }
    }

    private static class RemoveHeaderReifier extends ProcessorReifier<RemoveHeaderDefinition> {
        public RemoveHeaderReifier(ProcessorDefinition<?> definition) {
            super((RemoveHeaderDefinition) definition);
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) throws Exception {
            ObjectHelper.notNull(definition.getHeaderName(), "headerName", this);
            return new RemoveHeaderProcessor(definition.getHeaderName());
        }
    }

    private static class RemoveHeadersReifier extends ProcessorReifier<RemoveHeadersDefinition> {
        public RemoveHeadersReifier(ProcessorDefinition<?> definition) {
            super((RemoveHeadersDefinition) definition);
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) throws Exception {
            ObjectHelper.notNull(definition.getPattern(), "patterns", definition);
            if (definition.getExcludePatterns() != null) {
                return new RemoveHeadersProcessor(definition.getPattern(), definition.getExcludePatterns());
            } else if (definition.getExcludePattern() != null) {
                return new RemoveHeadersProcessor(definition.getPattern(), new String[]{definition.getExcludePattern()});
            } else {
                return new RemoveHeadersProcessor(definition.getPattern(), null);
            }
        }
    }

    private static class RemovePropertiesReifier extends ProcessorReifier<RemovePropertiesDefinition> {
        public RemovePropertiesReifier(ProcessorDefinition<?> definition) {
            super((RemovePropertiesDefinition) definition);
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) throws Exception {
            ObjectHelper.notNull(definition.getPattern(), "patterns", this);
            if (definition.getExcludePatterns() != null) {
                return new RemovePropertiesProcessor(definition.getPattern(), definition.getExcludePatterns());
            } else if (definition.getExcludePattern() != null) {
                return new RemovePropertiesProcessor(definition.getPattern(), new String[]{definition.getExcludePattern()});
            } else {
                return new RemovePropertiesProcessor(definition.getPattern(), null);
            }
        }
    }

    private static class RemovePropertyReifier extends ProcessorReifier<RemovePropertyDefinition> {
        public RemovePropertyReifier(ProcessorDefinition<?> definition) {
            super((RemovePropertyDefinition) definition);
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) throws Exception {
            ObjectHelper.notNull(definition.getPropertyName(), "propertyName", this);
            return new RemovePropertyProcessor(definition.getPropertyName());
        }
    }

    private static class SamplingReifier extends ProcessorReifier<SamplingDefinition> {
        public SamplingReifier(ProcessorDefinition<?> definition) {
            super((SamplingDefinition) definition);
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) throws Exception {
            Processor childProcessor = this.createChildProcessor(routeContext, true);
            if (definition.getMessageFrequency() != null) {
                return new SamplingThrottler(childProcessor, definition.getMessageFrequency());
            } else {
                // should default be 1 sample period
                long time = definition.getSamplePeriod() != null ? definition.getSamplePeriod() : 1L;
                // should default be in seconds
                TimeUnit tu = definition.getUnits() != null ? definition.getUnits() : TimeUnit.SECONDS;
                return new SamplingThrottler(childProcessor, time, tu);
            }
        }
    }

    private static class ScriptReifier extends ExpressionReifier<ScriptDefinition> {
        public ScriptReifier(ProcessorDefinition<?> definition) {
            super((ScriptDefinition) definition);
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) throws Exception {
            Expression expr = definition.getExpression().createExpression(routeContext);
            return new ScriptProcessor(expr);
        }
    }

    private static class SetBodyReifier extends ExpressionReifier<SetBodyDefinition> {
        public SetBodyReifier(ProcessorDefinition<?> definition) {
            super((SetBodyDefinition) definition);
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) throws Exception {
            Expression expr = definition.getExpression().createExpression(routeContext);
            return new SetBodyProcessor(expr);
        }
    }

    private static class SetExchangePatternReifier extends ProcessorReifier<SetExchangePatternDefinition> {
        public SetExchangePatternReifier(ProcessorDefinition<?> definition) {
            super((SetExchangePatternDefinition) definition);
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) {
            return new ExchangePatternProcessor(definition.getPattern());
        }
    }

    private static class SetFaultBodyReifier extends ExpressionReifier<SetFaultBodyDefinition> {
        public SetFaultBodyReifier(ProcessorDefinition<?> definition) {
            super((SetFaultBodyDefinition) definition);
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) throws Exception {
            Expression expr = definition.getExpression().createExpression(routeContext);
            return ProcessorBuilder.setFaultBody(expr);
        }
    }

    private static class SetHeaderReifier extends ExpressionReifier<SetHeaderDefinition> {
        public SetHeaderReifier(ProcessorDefinition<?> definition) {
            super((SetHeaderDefinition) definition);
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) throws Exception {
            ObjectHelper.notNull(definition.getHeaderName(), "headerName");
            Expression expr = definition.getExpression().createExpression(routeContext);
            Expression nameExpr = ExpressionBuilder.parseSimpleOrFallbackToConstantExpression(definition.getHeaderName(), routeContext.getCamelContext());
            return new SetHeaderProcessor(nameExpr, expr);
        }
    }

    private static class SetOutHeaderReifier extends ExpressionReifier<SetOutHeaderDefinition> {
        public SetOutHeaderReifier(ProcessorDefinition<?> definition) {
            super((SetOutHeaderDefinition) definition);
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) throws Exception {
            ObjectHelper.notNull(definition.getHeaderName(), "headerName", this);
            Expression expr = definition.getExpression().createExpression(routeContext);
            return ProcessorBuilder.setOutHeader(definition.getHeaderName(), expr);
        }
    }

    private static class SetPropertyReifier extends ExpressionReifier<SetPropertyDefinition> {
        public SetPropertyReifier(ProcessorDefinition<?> definition) {
            super((SetPropertyDefinition) definition);
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) throws Exception {
            ObjectHelper.notNull(definition.getPropertyName(), "propertyName", this);
            Expression expr = definition.getExpression().createExpression(routeContext);
            Expression nameExpr = ExpressionBuilder.parseSimpleOrFallbackToConstantExpression(definition.getPropertyName(), routeContext.getCamelContext());
            return new SetPropertyProcessor(nameExpr, expr);
        }
    }

    private static class StopReifier extends ProcessorReifier<StopDefinition> {
        public StopReifier(ProcessorDefinition<?> definition) {
            super((StopDefinition) definition);
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) throws Exception {
            return new StopProcessor();
        }
    }

    private static class TransformReifier extends ExpressionReifier<TransformDefinition> {
        public TransformReifier(ProcessorDefinition<?> definition) {
            super((TransformDefinition) definition);
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) throws Exception {
            Expression expr = definition.getExpression().createExpression(routeContext);
            return new TransformProcessor(expr);
        }
    }

    private static class UnmarshalReifier extends ProcessorReifier<UnmarshalDefinition> {
        public UnmarshalReifier(ProcessorDefinition<?> definition) {
            super((UnmarshalDefinition) definition);
        }

        @Override
        public Processor createProcessor(RouteContext routeContext) {
            DataFormat dataFormat = DataFormatDefinition.getDataFormat(routeContext, definition.getDataFormatType(), definition.getRef());
            return new UnmarshalProcessor(dataFormat);
        }
    }

    private static class ValidateReifier extends ExpressionReifier<ValidateDefinition> {
        public ValidateReifier(ProcessorDefinition<?> definition) {
            super((ValidateDefinition) definition);
        }

        @Override
        public PredicateValidatingProcessor createProcessor(RouteContext routeContext) throws Exception {
            Predicate pred = definition.getExpression().createPredicate(routeContext);
            return new PredicateValidatingProcessor(pred);
        }
    }

    private static class WhenSkipSendToEndpointReifier extends ExpressionReifier<WhenSkipSendToEndpointDefinition> {
        public WhenSkipSendToEndpointReifier(ProcessorDefinition<?> definition) {
            super((WhenSkipSendToEndpointDefinition) definition);
        }

        @Override
        public FilterProcessor createProcessor(RouteContext routeContext) throws Exception {
            return createFilterProcessor(routeContext);
        }

        @Override
        protected Predicate createPredicate(RouteContext routeContext) {
            // we need to keep track whether the when matches or not, so delegate
            // the predicate and add the matches result as a property on the exchange
            final Predicate delegate = super.createPredicate(routeContext);
            return new Predicate() {
                @Override
                public boolean matches(Exchange exchange) {
                    boolean matches = delegate.matches(exchange);
                    exchange.setProperty(Exchange.INTERCEPT_SEND_TO_ENDPOINT_WHEN_MATCHED, matches);
                    return matches;
                }

                @Override
                public String toString() {
                    return delegate.toString();
                }
            };
        }
    }

    private static class WhenReifier extends ExpressionReifier<WhenDefinition> {
        public WhenReifier(ProcessorDefinition<?> definition) {
            super((WhenDefinition) definition);
        }

        @Override
        public FilterProcessor createProcessor(RouteContext routeContext) throws Exception {
            return createFilterProcessor(routeContext);
        }
    }
}
