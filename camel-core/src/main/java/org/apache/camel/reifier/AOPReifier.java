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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.camel.Processor;
import org.apache.camel.model.AOPDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.processor.AOPProcessor;
import org.apache.camel.spi.RouteContext;

class AOPReifier extends ProcessorReifier<AOPDefinition> {

    AOPReifier(ProcessorDefinition<?> definition) {
        super(AOPDefinition.class.cast(definition));
    }

    @Override
    public Processor createProcessor(final RouteContext routeContext) throws Exception {
        // either before or after must be provided
        if (definition.getBeforeUri() == null && definition.getAfterUri() == null && definition.getAfterFinallyUri() == null) {
            throw new IllegalArgumentException("At least one of before, after or afterFinally must be provided on: " + this);
        }

        // use a pipeline to assemble the before and target processor
        // and the after if not afterFinally
        Collection<ProcessorDefinition<?>> pipe = new ArrayList<>();

        Processor finallyProcessor = null;

        if (definition.getBeforeUri() != null) {
            pipe.add(new ToDefinition(definition.getBeforeUri()));
        }
        pipe.addAll(definition.getOutputs());

        if (definition.getAfterUri() != null) {
            pipe.add(new ToDefinition(definition.getAfterUri()));
        } else if (definition.getAfterFinallyUri() != null) {
            finallyProcessor = createProcessor(routeContext, new ToDefinition(definition.getAfterFinallyUri()));
        }

        Processor tryProcessor = createOutputsProcessor(routeContext, pipe);

        // the AOP processor is based on TryProcessor so we do not have any catches
        return new AOPProcessor(tryProcessor, null, finallyProcessor);
    }

}
