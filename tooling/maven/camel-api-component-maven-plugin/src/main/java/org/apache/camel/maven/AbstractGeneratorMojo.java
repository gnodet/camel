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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.camel.apigen.AbstractGenerator;
import org.apache.camel.apigen.AbstractGenerator.Project;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Base class for API based generation MOJOs.
 */
public abstract class AbstractGeneratorMojo extends AbstractMojo {

    protected static final String PREFIX = "org.apache.camel.";
    protected static final String OUT_PACKAGE = PREFIX + "component.internal";
    protected static final String COMPONENT_PACKAGE = PREFIX + "component";

    @Parameter(defaultValue = OUT_PACKAGE)
    protected String outPackage;

    @Parameter(required = true, property = PREFIX + "scheme")
    protected String scheme;

    @Parameter(required = true, property = PREFIX + "componentName")
    protected String componentName;

    @Parameter(required = true, defaultValue = COMPONENT_PACKAGE)
    protected String componentPackage;

    @Parameter(required = true, defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    protected AbstractGeneratorMojo() {
        AbstractGenerator.clearSharedProjectState();
    }

    protected AbstractGenerator.Project createProject() {
        return new Project() {
            @Override
            public List<String> getClasspathElements() {
                try {
                    return project.getTestClasspathElements();
                } catch (DependencyResolutionRequiredException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Path getTestSourceDirectory() {
                return Paths.get(project.getBuild().getTestSourceDirectory());
            }

            @Override
            public Path getBasedir() {
                return project.getBasedir().toPath();
            }
        };
    }

}
