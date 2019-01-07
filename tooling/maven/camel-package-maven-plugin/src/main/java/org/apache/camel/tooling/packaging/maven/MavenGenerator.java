package org.apache.camel.tooling.packaging.maven;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.tooling.packaging.Generator;
import org.apache.maven.model.Resource;

public class MavenGenerator extends Generator {

    private final org.apache.maven.project.MavenProject project;
    private final org.apache.maven.plugin.logging.Log log;
    private final org.sonatype.plexus.build.incremental.BuildContext buildContext;

    public static Generator generator(org.apache.maven.project.MavenProject project,
                                      org.apache.maven.plugin.logging.Log log,
                                      org.sonatype.plexus.build.incremental.BuildContext buildContext) {
        return new MavenGenerator(project, log, buildContext);
    }

    public MavenGenerator(org.apache.maven.project.MavenProject project,
                          org.apache.maven.plugin.logging.Log log,
                          org.sonatype.plexus.build.incremental.BuildContext buildContext) {
        this.project = project;
        this.log = log;
        this.buildContext = buildContext;
    }

    @Override
    public String getGroupId() {
        return project.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return project.getArtifactId();
    }

    @Override
    public String getVersion() {
        return project.getVersion();
    }

    @Override
    public String getName() {
        return project.getName();
    }

    @Override
    public String getDescription() {
        return project.getDescription();
    }

    @Override
    public String getProperty(String key) {
        return project.getProperties().getProperty(key);
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public void debug(String message) {
        log.debug(message);
    }

    @Override
    public void info(String message) {
        log.info(message);
    }

    @Override
    public List<String> getResources() {
        return project.getResources().stream().map(Resource::getDirectory).collect(Collectors.toList());
    }

    @Override
    public List<String> getCompileSourceRoots() {
        return project.getCompileSourceRoots();
    }

    @Override
    public List<String> getClasspath() {
        try {
            return project.getRuntimeClasspathElements();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getCamelCoreLocations() {
        if ("camel-core".equals(getArtifactId())) {
            return getResources();
        } else {
            return getClasspath();
        }
    }

    @Override
    public String getOutputDirectory() {
        return project.getBuild().getOutputDirectory();
    }

    @Override
    public void addResourceDirectory(Path resourceDirectory) {
        if (Files.isDirectory(resourceDirectory)) {
            boolean exists = project.getResources().stream()
                    .map(Resource::getDirectory)
                    .anyMatch(r -> resourceDirectory.toString().equals(r));
            if (!exists) {
                Resource resource = new Resource();
                resource.setDirectory(resourceDirectory.toString());
                resource.setIncludes(Collections.singletonList("**/*"));
                resource.setExcludes(Collections.emptyList());
                resource.setFiltering(false);
                project.addResource(resource);
            }
        }
    }

    @Override
    public void addCompileSourceRoot(String dir) {
        project.addCompileSourceRoot(dir);
    }

    @Override
    public void refresh(Path file) {
        buildContext.refresh(file.toFile());
    }
}
