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
package org.apache.camel.maven.packaging.ok;

import java.io.File;

import org.apache.camel.tooling.Project;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Prepares a Camel component analyzing if the maven module contains Camel
 * <ul>
 *     <li>components</li>
 *     <li>dataformats</li>
 *     <li>languages</li>
 *     <li>others</li>
 * </ul>
 * And for each of those generates extra descriptors and schema files for easier auto-discovery in Camel and tooling.
 */
@Mojo(name = "prepare-components", threadSafe = true, defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class PrepareComponentMojo extends AbstractMojo {

    /**
     * The maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    /**
     * The output directory for generated components file
     */
    @Parameter(defaultValue = "${project.build.directory}/generated/camel/endpoints")
    protected File endpointsOutDir;

    /**
     * The output directory for generated service file
     *
     */
    @Parameter(defaultValue = "${project.build.directory}/generated/camel/services")
    protected File serviceOutDir;

    /**
     * The output directory for generated components file
     *
     */
    @Parameter(defaultValue = "${project.build.directory}/generated/camel/components")
    protected File componentOutDir;

    /**
     * The output directory for generated dataformats file
     *
     */
    @Parameter(defaultValue = "${project.build.directory}/generated/camel/dataformats")
    protected File dataFormatOutDir;

    /**
     * The output directory for generated languages file
     *
     */
    @Parameter(defaultValue = "${project.build.directory}/generated/camel/languages")
    protected File languageOutDir;

    /**
     * The output directory for generated others file
     *
     */
    @Parameter(defaultValue = "${project.build.directory}/generated/camel/others")
    protected File otherOutDir;

    /**
     * The output directory for generated schema file
     *
     */
    @Parameter(defaultValue = "${project.build.directory}/generated/camel/schemas")
    protected File schemaOutDir;

    /**
     * The output directory for generated schema file
     *
     */
    @Parameter(defaultValue = "${project.build.directory}/generated/camel/legal")
    protected File legalOutDir;

    /**
     * Maven ProjectHelper.
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * build context to check changed files and mark them for refresh
     * (used for m2e compatibility)
     */
    @Component
    private BuildContext buildContext;

    /**
     * Execute goal.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException execution of the main class or one of the
     *                                                        threads it generated failed.
     * @throws org.apache.maven.plugin.MojoFailureException   something bad happened...
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        Project build = Project.project(getLog(), project, buildContext);
        build.prepareLegal(legalOutDir.toPath());
        build.prepareServices(serviceOutDir.toPath());
        build.prepareComponent(componentOutDir.toPath());
        build.prepareDataFormat(dataFormatOutDir.toPath(), schemaOutDir.toPath());
        build.prepareLanguage(languageOutDir.toPath(), schemaOutDir.toPath());
        build.prepareOthers(otherOutDir.toPath(), schemaOutDir.toPath());
        build.processEndpoints(endpointsOutDir.toPath());
    }

}
