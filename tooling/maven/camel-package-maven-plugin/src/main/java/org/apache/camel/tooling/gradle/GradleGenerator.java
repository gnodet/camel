package org.apache.camel.tooling.gradle;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.tooling.Generator;

public class GradleGenerator extends Generator {

    Map<String, Object> properties = new HashMap<>();

    @Override
    public List<String> getRuntimeClasspathElements() {
        return (List) properties.get("project.runtimeClasspath");
    }

    @Override
    protected List<String> getCamelCoreLocations() {
        return null;
    }

    @Override
    protected List<String> getResources() {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getCompileSourceRoots() {
        return Collections.singletonList("src/main/java");
    }

    @Override
    protected String getOutputDirectory() {
        return getProperty("project.outputDirectory");
    }

    @Override
    protected String getGroupId() {
        return getProperty("project.groupId");
    }

    @Override
    protected String getArtifactId() {
        return getProperty("project.artifactId");
    }

    @Override
    protected String getVersion() {
        return getProperty("project.version");
    }

    @Override
    protected String getName() {
        return getProperty("project.name");
    }

    @Override
    protected String getDescription() {
        return getProperty("project.description");
    }

    @Override
    protected String getProperty(String key) {
        return properties.getOrDefault(key, "").toString();
    }

    @Override
    protected void addResourceDirectory(Path resourceDirectory) {

    }

    @Override
    protected void addCompileSourceRoot(String dir) {

    }

    @Override
    protected void refresh(Path file) {

    }

    @Override
    protected boolean isDebugEnabled() {
        return false;
    }

    @Override
    protected void debug(String message) {

    }

    @Override
    protected void info(String message) {

    }

    public void setGroupId(String groupId) {
        properties.put("project.groupId", groupId);
    }

    public void setArtifactId(String artifactId) {
        properties.put("project.artifactId", artifactId);
    }

    public void setVersion(String version) {
        properties.put("project.version", version);
    }

    public void setRuntimeClasspath(List<String> classpath) {
        properties.put("project.runtimeClasspath", classpath);
    }

    public void setOutputDirectory(String dir) {
        properties.put("project.outputDirectory", dir);
    }

    public void processCamelCore(File sources, File resources) {
        Path srcPath = sources.toPath();
        Path resPath = resources.toPath();
        prepareLegal(resPath);
        prepareServices(resPath);
        prepareModel(resPath);
        prepareComponent(resPath);
        processModelDoc(resPath);
        prepareDataFormat(resPath, resPath);
        prepareLanguage(resPath, resPath);
        processEndpoints(resPath);
        processJaxb(resPath);
        createConverter(srcPath);
    }

    public void processComponent(File resources) {
        Path resPath = resources.toPath();
        prepareLegal(resPath);
        prepareServices(resPath);
        prepareComponent(resPath);
        prepareDataFormat(resPath, resPath);
        prepareLanguage(resPath, resPath);
        prepareOthers(resPath, resPath);
        processEndpoints(resPath);
    }
}
