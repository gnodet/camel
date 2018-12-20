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
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.camel.apigen.DocumentGenerator;
import org.apache.camel.support.IntrospectionSupport;
import org.apache.camel.support.component.ApiCollection;
import org.apache.camel.support.component.ApiMethod;
import org.apache.camel.support.component.ApiMethodHelper;
import org.apache.camel.support.component.ApiName;
import org.apache.maven.doxia.siterenderer.RenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.velocity.VelocityContext;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generates documentation for API Component.
 */
@Mojo(name = "document", requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true,
        defaultPhase = LifecyclePhase.SITE)
public class DocumentGeneratorMojo extends AbstractGeneratorMojo implements MavenReport {

    // document output directory
    @Parameter(property = PREFIX + "reportOutputDirectory", defaultValue = "${project.reporting.outputDirectory}/cameldocs")
    private File reportOutputDirectory;

    // name of destination directory
    @Parameter(property = PREFIX + "destDir", defaultValue = "cameldocs")
    private String destDir;

    /**
     * The name of the Camel report to be displayed in the Maven Generated Reports page
     * (i.e. <code>project-reports.html</code>).
     */
    @Parameter(property = "name")
    private String name;

    /**
     * The description of the Camel report to be displayed in the Maven Generated Reports page
     * (i.e. <code>project-reports.html</code>).
     */
    @Parameter(property = "description")
    private String description;

