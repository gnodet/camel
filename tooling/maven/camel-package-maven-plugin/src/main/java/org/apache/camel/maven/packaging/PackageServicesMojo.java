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
package org.apache.camel.maven.packaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Analyses the Camel plugins in a project and generates extra descriptor information for easier auto-discovery in Camel.
 */
@Mojo(name = "generate-services-list", threadSafe = true, defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class PackageServicesMojo extends AbstractMojo {

    /**
     * The maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    /**
     * The output directory for generated components file
     */
    @Parameter(defaultValue = "${project.build.directory}/generated/camel/services")
    protected File serviceOutDir;

    /**
     * Maven ProjectHelper.
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * build context to check changed files and mark them for refresh (used for
     * m2e compatibility)
     */
    @Component
    private BuildContext buildContext;

    /**
     * Execute goal.
     *
     * @throws MojoExecutionException execution of the main class or one of the
     *                 threads it generated failed.
     * @throws MojoFailureException something bad happened...
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        prepareServices(getLog(), project, projectHelper, serviceOutDir, buildContext);
    }

    public static void prepareServices(Log log, MavenProject project, MavenProjectHelper projectHelper, File serviceOutDir, BuildContext buildContext) throws MojoExecutionException {

        System.err.println("Indexing...");
        System.err.println("Path: " + project.getBuild().getOutputDirectory());
        Index index;
        try {
            Indexer indexer = new Indexer();
            Files.walk(Paths.get(project.getBuild().getOutputDirectory()))
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .forEach(p -> {
                        try (InputStream is = Files.newInputStream(p)) {
                            indexer.index(is);
                        } catch (IOException e) {
                            throw new IOError(e);
                        }
                    });
            index = indexer.complete();
        } catch (IOException | IOError e) {
            throw new MojoExecutionException("Error", e);
        }

        System.err.println("Generating...");
        Path camelMetaDir = serviceOutDir.toPath().resolve("META-INF/services/org/apache/camel/");
        Stream.of("Component", "Language", "Dataformat")
                .map(s -> DotName.createSimple("org.apache.camel.spi.annotations." + s))
                .flatMap(n -> index.getAnnotations(n).stream())
                .forEach(ai -> {
                    String name = ai.value().asString();
                    String clazz = ai.target().asClass().name().toString();
                    Path dir = camelMetaDir.resolve(ai.name().local().toLowerCase());
                    try {
                        Files.createDirectories(dir);
                        try (Writer writer = Files.newBufferedWriter(dir.resolve(name))) {
                            writer.write("# Generated by camel annotation processor\n");
                            writer.write("class=" + clazz + "\n");
                        }
                    } catch (IOException e) {
                        throw new IOError(e);
                    }
                });

        projectHelper.addResource(project, serviceOutDir.getPath(), Collections.singletonList("META-INF/services/org/apache/camel/**/*"), Collections.emptyList());
    }

}
