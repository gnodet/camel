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

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.camel.tooling.apigen.model.ApiMethodAlias;
import org.apache.camel.tooling.apigen.model.ApiProxy;
import org.apache.camel.tooling.apigen.model.ExtraOption;
import org.apache.camel.tooling.apigen.model.FromJavadoc;
import org.apache.camel.tooling.apigen.model.Substitution;
import org.apache.velocity.VelocityContext;
import org.json.simple.JsonObject;

public class ApiComponentGenerator extends AbstractApiMethodBaseGenerator {

    public ApiProxy[] apis;

    /**
     * Common Javadoc code generation settings.
     */
    public FromJavadoc fromJavadoc;

    /**
     * Names of options that can be set to null value if not specified.
     */
    public String[] nullableOptions;

    /**
     * Method alias patterns for all APIs.
     */
    public List<ApiMethodAlias> aliases;

    public static ApiComponentGenerator toGenerator(JsonObject json) {
        ApiComponentGenerator generator = new ApiComponentGenerator();
        generator.substitutions = json.getCollectionOrDefault("substitutions", Collections.<JsonObject>emptyList())
                .stream().map(ApiComponentGenerator::toSubstitution).toArray(Substitution[]::new);
        generator.excludeConfigNames = json.getString("excludeConfigNames");
        generator.excludeConfigTypes = json.getString("excludeConfigTypes");
        generator.extraOptions = json.getCollectionOrDefault("extraOptions", Collections.<JsonObject>emptyList())
                .stream().map(ApiComponentGenerator::toExtraOption).toArray(ExtraOption[]::new);
        generator.apis = json.getCollectionOrDefault("apis", Collections.<JsonObject>emptyList())
                .stream().map(ApiComponentGenerator::toApi).toArray(ApiProxy[]::new);
        generator.fromJavadoc = Optional.ofNullable(json.<JsonObject>getMap("fromJavadoc"))
                .map(ApiComponentGenerator::toFromJavadoc)
                .orElseGet(FromJavadoc::new);
        generator.nullableOptions = json.getCollectionOrDefault("nullableOptions", Collections.<String>emptyList())
                .stream().map(Object::toString).toArray(String[]::new);
        generator.aliases = json.getCollectionOrDefault("aliases", Collections.<JsonObject>emptyList())
                .stream().map(ApiComponentGenerator::toApiMethodAlias).collect(Collectors.toList());
        return generator;
    }

    public static ApiProxy toApi(JsonObject json) {
        ApiProxy api = new ApiProxy();
        api.setApiName(json.getString("apiName"));
        api.setProxyClass(json.getString("proxyClass"));
        api.setFromSignatureFile(Optional.ofNullable(json.getString("fromSignatureFile"))
                .orElse(null));
        api.setFromJavadoc(Optional.ofNullable(json.<JsonObject>getMap("fromJavadoc")).map(ApiComponentGenerator::toFromJavadoc).orElseGet(FromJavadoc::new));
        api.setSubstitutions(json.getCollectionOrDefault("substitutions", Collections.<JsonObject>emptyList())
                .stream().map(ApiComponentGenerator::toSubstitution).toArray(Substitution[]::new));
        api.setExcludeConfigNames(json.getString("excludeConfigNames"));
        api.setExcludeConfigTypes(json.getString("excludeConfigTypes"));
        api.setExtraOptions(json.getCollectionOrDefault("extraOptions", Collections.<JsonObject>emptyList())
                .stream().map(ApiComponentGenerator::toExtraOption).toArray(ExtraOption[]::new));
        api.setNullableOptions(json.getCollectionOrDefault("nullableOptions", Collections.<String>emptyList())
                .stream().map(Object::toString).toArray(String[]::new));
        api.setAliases(json.getCollectionOrDefault("aliases", Collections.<JsonObject>emptyList())
                .stream().map(ApiComponentGenerator::toApiMethodAlias).collect(Collectors.toList()));
        return api;
    }

    public static Substitution toSubstitution(JsonObject json) {
        return new Substitution(
                json.getString("method"),
                json.getString("argName"),
                json.getString("argType"),
                json.getString("replacement"),
                json.getBooleanOrDefault("replaceWithType", false)
        );
    }

    public static ExtraOption toExtraOption(JsonObject json) {
        return new ExtraOption(
                json.getString("type"),
                json.getString("name")
        );
    }

