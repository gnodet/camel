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

import java.io.IOError;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.tooling.apigen.helpers.IOHelper;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGenerator {

    public interface Project {

        List<String> getClasspath();

        Path getTestSourceDirectory();

        Path getBasedir();

    }

    protected static final String PREFIX = "org.apache.camel.";
    protected static final String OUT_PACKAGE = PREFIX + "component.internal";
    protected static final String COMPONENT_PACKAGE = PREFIX + "component";

    private static VelocityEngine engine;
    private static ClassLoader projectClassLoader;

    private static boolean sharedProjectState;

    public String outPackage;

    public String scheme;

    public String componentName;

    public String componentPackage;

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    public Project project;

    public static void setSharedProjectState(boolean sharedProjectState) {
        AbstractGenerator.sharedProjectState = sharedProjectState;
    }

    public static void clearSharedProjectState() {
        if (!sharedProjectState) {
            projectClassLoader = null;
            IOHelper.closeJarFileSystems();
        }
    }

    protected static VelocityEngine getEngine() {
        if (engine == null) {
            // initialize velocity to load resources from class loader and use Log4J
            Properties velocityProperties = new Properties();
            velocityProperties.setProperty(RuntimeConstants.RESOURCE_LOADER, "cloader");
            velocityProperties.setProperty("cloader.resource.loader.class", ClasspathResourceLoader.class.getName());
            final Logger velocityLogger = LoggerFactory.getLogger("org.apache.camel.maven.Velocity");
            velocityProperties.setProperty(RuntimeConstants.RUNTIME_LOG_NAME, velocityLogger.getName());
            try {
                engine = new VelocityEngine(velocityProperties);
                engine.init();

                String v = VelocityEngine.class.getPackage().getImplementationVersion();
                if (v == null) {
                    v = VelocityEngine.class.getPackage().getSpecificationVersion();
                }
                if (!v.startsWith("2.")) {
                    ClassLoader cl = VelocityEngine.class.getClassLoader();
                    String classpath;
                    if (cl instanceof URLClassLoader) {
                        URL[] urls = ((URLClassLoader) cl).getURLs();
                        classpath = Stream.of(urls).map(URL::toString).collect(Collectors.joining(", "));
                    } else {
                        classpath = cl.getClass().getName() + "[" + cl.toString() + "]";
                    }
                    throw new IllegalStateException("Velocity 2 required, but found " + v + ", check the classpath: " + classpath);
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }

        }
        return engine;
    }

    public ClassLoader getProjectClassLoader() {
        if (projectClassLoader == null)  {
            final URL[] urls = project.getClasspath().stream()
                    .map(s -> {
                        try {
                            log.debug("Adding project path " + s);
                            return Paths.get(s).toUri().toURL();
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toArray(URL[]::new);

            final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            projectClassLoader = new URLClassLoader(urls, tccl != null ? tccl : getClass().getClassLoader());
        }
        return projectClassLoader;
    }

    public void mergeTemplate(VelocityContext context, Path outFile, String templateName) {
        // ensure parent directories exist
        final Path outDir = outFile.getParent();
        if (!Files.isDirectory(outDir)) {
            try {
                Files.createDirectories(outDir);
            } catch (IOException e) {
                throw new IOError(e);
            }
        }

        // add generated date
        context.put("generatedDate", new Date().toString());
        // add output package
        context.put("packageName", outPackage);
        context.put("newLine", "\n");

        Thread.currentThread().setContextClassLoader(null);

        // load velocity template
        Template template = null;
        try {
            template = getEngine().getTemplate(templateName, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        // generate file
        try (Writer writer = Files.newBufferedWriter(outFile)) {
            template.merge(context, writer);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String getCanonicalName(Class<?> type) {
        // remove java.lang prefix for default Java package
        String canonicalName = type.getCanonicalName();
        final int pkgEnd = canonicalName.lastIndexOf('.');
        if (pkgEnd > 0 && canonicalName.substring(0, pkgEnd).equals("java.lang")) {
            canonicalName = canonicalName.substring(pkgEnd + 1);
        }
        return canonicalName;
    }

}
