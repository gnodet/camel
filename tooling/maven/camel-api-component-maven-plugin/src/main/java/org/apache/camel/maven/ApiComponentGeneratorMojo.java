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
package org.apache.camel.maven;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.camel.apigen.ApiComponentGenerator;
import org.apache.camel.apigen.model.ApiMethodAlias;
import org.apache.camel.apigen.model.ApiProxy;
import org.apache.camel.apigen.model.ExtraOption;
import org.apache.camel.apigen.model.FromJavadoc;
import org.apache.camel.apigen.model.Substitution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;

/**
 * Generates Camel Component based on a collection of APIs.
 */
@Mojo(name = "fromApis", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresProject = true,
        defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ApiComponentGeneratorMojo extends AbstractSourceGeneratorMojo {

    @Parameter(defaultValue = "src/main/api/")
    protected File input;

    @Parameter(property = PREFIX + "substitutions")
    protected Substitution[] substitutions = new Substitution[0];

    @Parameter(property = PREFIX + "excludeConfigNames")
    protected String excludeConfigNames;

    @Parameter(property = PREFIX + "excludeConfigTypes")
    protected String excludeConfigTypes;

    @Parameter
    protected ExtraOption[] extraOptions;

    /**
     * List of API names, proxies and code generation settings.
     */
    @Parameter
    protected ApiProxy[] apis;

    /**
     * Common Javadoc code generation settings.
     */
    @Parameter
    protected FromJavadoc fromJavadoc = new FromJavadoc();

    /**
     * Names of options that can be set to null value if not specified.
     */
    @Parameter
    private String[] nullableOptions;

    /**
     * Method alias patterns for all APIs.
     */
    @Parameter
    private List<ApiMethodAlias> aliases = Collections.emptyList();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Stream<ApiComponentGenerator> generators;
        if (apis != null) {
            ApiComponentGenerator generator = new ApiComponentGenerator();
            generator.substitutions = substitutions;
            generator.excludeConfigNames = excludeConfigNames;
            generator.excludeConfigTypes = excludeConfigTypes;
            generator.extraOptions = extraOptions;
            generator.apis = apis;
            generator.fromJavadoc = fromJavadoc;
            generator.nullableOptions = nullableOptions;
            generator.aliases = aliases;
            generators = Stream.of(generator);
        } else if (input != null) {
            Path in = input.toPath();
            Stream<Path> s;
            if (Files.isDirectory(in)) {
                s = list(in).filter(p -> p.getFileName().toString().endsWith(".json"));
            } else if (Files.isRegularFile(in)) {
                s = Stream.of(in);
            } else {
                s = Stream.empty();
            }
            generators = s.map(this::toJson)
                    .map(ApiComponentGenerator::toGenerator);
        } else {
            generators = Stream.empty();
        }
        generators.forEach(generator -> {
            generator.project = createProject();
            generator.outPackage = outPackage;
            generator.scheme = scheme;
            generator.componentName = componentName;
            generator.componentPackage = componentPackage;
            generator.generatedSrcDir = generatedSrcDir.toPath();
            generator.generatedTestDir = generatedTestDir.toPath();
            generator.execute();
        });
        setCompileSourceRoots();
    }

    private JsonObject toJson(Path p) {
        try (Reader r = Files.newBufferedReader(p)) {
            return (JsonObject) Jsoner.deserialize(r);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Stream<Path> list(Path p) {
        try {
            return Files.list(p);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

}
