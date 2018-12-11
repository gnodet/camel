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
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Analyses the Camel plugins in a project and generates extra descriptor information for easier auto-discovery in Camel.
 */
@Mojo(name = "generate-static-core-converter", threadSafe = true, defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class PackageConverterMojo extends AbstractMojo {

    /**
     * The maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    /**
     * The output directory for generated components file
     */
    @Parameter(defaultValue = "${project.build.directory}/generated/camel/converter")
    protected File converterOutDir;

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

    private Index index;

    /**
     * Execute goal.
     *
     * @throws MojoExecutionException execution of the main class or one of the
     *                                threads it generated failed.
     * @throws MojoFailureException   something bad happened...
     */
    public void execute() throws MojoExecutionException, MojoFailureException {

        List<String> locations = new ArrayList<>();
        locations.add(project.getBuild().getOutputDirectory());
        project.getDependencyArtifacts()
                .stream()
                .map(Artifact::getFile)
                .forEach(f -> locations.add(f.toString()));

        createConverter(createIndex(locations));

        project.addCompileSourceRoot(converterOutDir.toString());
    }

    private Index createIndex(List<String> locations) throws MojoExecutionException {
        try {
            Indexer indexer = new Indexer();
            locations.stream()
                    .map(this::asFolder)
                    .flatMap(this::walk)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .forEach(p -> index(indexer, p));
            return indexer.complete();
        } catch (IOError e) {
            throw new MojoExecutionException("Error", e);
        }
    }

    private Path asFolder(String p) {
        if (p.endsWith(".jar")) {
            try {
                Map<String, String> env = new HashMap<>();
                return FileSystems.newFileSystem(URI.create("jar:file:" + p + "!/"), env)
                        .getPath("/");
            } catch (IOException e) {
                throw new IOError(e);
            }
        } else {
            return Paths.get(p);
        }
    }

    private Stream<Path> walk(Path p) {
        try {
            return Files.walk(p);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private void index(Indexer indexer, Path p) {
        try (InputStream is = Files.newInputStream(p)) {
            indexer.index(is);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public void createConverter(Index index) {
        this.index = index;

        Map<String, Map<Type, AnnotationInstance>> converters = new TreeMap<>();
        DotName converterName = DotName.createSimple("org.apache.camel.Converter");
        index.getAnnotations(converterName).stream()
                .filter(ai -> ai.target().kind() == Kind.METHOD)
                .forEach(ai -> {
                    Type to = ai.target().asMethod().returnType();
                    Type from = ai.target().asMethod().parameters().get(0);
                    converters.computeIfAbsent(toString(to), c -> new TreeMap<>(this::compare)).put(from, ai);
                });

        DotName fallbackConverterName = DotName.createSimple("org.apache.camel.FallbackConverter");
        List<AnnotationInstance> fallbackConverters = index.getAnnotations(fallbackConverterName).stream()
                .filter(ai -> ai.target().kind() == Kind.METHOD)
                .sorted(Comparator.comparing(ai -> ai.target().asMethod().declaringClass().name()))
                .collect(Collectors.toList());

        StringBuilder source = new StringBuilder();
        try {
            doWriteConverter(source, converters, fallbackConverters);
        } catch (IOException e) {
            throw new IOError(e);
        }

        String p = "org.apache.camel.impl.converter";
        String c = "CoreStaticTypeConverterLoader";
        try {
            Path output = converterOutDir.toPath().resolve(p.replace('.', '/')).resolve(c + ".java");
            Files.createDirectories(output.getParent());
            try (Writer writer = Files.newBufferedWriter(output, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                writer.append(source.toString());
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private void doWriteConverter(Appendable writer, Map<String, Map<Type, AnnotationInstance>> converters, List<AnnotationInstance> fallbackConverters) throws IOException {

        Set<String> converterClasses = new LinkedHashSet<>();

        writer.append("package org.apache.camel.impl.converter;\n");
        writer.append("\n");
        writer.append("import org.apache.camel.Exchange;\n");
        writer.append("import org.apache.camel.TypeConversionException;\n");
        writer.append("import org.apache.camel.TypeConverterLoaderException;\n");
        writer.append("import org.apache.camel.spi.TypeConverterLoader;\n");
        writer.append("import org.apache.camel.spi.TypeConverterRegistry;\n");
        writer.append("import org.apache.camel.support.TypeConverterSupport;\n");
        writer.append("\n");
        writer.append("@SuppressWarnings(\"unchecked\")\n");
        writer.append("public class CoreStaticTypeConverterLoader implements TypeConverterLoader {\n");
        writer.append("\n");
        writer.append("    public static final CoreStaticTypeConverterLoader INSTANCE = new CoreStaticTypeConverterLoader();\n");
        writer.append("\n");
        writer.append("    static abstract class SimpleTypeConverter extends TypeConverterSupport {\n");
        writer.append("        private final boolean allowNull;\n");
        writer.append("\n");
        writer.append("        public SimpleTypeConverter(boolean allowNull) {\n");
        writer.append("            this.allowNull = allowNull;\n");
        writer.append("        }\n");
        writer.append("\n");
        writer.append("        @Override\n");
        writer.append("        public boolean allowNull() {\n");
        writer.append("            return allowNull;\n");
        writer.append("        }\n");
        writer.append("\n");
        writer.append("        @Override\n");
        writer.append("        public <T> T convertTo(Class<T> type, Exchange exchange, Object value) throws TypeConversionException {\n");
        writer.append("            try {\n");
        writer.append("                return (T) doConvert(exchange, value);\n");
        writer.append("            } catch (TypeConversionException e) {\n");
        writer.append("                throw e;\n");
        writer.append("            } catch (Exception e) {\n");
        writer.append("                throw new TypeConversionException(value, type, e);\n");
        writer.append("            }\n");
        writer.append("        }\n");
        writer.append("        protected abstract Object doConvert(Exchange exchange, Object value) throws Exception;\n");
        writer.append("    };\n");
        writer.append("\n");
        writer.append("    private DoubleMap<Class<?>, Class<?>, SimpleTypeConverter> converters = new DoubleMap<>(256);\n");
        writer.append("\n");
        writer.append("    private CoreStaticTypeConverterLoader() {\n");

        for (Map.Entry<String, Map<Type, AnnotationInstance>> toE : converters.entrySet()) {
            for (Map.Entry<Type, AnnotationInstance> fromE : toE.getValue().entrySet()) {
                String to = toE.getKey();
                Type from = fromE.getKey();
                AnnotationInstance ai = fromE.getValue();
                boolean allowNull = ai.value("allowNull") != null && ai.value("allowNull").asBoolean();
                writer.append("        converters.put(").append(to).append(".class").append(", ")
                        .append(toString(from)).append(".class, new SimpleTypeConverter(")
                        .append(Boolean.toString(allowNull)).append(") {\n");
                writer.append("            @Override\n");
                writer.append("            public Object doConvert(Exchange exchange, Object value) throws Exception {\n");
                writer.append("                return ").append(toJava(ai, converterClasses)).append(";\n");
                writer.append("            }\n");
                writer.append("        });\n");
            }
        }
        writer.append("    }\n");
        writer.append("\n");
        writer.append("    @Override\n");
        writer.append("    public void load(TypeConverterRegistry registry) throws TypeConverterLoaderException {\n");
        writer.append("        converters.forEach((k, v, c) -> registry.addTypeConverter(k, v, c));\n");
        for (AnnotationInstance ai : fallbackConverters) {
            boolean allowNull = ai.value("allowNull") != null && ai.value("allowNull").asBoolean();
            boolean canPromote = ai.value("canPromote") != null && ai.value("canPromote").asBoolean();

            writer.append("        registry.addFallbackTypeConverter(new TypeConverterSupport() {\n");
            writer.append("            @Override\n");
            writer.append("            public boolean allowNull() {\n");
            writer.append("                return ").append(Boolean.toString(allowNull)).append(";\n");
            writer.append("            }\n");
            writer.append("            @Override\n");
            writer.append("            public <T> T convertTo(Class<T> type, Exchange exchange, Object value) throws TypeConversionException {\n");
            writer.append("                try {\n");
            writer.append("                    return (T) ").append(toJavaFallback(ai, converterClasses)).append(";\n");
            writer.append("                } catch (TypeConversionException e) {\n");
            writer.append("                    throw e;\n");
            writer.append("                } catch (Exception e) {\n");
            writer.append("                    throw new TypeConversionException(value, type, e);\n");
            writer.append("                }\n");
            writer.append("            }\n");
            writer.append("        }, ").append(Boolean.toString(canPromote)).append(");\n");
        }
        writer.append("    }\n");
        writer.append("\n");

        for (String f : converterClasses) {
            String s = f.substring(f.lastIndexOf('.') + 1);
            String v = s.substring(0, 1).toLowerCase() + s.substring(1);
            writer.append("    private volatile ").append(f).append(" ").append(v).append(";\n");
            writer.append("    private ").append(f).append(" get").append(s).append("() {\n");
            writer.append("        if (").append(v).append(" == null) {\n");
            writer.append("            synchronized (this) {\n");
            writer.append("                if (").append(v).append(" == null) {\n");
            writer.append("                    ").append(v).append(" = new ").append(f).append("();\n");
            writer.append("                }\n");
            writer.append("            }\n");
            writer.append("        }\n");
            writer.append("        return ").append(v).append(";\n");
            writer.append("    }\n");
        }

        writer.append("}\n");
    }

    private int compare(Type t1, Type t2) {
        if (t1 == t2) {
            return 0;
        }
        String s1 = toString(t1);
        String s2 = toString(t2);
        if ("java.lang.Object".equals(s1)) {
            return +1;
        }
        if ("java.lang.Object".equals(s2)) {
            return -1;
        }
        if (t1.kind() == Type.Kind.CLASS && t2.kind() == Type.Kind.CLASS) {
            if (isAssignable(s1, s2)) {
                return -1;
            }
            if (isAssignable(s2, s1)) {
                return +1;
            }
        }
        return toString(t1).compareTo(toString(t2));
    }

    private boolean isAssignable(String c1, String c2) {
        return superTypes(c1).anyMatch(c2::equals);
    }

    private String className(Type t) {
        return t.name().toString();
    }

    private Stream<String> superTypes(String clazz) {
        ClassInfo ci = index.getClassByName(DotName.createSimple(clazz));
        if (ci != null) {
            return Stream.concat(
                    Stream.concat(Stream.of(clazz), superTypes(className(ci.superClassType()))),
                    ci.interfaceTypes().stream().map(this::className).flatMap(this::superTypes));
        } else {
            try {
                Class cl = Class.forName(clazz);
                return superTypes(cl);
            } catch (Throwable t) {
                throw new IllegalStateException("Unable to find class info for " + clazz);
            }
        }
    }

    private Stream<String> superTypes(Class clazz) {
        if (clazz != null) {
            return Stream.concat(
                    Stream.concat(Stream.of(clazz.getName()), superTypes(clazz.getSuperclass())),
                    Stream.of(clazz.getInterfaces()).flatMap(this::superTypes));
        } else {
            return Stream.empty();
        }
    }

    private String toJava(AnnotationInstance ai, Set<String> converterClasses) {
        String pfx;
        MethodInfo converter = ai.target().asMethod();
        if (java.lang.reflect.Modifier.isStatic(converter.flags())) {
            pfx = converter.declaringClass().toString() + "." + converter.name();
        } else {
            converterClasses.add(converter.declaringClass().toString());
            pfx = "get" + converter.declaringClass().name().local() + "()." + converter.name();
        }
        String type = toString(converter.parameters().get(0));
        String cast = type.equals("java.lang.Object") ? "" : "(" + type + ") ";
        return pfx + "(" + cast + "value" + (converter.parameters().size() == 2 ? ", exchange" : "") + ")";
    }

    private String toJavaFallback(AnnotationInstance ai, Set<String> converterClasses) {
        String pfx;
        MethodInfo converter = ai.target().asMethod();
        if (java.lang.reflect.Modifier.isStatic(converter.flags())) {
            pfx = converter.declaringClass().toString() + "." + converter.name();
        } else {
            converterClasses.add(converter.declaringClass().toString());
            pfx = "get" + converter.declaringClass().name().local() + "()." + converter.name();
        }
        String type = toString(converter.parameters().get(converter.parameters().size() - 2));
        String cast = type.equals("java.lang.Object") ? "" : "(" + type + ") ";
        return pfx + "(type, " + (converter.parameters().size() == 4 ? "exchange, " : "") + cast + "value" + ", registry)";
    }

    private String toString(Type type) {
        if (type == null) {
            throw new IllegalArgumentException("null");
        }
        switch (type.kind()) {
            case CLASS:
            case PRIMITIVE:
                return type.toString();
            case ARRAY:
                return toString(type.asArrayType().component()) + "[]";
            case PARAMETERIZED_TYPE:
                return type.name().toString();
            default:
                throw new UnsupportedOperationException();
        }
    }

}