    private ApiCollection collection;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        RenderingContext context = new RenderingContext(reportOutputDirectory, getOutputName() + ".html");
        SiteRendererSink sink = new SiteRendererSink(context);
        Locale locale = Locale.getDefault();
        try {
            generate(sink, locale);
        } catch (MavenReportException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void loadApiCollection(ClassLoader classLoader) throws MavenReportException {
        try {
            final Class<?> collectionClass = classLoader.loadClass(
                    outPackage + "." + componentName + "ApiCollection");
            final Method getCollection = collectionClass.getMethod("getCollection");
            this.collection = (ApiCollection) getCollection.invoke(null);
        } catch (Exception e) {
            throw new MavenReportException(e.getMessage(), e);
        }
    }

    private VelocityContext getDocumentContext(DocumentGenerator generator) throws MavenReportException {
        final VelocityContext context = new VelocityContext();
        context.put("helper", generator);

        // project GAV
        context.put("groupId", project.getGroupId());
        context.put("artifactId", project.getArtifactId());
        context.put("version", project.getVersion());

        // component URI format
        // look for single API, no endpoint-prefix
        @SuppressWarnings("unchecked")
        final Set<String> apiNames = new TreeSet<String>(collection.getApiNames());
        context.put("apiNames", apiNames);
        String suffix;
        if (apiNames.size() == 1 && ((Set) apiNames).contains("")) {
            suffix = "://endpoint?[options]";
        } else {
            suffix = "://endpoint-prefix/endpoint?[options]";
        }
        context.put("uriFormat", scheme + suffix);

        // API helpers
        final Map<String, ApiMethodHelper> apiHelpers = new TreeMap<>();
        for (Object element : collection.getApiHelpers().entrySet()) {
            Map.Entry entry = (Map.Entry) element;
            apiHelpers.put(((ApiName) entry.getKey()).getName(), (ApiMethodHelper) entry.getValue());
        }
        context.put("apiHelpers", apiHelpers);

        // API methods and endpoint configurations
        final Map<String, Class<? extends ApiMethod>> apiMethods = new TreeMap<>();
        final Map<String, Class<?>> apiConfigs = new TreeMap<>();
        for (Object element : collection.getApiMethods().entrySet()) {
            Map.Entry entry = (Map.Entry) element;
            final String name = ((ApiName) entry.getValue()).getName();

            @SuppressWarnings("unchecked")
            Class<? extends ApiMethod> apiMethod = (Class<? extends ApiMethod>) entry.getKey();
            apiMethods.put(name, apiMethod);

            Class<?> configClass;
            try {
                configClass = generator.getProjectClassLoader().loadClass(getEndpointConfigName(apiMethod));
            } catch (ClassNotFoundException e) {
                throw new MavenReportException(e.getMessage(), e);
            }
            apiConfigs.put(name, configClass);
        }
        context.put("apiMethods", apiMethods);
        context.put("apiConfigs", apiConfigs);

        // API component properties
        context.put("scheme", this.scheme);
        context.put("componentName", this.componentName);
        Class<?> configClass;
        try {
            configClass = generator.getProjectClassLoader().loadClass(getComponentConfig());
        } catch (ClassNotFoundException e) {
            throw new MavenReportException(e.getMessage(), e);
        }
        context.put("componentConfig", configClass);
        // get declared and derived fields for component config
        // use get/set methods instead of fields, since this class could inherit others, that have private fields
        // so getDeclaredFields() won't work, like it does for generated endpoint config classes!!!
        final Map<String, String> configFields = new TreeMap<>();
        do {
            IntrospectionSupport.ClassInfo classInfo = IntrospectionSupport.cacheClass(configClass);
            for (IntrospectionSupport.MethodInfo method : classInfo.methods) {
                if (method.isSetter) {
                    configFields.put(method.getterOrSetterShorthandName, getCanonicalName(method.method.getParameterTypes()[0]));
                }
            }
            configClass = configClass.getSuperclass();
        } while (configClass != null && !configClass.equals(Object.class));
        context.put("componentConfigFields", configFields);

        return context;
    }

    private String getComponentConfig() {
        StringBuilder builder = new StringBuilder(componentPackage);
        builder.append(".").append(componentName).append("Configuration");
        return builder.toString();
    }

    private String getEndpointConfigName(Class<? extends ApiMethod> apiMethod) {
        final String simpleName = apiMethod.getSimpleName();
        StringBuilder builder = new StringBuilder(componentPackage);
        builder.append(".");
        builder.append(simpleName.substring(0, simpleName.indexOf("ApiMethod")));
        builder.append("EndpointConfiguration");
        return builder.toString();
    }

    private File getDocumentFile() {
        return new File(getReportOutputDirectory(), getDocumentName() + ".html");
    }

    private String getDocumentName() {
        return this.componentName + "Component";
    }

    @Override
    public void generate(Sink sink, Locale locale) throws MavenReportException {
        // load APICollection

        try {
            DocumentGenerator generator = new DocumentGenerator();
            loadApiCollection(generator.getProjectClassLoader());
            generator.mergeTemplate(getDocumentContext(generator), getDocumentFile().toPath(), "/api-document.vm");
        } catch (Exception e) {
            throw new MavenReportException(e.getMessage(), e);
        }
    }

    @Override
    public String getOutputName() {
        return this.destDir + "/" + getDocumentName();
    }

    @Override
    public String getCategoryName() {
        return CATEGORY_PROJECT_REPORTS;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName(Locale locale) {
        if (StringUtils.isEmpty(name)) {
            return getBundle(locale).getString("report.cameldoc.name");
        }
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getDescription(Locale locale) {
        if (StringUtils.isEmpty(description)) {
            return getBundle(locale).getString("report.cameldoc.description");
        }
        return description;
    }

    @Override
    public File getReportOutputDirectory() {
        return reportOutputDirectory;
    }

    @Override
    public void setReportOutputDirectory(File reportOutputDirectory) {
        updateReportOutputDirectory(reportOutputDirectory);
    }

    private void updateReportOutputDirectory(File reportOutputDirectory) {
        // append destDir if needed
        if (this.destDir != null && reportOutputDirectory != null
                && !reportOutputDirectory.getAbsolutePath().endsWith(destDir)) {
            this.reportOutputDirectory = new File(reportOutputDirectory, destDir);
        } else {
            this.reportOutputDirectory = reportOutputDirectory;
        }
    }

    public String getDestDir() {
        return destDir;
    }

    public void setDestDir(String destDir) {
        this.destDir = destDir;
        updateReportOutputDirectory(this.reportOutputDirectory);
    }

    @Override
    public boolean isExternalReport() {
        return true;
    }

    @Override
    public boolean canGenerateReport() {
        // TODO check for class availability??
        return true;
    }

    private ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle("cameldoc-report", locale, getClass().getClassLoader());
    }

    private static String getCanonicalName(Class<?> type) {
        // remove java.lang prefix for default Java package
        String canonicalName = type.getCanonicalName();
        final int pkgEnd = canonicalName.lastIndexOf('.');
        if (pkgEnd > 0 && canonicalName.substring(0, pkgEnd).equals("java.lang")) {
            canonicalName = canonicalName.substring(pkgEnd + 1);
        }
        return canonicalName;
    }

}
