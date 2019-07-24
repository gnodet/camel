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

import java.util.Comparator;

import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.SortDefinition;
import org.apache.camel.processor.SortProcessor;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.support.ObjectHelper;

import static org.apache.camel.builder.ExpressionBuilder.bodyExpression;

public class SortReifier<T, Type extends ProcessorDefinition<Type>> extends ExpressionReifier<SortDefinition<T, Type>> {

    @SuppressWarnings("unchecked")
    SortReifier(ProcessorDefinition<?> definition) {
        super((SortDefinition) definition);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Processor createProcessor(RouteContext routeContext) throws Exception {
        Comparator<T> comparator;
        if (definition.getComparator() != null) {
            // lookup in registry
            comparator = resolve(routeContext, Comparator.class, definition.getComparator());
        } else {
            // if no comparator then default on to string representation
            comparator = ObjectHelper::compare;
        }

        // if no expression provided then default to body expression
        Expression exp;
        if (definition.getExpression() != null) {
            exp = definition.getExpression().createExpression(routeContext);
        } else {
            exp = bodyExpression();
        }
        return new SortProcessor<>(exp, comparator);
    }

}
