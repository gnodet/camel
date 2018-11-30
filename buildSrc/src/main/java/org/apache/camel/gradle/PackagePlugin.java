package org.apache.camel.gradle;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.DomainObjectCollection;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

public class PackagePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPlugins().withType(JavaPlugin.class, plugin -> {
            JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
            SourceSet main = javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            Set<File> sources = main.getResources().getSrcDirs();
            Path output = project.getBuildDir().toPath().resolve("resources/camel");
            List<Path> inputs = sources.stream().map(File::toPath)
                    .filter(Files::isDirectory)
                    .collect(Collectors.toList());
            if (!inputs.isEmpty()) {
                inputs.forEach(input -> {
                    project.getTasks().create("generate-components-list", PackageComponent.class, task -> {
                        task.setInputDirectory(input);
                        task.setOutputDirectory(output);
                    });
                });
                sources.add(output.toFile());
            }
        });

    }

}
