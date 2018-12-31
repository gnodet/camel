package org.apache.camel.tooling.apigen.gradle;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.camel.tooling.apigen.ApiComponentGenerator;
import org.apache.camel.tooling.apigen.helpers.IOHelper;

public class GradleGenerator implements ApiComponentGenerator.Project {

    String scheme;
    String outPackage;
    String componentName;
    String componentPackage;
    List<String> classpath;
    Path basedir;
    Path testSourceDirectory;

    public void fromApis(File input, File outputMain, File outputTest) {
        if (Files.isDirectory(input.toPath())) {
            IOHelper.list(input.toPath())
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(IOHelper::toJson)
                    .map(ApiComponentGenerator::toGenerator)
                    .forEach(generator -> {
                        generator.project = this;
                        generator.scheme = scheme;
                        generator.outPackage = outPackage;
                        generator.componentName = componentName;
                        generator.componentPackage = componentPackage;
                        generator.generatedSrcDir = outputMain.toPath();
                        generator.generatedTestDir = outputTest.toPath();
                        generator.execute();
                    });
        }
    }

    @Override
    public Path getTestSourceDirectory() {
        return testSourceDirectory;
    }

    public void setTestSourceDirectory(Path testSourceDirectory) {
        this.testSourceDirectory = testSourceDirectory;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getOutPackage() {
        return outPackage;
    }

    public void setOutPackage(String outPackage) {
        this.outPackage = outPackage;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentPackage() {
        return componentPackage;
    }

    public void setComponentPackage(String componentPackage) {
        this.componentPackage = componentPackage;
    }

    @Override
    public List<String> getClasspath() {
        return classpath;
    }

    public void setClasspath(List<String> classpath) {
        this.classpath = classpath;
    }

    @Override
    public Path getBasedir() {
        return basedir;
    }

    public void setBasedir(Path basedir) {
        this.basedir = basedir;
    }
}
