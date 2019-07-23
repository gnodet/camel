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

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.camel.NoSuchBeanException;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.Service;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.TransactedDefinition;
import org.apache.camel.processor.WrapProcessor;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.spi.TransactedPolicy;

public class TransactedReifier<Type extends ProcessorDefinition<Type>> extends ProcessorReifier<TransactedDefinition<Type>> {

    public static final String PROPAGATION_REQUIRED = "PROPAGATION_REQUIRED";

    @SuppressWarnings("unchecked")
    TransactedReifier(ProcessorDefinition<?> definition) {
        super((TransactedDefinition) definition);
    }

    @Override
    public Processor createProcessor(RouteContext routeContext) throws Exception {
        Class<?> type = definition.getType() != null ? asClass(routeContext, definition.getType())
                                                     : TransactedPolicy.class;
        if (!TransactedPolicy.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Policy type does not inherit from " + TransactedPolicy.class.getName());
        }
        TransactedPolicy policy = resolvePolicy(routeContext, (Class<? extends TransactedPolicy>) type, definition.getInstance());
        org.apache.camel.util.ObjectHelper.notNull(policy, "policy", this);

        // before wrap
        policy.beforeWrap(routeContext, definition);

        // create processor after the before wrap
        Processor childProcessor = this.createChildProcessor(routeContext, true);

        // wrap
        Processor target = policy.wrap(routeContext, childProcessor);

        if (!(target instanceof Service)) {
            // wrap the target so it becomes a service and we can manage its lifecycle
            target = new WrapProcessor(target, childProcessor);
        }
        return target;
    }

    protected <T extends TransactedPolicy> T resolveTransactedPolicy(RouteContext routeContext, Class<T> type, Object value) {
        T answer = resolvePolicy(routeContext, type, value);

        // for transacted routing try the default REQUIRED name
        if (answer == null) {
            // still not found try with the default name PROPAGATION_REQUIRED
            answer = routeContext.lookup(PROPAGATION_REQUIRED, type);
        }

        if (answer == null) {
            // this logic only applies if we are a transacted policy
            // still no policy found then try lookup the platform transaction manager and use it as policy
            Class<?> tmClazz = routeContext.getCamelContext().getClassResolver().resolveClass("org.springframework.transaction.PlatformTransactionManager");
            if (tmClazz != null) {
                // see if we can find the platform transaction manager in the registry
                Map<String, ?> maps = routeContext.lookupByType(tmClazz);
                if (maps.size() == 1) {
                    // only one platform manager then use it as default and create a transacted
                    // policy with it and default to required

                    // as we do not want dependency on spring jars in the camel-core we use
                    // reflection to lookup classes and create new objects and call methods
                    // as this is only done during route building it does not matter that we
                    // use reflection as performance is no a concern during route building
                    Object transactionManager = maps.values().iterator().next();
                    log.debug("One instance of PlatformTransactionManager found in registry: {}", transactionManager);
                    Class<?> txClazz = routeContext.getCamelContext().getClassResolver().resolveClass("org.apache.camel.spring.spi.SpringTransactionPolicy");
                    if (txClazz != null) {
                        log.debug("Creating a new temporary SpringTransactionPolicy using the PlatformTransactionManager: {}", transactionManager);
                        TransactedPolicy txPolicy = org.apache.camel.support.ObjectHelper.newInstance(txClazz, TransactedPolicy.class);
                        Method method;
                        try {
                            method = txClazz.getMethod("setTransactionManager", tmClazz);
                        } catch (NoSuchMethodException e) {
                            throw new RuntimeCamelException("Cannot get method setTransactionManager(PlatformTransactionManager) on class: " + txClazz);
                        }
                        org.apache.camel.support.ObjectHelper.invokeMethod(method, txPolicy, transactionManager);
                        return type.cast(txPolicy);
                    } else {
                        // camel-spring is missing on the classpath
                        throw new RuntimeCamelException("Cannot create a transacted policy as camel-spring.jar is not on the classpath!");
                    }
                } else {
                    if (maps.isEmpty()) {
                        throw new NoSuchBeanException(null, "PlatformTransactionManager");
                    } else {
                        throw new IllegalArgumentException("Found " + maps.size() + " PlatformTransactionManager in registry. "
                                + "Cannot determine which one to use. Please configure a TransactionTemplate on the transacted policy.");
                    }
                }
            }
        }

        return answer;
    }

}
