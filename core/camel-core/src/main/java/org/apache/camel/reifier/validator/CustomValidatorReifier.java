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
package org.apache.camel.reifier.validator;

import org.apache.camel.CamelContext;
import org.apache.camel.model.validator.CustomValidatorDefinition;
import org.apache.camel.model.validator.ValidatorDefinition;
import org.apache.camel.spi.DataType;
import org.apache.camel.spi.Validator;

public class CustomValidatorReifier extends ValidatorReifier<CustomValidatorDefinition> {

    CustomValidatorReifier(ValidatorDefinition definition) {
        super((CustomValidatorDefinition) definition);
    }

    @Override
    protected Validator doCreateValidator(CamelContext context) throws Exception {
        if (definition.getValidator() == null) {
            throw new IllegalArgumentException("'validator' must be specified for customValidator");
        }
        Validator validator;
        Object value = definition.getValidator();
        if (value instanceof Validator) {
            validator = (Validator) value;
        } else if (value instanceof Class) {
            Class<?> validatorClass = (Class) value;
            if (!Validator.class.isAssignableFrom(validatorClass)) {
                throw new IllegalArgumentException("Illegal validator class: " + value);
            }
            validator = context.getInjector().newInstance((Class<Validator>) validatorClass, false);
        } else if (value instanceof String) {
            String str = (String) value;
            if (str.startsWith("#bean:")) {
                validator = resolve(context, Validator.class, str);
            } else if (str.startsWith("#class:")) {
                String name = str.substring("#class:".length());
                Class<?> validatorClass = context.getClassResolver().resolveMandatoryClass(name, Validator.class);
                if (!Validator.class.isAssignableFrom(validatorClass)) {
                    throw new IllegalArgumentException("Illegal validator class: " + value);
                }
                validator = context.getInjector().newInstance((Class<Validator>) validatorClass, false);
            } else {
                throw new IllegalArgumentException("Unsupported value: " + value);
            }
        } else {
            throw new IllegalArgumentException("Unsupported value: " + value);
        }
        if (validator.getType() != null) {
            throw new IllegalArgumentException(String.format("Validator '%s' is already in use. " +
                    "Please check if duplicate validator exists.", value));
        }
        validator.setCamelContext(context);
        return validator.setType(resolve(context, DataType.class, definition.getType()));
    }

}
