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

import org.apache.camel.tooling.apigen.JavadocApiMethodGenerator;
import org.apache.camel.tooling.apigen.model.ExtraOption;
import org.apache.camel.tooling.apigen.model.Substitution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Parses ApiMethod signatures from Javadoc.
 */
@Mojo(name = "fromJavadoc", requiresDependencyResolution = ResolutionScope.TEST, requiresProject = true,
        defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class JavadocApiMethodGeneratorMojo extends AbstractSourceGeneratorMojo {

    public static final String DEFAULT_EXCLUDE_PACKAGES = "javax?\\.lang.*";

    @Parameter(property = PREFIX + "substitutions")
    protected Substitution[] substitutions = new Substitution[0];

    @Parameter(property = PREFIX + "excludeConfigNames")
    protected String excludeConfigNames;

    @Parameter(property = PREFIX + "excludeConfigTypes")
    protected String excludeConfigTypes;

    @Parameter
    protected ExtraOption[] extraOptions;

    @Parameter(required = true, property = PREFIX + "proxyClass")
    protected String proxyClass;

    @Parameter(property = PREFIX + "excludePackages", defaultValue = DEFAULT_EXCLUDE_PACKAGES)
    protected String excludePackages;

    @Parameter(property = PREFIX + "excludeClasses")
    protected String excludeClasses;

    @Parameter(property = PREFIX + "includeMethods")
    protected String includeMethods;

    @Parameter(property = PREFIX + "excludeMethods")
    protected String excludeMethods;

    @Parameter(property = PREFIX + "includeStaticMethods")
    protected Boolean includeStaticMethods;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        JavadocApiMethodGenerator generator = new JavadocApiMethodGenerator();
        generator.project = createProject();
        generator.outPackage = outPackage;
        generator.scheme = scheme;
        generator.componentName = componentName;
        generator.componentPackage = componentPackage;
        generator.generatedSrcDir = generatedSrcDir.toPath();
        generator.generatedTestDir = generatedTestDir.toPath();
        generator.substitutions = substitutions;
        generator.excludeConfigNames = excludeConfigNames;
        generator.excludeConfigTypes = excludeConfigTypes;
        generator.extraOptions = extraOptions;
        generator.proxyClass = proxyClass;
        generator.excludePackages = excludePackages;
        generator.excludeClasses = excludeClasses;
        generator.includeMethods = includeMethods;
        generator.excludeMethods = excludeMethods;
        generator.includeStaticMethods = includeStaticMethods;
        generator.execute();
        setCompileSourceRoots();
    }


}