    public static ApiMethodAlias toApiMethodAlias(JsonObject json) {
        return new ApiMethodAlias(
                json.getString("methodPattern"),
                json.getString("methodAlias")
        );
    }

    public static FromJavadoc toFromJavadoc(JsonObject json) {
        return new FromJavadoc(
                json.getString("excludePackages"),
                json.getString("excludeClasses"),
                json.getString("includeMethods"),
                json.getString("excludeMethods"),
                json.getBoolean("includeStaticMethods")
        );
    }

    public void execute() {
        if (apis == null || apis.length == 0) {
            throw new RuntimeException("One or more API proxies are required");
        }

        // starting with a new project
        clearSharedProjectState();
        setSharedProjectState(true);

        try {
            // fix apiName for single API use-case since Maven configurator sets empty parameters as null!!!
            if (apis.length == 1 && apis[0].getApiName() == null) {
                apis[0].setApiName("");
            }

            // generate API methods for each API proxy
            for (ApiProxy api : apis) {
                // validate API configuration
                api.validate();

                // create the appropriate code generator if signatureFile or fromJavaDoc are specified
                // this way users can skip generating API classes for duplicate proxy class references
                final AbstractApiMethodGenerator apiMethodGenerator = getApiMethodGenerator(api);

                if (apiMethodGenerator != null) {
                    // configure API method properties and generate Proxy classes
                    configureMethodGenerator(apiMethodGenerator, api);
                    try {
                        apiMethodGenerator.execute();
                    } catch (Exception e) {
                        final String msg = "Error generating source for " + api.getProxyClass() + ": " + e.toString();
                        throw new RuntimeException(msg, e);
                    }
                } else {
                    // make sure the proxy class is being generated elsewhere
                    final String proxyClass = api.getProxyClass();
                    boolean found = false;
                    for (ApiProxy other : apis) {
                        if (other != api && proxyClass.equals(other.getProxyClass())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new RuntimeException("Missing one of fromSignatureFile or fromJavadoc for "
                                + proxyClass);
                    }
                }

                // set common aliases if needed
                if (!aliases.isEmpty() && api.getAliases().isEmpty()) {
                    api.setAliases(aliases);
                }

                // set common nullable options if needed
                if (api.getNullableOptions() == null) {
                    api.setNullableOptions(nullableOptions);
                }
            }

            // generate ApiCollection
            mergeTemplate(getApiContext(), getApiCollectionFile(), "/api-collection.vm");

            // generate ApiName
            mergeTemplate(getApiContext(), getApiNameFile(), "/api-name-enum.vm");

        } finally {
            // clear state for next Mojo
            setSharedProjectState(false);
            clearSharedProjectState();
        }
    }

    private void configureMethodGenerator(AbstractApiMethodGenerator mojo, ApiProxy apiProxy) {

        // set AbstractGeneratorMojo properties
        mojo.componentName = componentName;
        mojo.scheme = scheme;
        mojo.outPackage = outPackage;
        mojo.componentPackage = componentPackage;

        // set AbstractSourceGeneratorMojo properties
        mojo.generatedSrcDir = generatedSrcDir;
        mojo.generatedTestDir = generatedTestDir;

        // set AbstractAPIMethodBaseMojo properties
        mojo.substitutions = apiProxy.getSubstitutions().length != 0
                ? apiProxy.getSubstitutions() : substitutions;
        mojo.excludeConfigNames = apiProxy.getExcludeConfigNames() != null
                ? apiProxy.getExcludeConfigNames() : excludeConfigNames;
        mojo.excludeConfigTypes = apiProxy.getExcludeConfigTypes() != null
                ? apiProxy.getExcludeConfigTypes() : excludeConfigTypes;
        mojo.extraOptions = apiProxy.getExtraOptions() != null
                ? apiProxy.getExtraOptions() : extraOptions;

        // set AbstractAPIMethodGeneratorMojo properties
        mojo.proxyClass = apiProxy.getProxyClass();
    }

    private AbstractApiMethodGenerator getApiMethodGenerator(ApiProxy api) {
        AbstractApiMethodGenerator apiMethodGenerator = null;

        final String signatureFile = api.getFromSignatureFile();
        if (signatureFile != null) {

            final FileApiMethodGenerator fileMojo = new FileApiMethodGenerator();
            fileMojo.signatureFile = signatureFile;
            apiMethodGenerator = fileMojo;

        } else {

            final FromJavadoc apiFromJavadoc = api.getFromJavadoc();
            if (apiFromJavadoc != null) {
                final JavadocApiMethodGenerator javadocMojo = new JavadocApiMethodGenerator();
                javadocMojo.excludePackages = apiFromJavadoc.getExcludePackages() != null
                        ? apiFromJavadoc.getExcludePackages() : fromJavadoc.getExcludePackages();
                javadocMojo.excludeClasses = apiFromJavadoc.getExcludeClasses() != null
                        ? apiFromJavadoc.getExcludeClasses() : fromJavadoc.getExcludeClasses();
                javadocMojo.includeMethods = apiFromJavadoc.getIncludeMethods() != null
                        ? apiFromJavadoc.getIncludeMethods() : fromJavadoc.getIncludeMethods();
                javadocMojo.excludeMethods = apiFromJavadoc.getExcludeMethods() != null
                        ? apiFromJavadoc.getExcludeMethods() : fromJavadoc.getExcludeMethods();
                javadocMojo.includeStaticMethods = apiFromJavadoc.getIncludeStaticMethods() != null
                        ? apiFromJavadoc.getIncludeStaticMethods() : fromJavadoc.getIncludeStaticMethods();

                apiMethodGenerator = javadocMojo;
            }
        }
        apiMethodGenerator.project = project;
        apiMethodGenerator.scheme = scheme;
        apiMethodGenerator.outPackage = outPackage;
        apiMethodGenerator.componentName = componentName;
        apiMethodGenerator.componentPackage = componentPackage;
        return apiMethodGenerator;
    }

    private VelocityContext getApiContext() {
        final VelocityContext context = new VelocityContext();
        context.put("componentName", componentName);
        context.put("componentPackage", componentPackage);
        context.put("apis", apis);
        context.put("helper", getClass());
        context.put("collectionName", getApiCollectionName());
        context.put("apiNameEnum", getApiNameEnum());
        return context;
    }

    private String getApiCollectionName() {
        return componentName + "ApiCollection";
    }

    private String getApiNameEnum() {
        return componentName + "ApiName";
    }

    private Path getApiCollectionFile() {
        String fileName = outPackage.replaceAll("\\.", Matcher.quoteReplacement(File.separator))
                + File.separator + getApiCollectionName() + ".java";
        return generatedSrcDir.resolve(fileName);
    }

    private Path getApiNameFile() {
        String fileName = outPackage.replaceAll("\\.", Matcher.quoteReplacement(File.separator))
                + File.separator + getApiNameEnum() + ".java";
        return generatedSrcDir.resolve(fileName);
    }

    public static String getApiMethod(String proxyClass) {
        String proxyClassWithCanonicalName = getProxyClassWithCanonicalName(proxyClass);
        return proxyClassWithCanonicalName.substring(proxyClassWithCanonicalName.lastIndexOf('.') + 1) + "ApiMethod";
    }

    public static String getEndpointConfig(String proxyClass) {
        String proxyClassWithCanonicalName = getProxyClassWithCanonicalName(proxyClass);
        return proxyClassWithCanonicalName.substring(proxyClassWithCanonicalName.lastIndexOf('.') + 1) + "EndpointConfiguration";
    }

    private static String getProxyClassWithCanonicalName(String proxyClass) {
        return proxyClass.replace("$", "");
    }

    public static String getEnumConstant(String enumValue) {
        if (enumValue == null || enumValue.isEmpty()) {
            return "DEFAULT";
        }
        StringBuilder builder = new StringBuilder();
        if (!Character.isJavaIdentifierStart(enumValue.charAt(0))) {
            builder.append('_');
        }
        for (char c : enumValue.toCharArray()) {
            char upperCase = Character.toUpperCase(c);
            if (!Character.isJavaIdentifierPart(upperCase)) {
                builder.append('_');
            } else {
                builder.append(upperCase);
            }
        }
        return builder.toString();
    }

    public static String getNullableOptionValues(String[] nullableOptions) {

        if (nullableOptions == null || nullableOptions.length == 0) {
            return "";
        }

        final StringBuilder builder = new StringBuilder();
        final int nOptions = nullableOptions.length;
        int i = 0;
        for (String option : nullableOptions) {
            builder.append('"').append(option).append('"');
            if (++i < nOptions) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }
}
