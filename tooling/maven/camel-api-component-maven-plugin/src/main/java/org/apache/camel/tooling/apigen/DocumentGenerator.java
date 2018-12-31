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
package org.apache.camel.tooling.apigen;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.camel.support.component.ApiMethod;
import org.apache.camel.support.component.ApiMethodHelper;
import org.apache.commons.lang.ClassUtils;

public class DocumentGenerator extends AbstractGenerator {

    public static List<EndpointInfo> getEndpoints(Class<? extends ApiMethod> apiMethod,
                                                  ApiMethodHelper<?> helper, Class<?> endpointConfig) {
        // get list of valid options
        final Set<String> validOptions = new HashSet<>();
        for (Field field : endpointConfig.getDeclaredFields()) {
            validOptions.add(field.getName());
        }

        // create method name map
        final Map<String, List<ApiMethod>> methodMap = new TreeMap<>();
        for (ApiMethod method : apiMethod.getEnumConstants()) {
            String methodName = method.getName();
            List<ApiMethod> apiMethods = methodMap.get(methodName);
            if (apiMethods == null) {
                apiMethods = new ArrayList<>();
                methodMap.put(methodName, apiMethods);
            }
            apiMethods.add(method);
        }

        // create method name to alias name map
        final Map<String, Set<String>> aliasMap = new TreeMap<>();
        final Map<String, Set<String>> aliasToMethodMap = helper.getAliases();
        for (Map.Entry<String, Set<String>> entry : aliasToMethodMap.entrySet()) {
            final String alias = entry.getKey();
            for (String method : entry.getValue()) {
                Set<String> aliases = aliasMap.get(method);
                if (aliases == null) {
                    aliases = new TreeSet<>();
                    aliasMap.put(method, aliases);
                }
                aliases.add(alias);
            }
        }

        // create options map and return type map
        final Map<String, Set<String>> optionMap = new TreeMap<>();
        final Map<String, Set<String>> returnType = new TreeMap<>();
        for (Map.Entry<String, List<ApiMethod>> entry : methodMap.entrySet()) {
            final String name = entry.getKey();
            final List<ApiMethod> apiMethods = entry.getValue();

            // count the number of times, every valid option shows up across methods
            // and also collect return types
            final Map<String, Integer> optionCount = new TreeMap<>();
            final TreeSet<String> resultTypes = new TreeSet<>();
            returnType.put(name, resultTypes);

            for (ApiMethod method : apiMethods) {
                for (String arg : method.getArgNames()) {

                    if (validOptions.contains(arg)) {
                        Integer count = optionCount.get(arg);
                        if (count == null) {
                            count = 1;
                        } else {
                            count += 1;
                        }
                        optionCount.put(arg, count);
                    }
                }

                // wrap primitive result types
                Class<?> resultType = method.getResultType();
                if (resultType.isPrimitive()) {
                    resultType = ClassUtils.primitiveToWrapper(resultType);
                }
                resultTypes.add(getCanonicalName(resultType));
            }

            // collect method options
            final TreeSet<String> options = new TreeSet<>();
            optionMap.put(name, options);
            final Set<String> mandatory = new TreeSet<>();

            // generate optional and mandatory lists for overloaded methods
            int nMethods = apiMethods.size();
            for (ApiMethod method : apiMethods) {
                final Set<String> optional = new TreeSet<>();

                for (String arg : method.getArgNames()) {
                    if (validOptions.contains(arg)) {

                        final Integer count = optionCount.get(arg);
                        if (count == nMethods) {
                            mandatory.add(arg);
                        } else {
                            optional.add(arg);
                        }
                    }
                }

                if (!optional.isEmpty()) {
                    options.add(optional.toString());
                }
            }

            if (!mandatory.isEmpty()) {
                // strip [] from mandatory options
                final String mandatoryOptions = mandatory.toString();
                options.add(mandatoryOptions.substring(1, mandatoryOptions.length() - 1));
            }
        }

        // create endpoint data
        final List<EndpointInfo> infos = new ArrayList<>();
        for (Map.Entry<String, List<ApiMethod>> methodEntry : methodMap.entrySet()) {
            final String endpoint = methodEntry.getKey();

            // set endpoint name
            EndpointInfo info = new EndpointInfo();
            info.endpoint = endpoint;
            info.aliases = convertSetToString(aliasMap.get(endpoint));
            info.options = convertSetToString(optionMap.get(endpoint));
            final Set<String> resultTypes = returnType.get(endpoint);
            // get rid of void results
            resultTypes.remove("void");
            info.resultTypes = convertSetToString(resultTypes);

            infos.add(info);
        }

        return infos;
    }

    private static String convertSetToString(Set<String> values) {
        if (values != null && !values.isEmpty()) {
            final String result = values.toString();
            return result.substring(1, result.length() - 1);
        } else {
            return "";
        }
    }

    public static String getCanonicalName(Field field) {
        final Type fieldType = field.getGenericType();
        if (fieldType instanceof ParameterizedType) {
            return getCanonicalName((ParameterizedType) fieldType);
        } else {
            return getCanonicalName(field.getType());
        }
    }

    private static String getCanonicalName(ParameterizedType fieldType) {
        final Type[] typeArguments = fieldType.getActualTypeArguments();

        final int nArguments = typeArguments.length;
        if (nArguments > 0) {
            final StringBuilder result = new StringBuilder(getCanonicalName((Class<?>) fieldType.getRawType()));
            result.append("&lt;");
            int i = 0;
            for (Type typeArg : typeArguments) {
                if (typeArg instanceof ParameterizedType) {
                    result.append(getCanonicalName((ParameterizedType) typeArg));
                } else {
                    result.append(getCanonicalName((Class<?>) typeArg));
                }
                if (++i < nArguments) {
                    result.append(',');
                }
            }
            result.append("&gt;");
            return result.toString();
        }

        return getCanonicalName((Class<?>) fieldType.getRawType());
    }

    public static class EndpointInfo {
        private String endpoint;
        private String aliases;
        private String options;
        private String resultTypes;

        public String getEndpoint() {
            return endpoint;
        }

        public String getAliases() {
            return aliases;
        }

        public String getOptions() {
            return options;
        }

        public String getResultTypes() {
            return resultTypes;
        }
    }

}
