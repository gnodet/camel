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
package org.apache.camel.model.languages;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.ExpressionFactory;
import org.apache.camel.Predicate;
import org.apache.camel.model.StructDefinition;

public class ExpressionDefinition extends StructDefinition implements Expression, Predicate, ExpressionFactory {

    public ExpressionDefinition() {
    }

    public ExpressionDefinition(Expression expression) {
    }

    public void setExpression(String expression) {
        doSetProperty("expression", expression);
    }

    public String getExpression() {
        return (String) doGetProperty("expression");
    }

    @Override
    public Expression createExpression(CamelContext camelContext) {
        return null;
    }

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        return null;
    }

    @Override
    public boolean matches(Exchange exchange) {
        return false;
    }

    public Expression getExpressionValue() {
        return null;
    }

    public Predicate getPredicate() {
        return null;
    }

    public String getLabel() {
        return null;
    }
}
