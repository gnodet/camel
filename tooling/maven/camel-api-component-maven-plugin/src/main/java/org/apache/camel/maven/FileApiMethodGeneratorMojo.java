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

import org.apache.camel.apigen.FileApiMethodGenerator;
import org.apache.camel.apigen.model.ExtraOption;
import org.apache.camel.apigen.model.Substitution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Parses ApiMethod signatures from a File.
 */
@Mojo(name = "fromFile", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresProject = true,
        defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class FileApiMethodGeneratorMojo extends AbstractSourceGeneratorMojo {

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

    @Parameter(required = true, property = PREFIX + "signatureFile")
    protected File signatureFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        FileApiMethodGenerator generator = new FileApiMethodGenerator();
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
        generator.signatureFile = signatureFile.toString();
        generator.execute();
        setCompileSourceRoots();
    }

}
