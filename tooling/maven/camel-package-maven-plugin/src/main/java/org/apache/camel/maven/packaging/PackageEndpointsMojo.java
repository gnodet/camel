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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.type.DeclaredType;

import com.thoughtworks.qdox.library.SourceLibrary;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaSource;
import org.apache.camel.maven.packaging.model.ComponentModel;
import org.apache.camel.maven.packaging.model.ComponentOptionModel;
import org.apache.camel.maven.packaging.model.EndpointOptionModel;
import org.apache.camel.maven.packaging.model.EndpointPathModel;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;
import org.apache.camel.spi.UriPath;
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
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.sonatype.plexus.build.incremental.BuildContext;

import static org.apache.camel.maven.packaging.Strings.canonicalClassName;
import static org.apache.camel.maven.packaging.Strings.getOrElse;
import static org.apache.camel.maven.packaging.Strings.isNullOrEmpty;

/**
 * Analyses the Camel plugins in a project and generates extra descriptor information for easier auto-discovery in Camel.
 */
@Mojo(name = "generate-endpoints-list", threadSafe = true, defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class PackageEndpointsMojo extends AbstractMojo {

    private static final String HEADER_FILTER_STRATEGY_JAVADOC = "To use a custom HeaderFilterStrategy to filter header to and from Camel message.";


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
     *                 threads it generated failed.
     * @throws MojoFailureException something bad happened...
     */
    public void execute() throws MojoExecutionException, MojoFailureException {

        this.index = createIndex();

        index.getAnnotations(DotName.createSimple("org.apache.camel.spi.UriEndpoint"))
                .forEach(this::processEndpointClass);

        projectHelper.addResource(project, endpointsOutDir.getPath(), Collections.singletonList("org/apache/camel/**/*"), Collections.emptyList());
    }

    private Index createIndex() throws MojoExecutionException {
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
        return index;
    }

    private Optional<AnnotationValue> value(AnnotationInstance ai, String name) {
        return Optional.ofNullable(ai).map(a -> a.value(name));
    }

    private Optional<String> string(AnnotationInstance ai, String name) {
        return value(ai, name).map(AnnotationValue::asString)
                .filter(s -> !isNullOrEmpty(s));
    }

    private String string(AnnotationInstance ai, String name, String def) {
        return string(ai, name).orElse(def);
    }

    private Optional<Boolean> bool(AnnotationInstance ai, String name) {
        try {
            return value(ai, name).map(AnnotationValue::asBoolean);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to retrieve value '" + name + "' from annotation " + ai.toString(), e);
        }
    }

    private boolean bool(AnnotationInstance ai, String name, boolean def) {
        return bool(ai, name).orElse(def);
    }

    private void processEndpointClass(AnnotationInstance uriEndpoint) {
        if (uriEndpoint != null) {
            String scheme = string(uriEndpoint, "scheme", "");
            String extendsScheme = string(uriEndpoint, "extendsScheme").orElse("");
            String title = string(uriEndpoint, "title", "");
            String label = string(uriEndpoint, "label").orElse("");
            if (!isNullOrEmpty(scheme)) {
                // support multiple schemes separated by comma, which maps to the exact same component
                // for example camel-mail has a bunch of component schema names that does that
                String[] schemes = scheme.split(",");
                String[] titles = title.split(",");
                String[] extendsSchemes = extendsScheme.split(",");
                for (int i = 0; i < schemes.length; i++) {
                    final String alias = schemes[i];
                    final String extendsAlias = i < extendsSchemes.length ? extendsSchemes[i] : extendsSchemes[0];
                    String aTitle = i < titles.length ? titles[i] : titles[0];

                    // some components offer a secure alternative which we need to amend the title accordingly
                    if (secureAlias(schemes[0], alias)) {
                        aTitle += " (Secure)";
                    }
                    final String aliasTitle = aTitle;

                    // write json schema
                    String name = canonicalClassName(uriEndpoint.target().asClass().name().toString());
                    String packageName = name.substring(0, name.lastIndexOf("."));
                    String fileName = alias + ".json";
                    String json = writeJSonSchemeDocumentation(uriEndpoint, uriEndpoint.target().asClass(), aliasTitle, alias, extendsAlias, label, schemes);

                    Path dir = endpointsOutDir.toPath().resolve(packageName.replace('.', '/'));
                    try {
                        Files.createDirectories(dir);
                        try (Writer writer = Files.newBufferedWriter(dir.resolve(fileName))) {
                            writer.write("// Generated by camel annotation processor\n");
                            writer.write(json);
                            writer.write("\n");
                        }
                    } catch (IOException e) {
                        throw new IOError(e);
                    }
                }
            }
        }
    }

    protected String writeJSonSchemeDocumentation(AnnotationInstance uriEndpoint, ClassInfo classElement,
                                                String title, String scheme, String extendsScheme, String label, String[] schemes) {
        // gather component information
        ComponentModel componentModel = findComponentProperties(uriEndpoint, classElement, title, scheme, extendsScheme, label);

        // get endpoint information which is divided into paths and options (though there should really only be one path)
        Set<EndpointPathModel> endpointPaths = new LinkedHashSet<>();
        Set<EndpointOptionModel> endpointOptions = new LinkedHashSet<>();
//        Set<ComponentOptionModel> componentOptions = new LinkedHashSet<>();

        Optional.ofNullable(componentModel.getJavaType())
                .map(this::findTypeElement)
                .ifPresent(ci -> findComponentClassProperties(componentModel, ci, ""));

        findClassProperties(componentModel, endpointPaths, endpointOptions, classElement, "", string(uriEndpoint, "excludeProperties", ""));

        return createParameterJsonSchema(componentModel, endpointPaths, endpointOptions, schemes);
    }

    public String createParameterJsonSchema(ComponentModel componentModel,
                                            Set<EndpointPathModel> endpointPaths, Set<EndpointOptionModel> endpointOptions, String[] schemes) {
        StringBuilder buffer = new StringBuilder("{");
        // component model
        buffer.append("\n  \"component\": {");
        buffer.append("\n    \"kind\": \"").append("component").append("\",");
        buffer.append("\n    \"scheme\": \"").append(componentModel.getScheme()).append("\",");
        if (!isNullOrEmpty(componentModel.getExtendsScheme())) {
            buffer.append("\n    \"extendsScheme\": \"").append(componentModel.getExtendsScheme()).append("\",");
        }
        // the first scheme is the regular so only output if there is alternatives
        if (schemes != null && schemes.length > 1) {
            CollectionStringBuffer csb = new CollectionStringBuffer(",");
            for (String altScheme : schemes) {
                csb.append(altScheme);
            }
            buffer.append("\n    \"alternativeSchemes\": \"").append(csb.toString()).append("\",");
        }
        buffer.append("\n    \"syntax\": \"").append(componentModel.getSyntax()).append("\",");
        if (componentModel.getAlternativeSyntax() != null) {
            buffer.append("\n    \"alternativeSyntax\": \"").append(componentModel.getAlternativeSyntax()).append("\",");
        }
        buffer.append("\n    \"title\": \"").append(componentModel.getTitle()).append("\",");
        buffer.append("\n    \"description\": \"").append(componentModel.getDescription()).append("\",");
        buffer.append("\n    \"label\": \"").append(getOrElse(componentModel.getLabel(), "")).append("\",");
        buffer.append("\n    \"deprecated\": ").append(componentModel.isDeprecated()).append(",");
        buffer.append("\n    \"deprecationNote\": \"").append(getOrElse(componentModel.getDeprecationNote(), "")).append("\",");
        buffer.append("\n    \"async\": ").append(componentModel.isAsync()).append(",");
        buffer.append("\n    \"consumerOnly\": ").append(componentModel.getConsumerOnly()).append(",");
        buffer.append("\n    \"producerOnly\": ").append(componentModel.getProducerOnly()).append(",");
        buffer.append("\n    \"lenientProperties\": ").append(componentModel.isLenientProperties()).append(",");
        buffer.append("\n    \"javaType\": \"").append(componentModel.getJavaType()).append("\",");
        if (componentModel.getFirstVersion() != null) {
            buffer.append("\n    \"firstVersion\": \"").append(componentModel.getFirstVersion()).append("\",");
        }
        buffer.append("\n    \"groupId\": \"").append(componentModel.getGroupId()).append("\",");
        buffer.append("\n    \"artifactId\": \"").append(componentModel.getArtifactId()).append("\",");
        if (componentModel.getVerifiers() != null) {
            buffer.append("\n    \"verifiers\": \"").append(componentModel.getVerifiers()).append("\",");
        }
        buffer.append("\n    \"version\": \"").append(componentModel.getVersion()).append("\"");

        buffer.append("\n  },");

        // and component properties
        buffer.append("\n  \"componentProperties\": {");
        boolean first = true;
        for (ComponentOptionModel entry : componentModel.getComponentOptions()) {
            if (first) {
                first = false;
            } else {
                buffer.append(",");
            }
            buffer.append("\n    ");
            // either we have the documentation from this apt plugin or we need help to find it from extended component
            String doc = entry.getDocumentationWithNotes();
            if (isNullOrEmpty(doc)) {
                doc = DocumentationHelper.findComponentJavaDoc(componentModel.getScheme(), componentModel.getExtendsScheme(), entry.getName());
            }
            // as its json we need to sanitize the docs
            doc = JSonSchemaHelper.sanitizeDescription(doc, false);
            boolean required = entry.isRequired();
            String defaultValue = entry.getDefaultValue();
            if (isNullOrEmpty(defaultValue) && "boolean".equals(entry.getType())) {
                // fallback as false for boolean types
                defaultValue = "false";
            }

            // component options do not have prefix
            String optionalPrefix = "";
            String prefix = "";
            boolean multiValue = false;
            boolean asPredicate = false;

            buffer.append(JSonSchemaHelper.toJson(entry.getName(), entry.getDisplayName(), "property", required, entry.getType(), defaultValue, doc,
                    entry.isDeprecated(), entry.getDeprecationNote(), entry.isSecret(), entry.getGroup(), entry.getLabel(), entry.isEnumType(), entry.getEnums(),
                    false, null, asPredicate, optionalPrefix, prefix, multiValue));
        }
        buffer.append("\n  },");

        buffer.append("\n  \"properties\": {");
        first = true;

        // sort the endpoint options in the standard order we prefer
        List<EndpointPathModel> paths = new ArrayList<>(endpointPaths);
        paths.sort(EndpointHelper.createPathComparator(componentModel.getSyntax()));

        // include paths in the top
        for (EndpointPathModel entry : paths) {
            String label = entry.getLabel();
            if (label != null) {
                // skip options which are either consumer or producer labels but the component does not support them
                if (label.contains("consumer") && componentModel.isProducerOnly()) {
                    continue;
                } else if (label.contains("producer") && componentModel.isConsumerOnly()) {
                    continue;
                }
            }

            if (first) {
                first = false;
            } else {
                buffer.append(",");
            }
            buffer.append("\n    ");
            // either we have the documentation from this apt plugin or we need help to find it from extended component
            String doc = entry.getDocumentation();
            if (isNullOrEmpty(doc)) {
                doc = DocumentationHelper.findEndpointJavaDoc(componentModel.getScheme(), componentModel.getExtendsScheme(), entry.getName());
            }
            // as its json we need to sanitize the docs
            doc = JSonSchemaHelper.sanitizeDescription(doc, false);
            boolean required = entry.getRequired();
            String defaultValue = entry.getDefaultValue();
            if (isNullOrEmpty(defaultValue) && "boolean".equals(entry.getType())) {
                // fallback as false for boolean types
                defaultValue = "false";
            }

            // @UriPath options do not have prefix
            String optionalPrefix = "";
            String prefix = "";
            boolean multiValue = false;
            boolean asPredicate = false;

            buffer.append(JSonSchemaHelper.toJson(entry.getName(), entry.getDisplayName(), "path", required, entry.getType(), defaultValue, doc,
                    entry.isDeprecated(), entry.getDeprecationNote(), entry.isSecret(), entry.getGroup(), entry.getLabel(), entry.isEnumType(), entry.getEnums(),
                    false, null, asPredicate, optionalPrefix, prefix, multiValue));
        }

        // sort the endpoint options in the standard order we prefer
        List<EndpointOptionModel> options = new ArrayList<>(endpointOptions);
        options.sort(EndpointHelper.createGroupAndLabelComparator());

        // and then regular parameter options
        for (EndpointOptionModel entry : options) {
            String label = entry.getLabel();
            if (label != null) {
                // skip options which are either consumer or producer labels but the component does not support them
                if (label.contains("consumer") && componentModel.isProducerOnly()) {
                    continue;
                } else if (label.contains("producer") && componentModel.isConsumerOnly()) {
                    continue;
                }
            }

            if (first) {
                first = false;
            } else {
                buffer.append(",");
            }
            buffer.append("\n    ");
            // either we have the documentation from this apt plugin or we need help to find it from extended component
            String doc = entry.getDocumentationWithNotes();
            if (isNullOrEmpty(doc)) {
                doc = DocumentationHelper.findEndpointJavaDoc(componentModel.getScheme(), componentModel.getExtendsScheme(), entry.getName());
            }
            // as its json we need to sanitize the docs
            doc = JSonSchemaHelper.sanitizeDescription(doc, false);
            boolean required = entry.getRequired();
            String defaultValue = entry.getDefaultValue();
            if (isNullOrEmpty(defaultValue) && "boolean".equals(entry.getType())) {
                // fallback as false for boolean types
                defaultValue = "false";
            }
            String optionalPrefix = entry.getOptionalPrefix();
            String prefix = entry.getPrefix();
            boolean multiValue = entry.isMultiValue();
            boolean asPredicate = false;

            buffer.append(JSonSchemaHelper.toJson(entry.getName(), entry.getDisplayName(), "parameter", required, entry.getType(), defaultValue,
                    doc, entry.isDeprecated(), entry.getDeprecationNote(), entry.isSecret(), entry.getGroup(), entry.getLabel(), entry.isEnumType(), entry.getEnums(),
                    false, null, asPredicate, optionalPrefix, prefix, multiValue));
        }
        buffer.append("\n  }");

        buffer.append("\n}\n");
        return buffer.toString();
    }

    protected ComponentModel findComponentProperties(AnnotationInstance uriEndpoint, ClassInfo endpointClassElement,
                                                     String title, String scheme, String extendsScheme, String label) {
        boolean coreOnly = project.getArtifactId().equals("camel-core");
        ComponentModel model = new ComponentModel(coreOnly);
        model.setScheme(scheme);

        // if the scheme is an alias then replace the scheme name from the syntax with the alias
        String syntax = scheme + ":" + Strings.after(string(uriEndpoint, "syntax").get(), ":");
        // alternative syntax is optional
        if (!isNullOrEmpty(string(uriEndpoint, "alternativeSyntax", ""))) {
            String alternativeSyntax = scheme + ":" + Strings.after(string(uriEndpoint, "alternativeSyntax", ""), ":");
            model.setAlternativeSyntax(alternativeSyntax);
        }

        model.setExtendsScheme(extendsScheme);
        model.setSyntax(syntax);
        model.setTitle(title);
        model.setLabel(label);
        model.setConsumerOnly(bool(uriEndpoint, "consumerOnly", false));
        model.setProducerOnly(bool(uriEndpoint, "producerOnly", false));
        model.setLenientProperties(bool(uriEndpoint, "lenientProperties", false));
        model.setAsync(implementsInterface(endpointClassElement, "org.apache.camel.AsyncEndpoint"));

        // what is the first version this component was added to Apache Camel
        String firstVersion = string(uriEndpoint, "firstVersion", "");
        if (isNullOrEmpty(firstVersion) && annotation(endpointClassElement, Metadata.class.getName()).isPresent()) {
            // fallback to @Metadata if not from @UriEndpoint
            firstVersion = string(annotation(endpointClassElement, Metadata.class.getName()).get(), "firstVersion", null);
        }
        if (!isNullOrEmpty(firstVersion)) {
            model.setFirstVersion(firstVersion);
        }

        String data = loadResource("META-INF/services/org/apache/camel/component", scheme);
        if (data != null) {
            Map<String, String> map = parseAsMap(data);
            model.setJavaType(map.get("class"));
        }

        data = loadResource("META-INF/services/org/apache/camel", "component.properties");
        if (data != null) {
            Map<String, String> map = parseAsMap(data);
            // now we have a lot more data, so we need to load it as key/value
            // need to sanitize the description first
            String doc = map.get("projectDescription");
            if (doc != null) {
                model.setDescription(JSonSchemaHelper.sanitizeDescription(doc, true));
            } else {
                model.setDescription("");
            }

            // we can mark a component as deprecated by using the annotation or in the pom.xml
            model.setDeprecated(annotation(endpointClassElement, Deprecated.class.getName()).isPresent()
                    || map.getOrDefault("projectName", "").contains("(deprecated)"));

            model.setDeprecationNote(annotation(endpointClassElement, Metadata.class.getName())
                    .map(ai -> string(ai, "deprecationNote", ""))
                    .orElse(null));

            model.setGroupId(map.getOrDefault("groupId", ""));
            model.setArtifactId(map.getOrDefault("artifactId", ""));
            model.setVersion(map.getOrDefault("version", ""));
        }

        return model;
    }

    private boolean implementsInterface(ClassInfo endpointClassElement, String itfName) {
        return false;
    }

    private String loadResource(String path, String name) {
        String out = project.getBuild().getOutputDirectory();
        Path resource = Paths.get(out).resolve(path).resolve(name);
        if (Files.isRegularFile(resource)) {
            try {
                return Files.lines(resource)
                        .filter(s -> !s.startsWith("#"))
                        .collect(Collectors.joining("\n"));
            } catch (IOException e) {
                throw new IOError(e);
            }
        } else {
            return null;
        }
    }

    protected void findComponentClassProperties(ComponentModel componentModel, ClassInfo classElement, String prefix) {
        while (true) {
            AnnotationInstance componentAnnotation = annotation(classElement, "org.apache.camel.spi.Metadata")
                    .orElse(null);
            if (componentAnnotation != null && Objects.equals("verifiers", string(componentAnnotation, "label", ""))) {
                componentModel.setVerifiers(string(componentAnnotation, "enums", ""));
            }


            List<MethodInfo> methods = classElement.methods();
            for (MethodInfo method : methods) {
                String methodName = method.name();
                boolean deprecated = method.annotation(DotName.createSimple(Deprecated.class.getName())) != null;
                AnnotationInstance metadata = method.annotation(DotName.createSimple(Metadata.class.getName()));
                String deprecationNote = null;
                if (metadata != null) {
                    deprecationNote = string(metadata, "deprecationNode", null);
                }

                // must be the setter
                boolean isSetter = methodName.startsWith("set") && method.parameters().size() == 1 & method.returnType().kind() == Type.Kind.VOID;
                if (!isSetter) {
                    continue;
                }

                // skip unwanted methods as they are inherited from default component and are not intended for end users to configure
                if ("setEndpointClass".equals(methodName) || "setCamelContext".equals(methodName)
                        || "setEndpointHeaderFilterStrategy".equals(methodName) || "setApplicationContext".equals(methodName)) {
                    continue;
                }

                if (isGroovyMetaClassProperty(method)) {
                    continue;
                }

                // must be a getter/setter pair
                String fieldName = methodName.substring(3);
                fieldName = fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);

                // we usually favor putting the @Metadata annotation on the field instead of the setter, so try to use it if its there
                FieldInfo field = classElement.field(fieldName);
                if (field != null && metadata == null) {
                    String an = Metadata.class.getName();
                    metadata = annotation(field, an).orElse(null);
                }

                boolean required = Boolean.parseBoolean(string(metadata, "required", "false"));
                String label = string(metadata, "label", null);
                boolean secret = bool(metadata, "secret", false);
                String displayName = string(metadata, "displayName", null);

                // we do not yet have default values / notes / as no annotation support yet
                // String defaultValueNote = param.defaultValueNote();
                String defaultValue = string(metadata, "defaultValue", null);
                String defaultValueNote = null;

                MethodInfo setter = method;
                String name = fieldName;
                name = prefix + name;
                Type fieldType = setter.parameters().get(0);
                String fieldTypeName = fieldType.toString();
                ClassInfo fieldTypeElement = findTypeElement(fieldTypeName);

                String docComment = findJavaDoc(fieldName, name, classElement, false);
                if (isNullOrEmpty(docComment)) {
                    docComment = string(metadata, "description", "");
                }
                if (isNullOrEmpty(docComment)) {
                    // apt cannot grab javadoc from camel-core, only from annotations
                    if ("setHeaderFilterStrategy".equals(methodName)) {
                        docComment = HEADER_FILTER_STRATEGY_JAVADOC;
                    } else {
                        docComment = "";
                    }
                }

                // gather enums
                Set<String> enums = new LinkedHashSet<>();
                boolean isEnum;
                String enumsStr = string(metadata, "enums", "");
                if (!isNullOrEmpty(enumsStr)) {
                    isEnum = true;
                    Collections.addAll(enums, enumsStr.split(","));
                } else if (isEnumClass(fieldTypeElement)) {
                    isEnum = true;
                    // find all the enum constants which has the possible enum value that can be used
                    fieldTypeElement.fields().stream()
                            .filter(PackageEndpointsMojo::isEnumConstant)
                            .map(FieldInfo::name)
                            .forEach(enums::add);
                } else {
                    isEnum = false;
                }

                // the field type may be overloaded by another type
                if (!isNullOrEmpty(string(metadata, "javaType", null))) {
                    fieldTypeName = string(metadata, "javaType", null);
                }

                String group = EndpointHelper.labelAsGroupName(label, componentModel.isConsumerOnly(), componentModel.isProducerOnly());
                ComponentOptionModel option = new ComponentOptionModel();
                option.setName(name);
                option.setDisplayName(displayName);
                option.setJavaType(fieldTypeName);
                option.setRequired(required);
                option.setDefaultValue(defaultValue);
                option.setDefaultValueNote(defaultValueNote);
                option.setDescription(docComment.trim());
                option.setDeprecated(deprecated);
                option.setDeprecationNote(deprecationNote);
                option.setSecret(secret);
                option.setGroup(group);
                option.setLabel(label);
                option.setEnumType(isEnum);
                option.setEnums(enums);

                componentModel.addComponentOption(option);
            }

            // check super classes which may also have fields
            if (classElement.superName() != null) {
                classElement = findTypeElement(classElement.superName());
                if (classElement == null) {
                    break;
                }
            } else {
                break;
            }
        }
    }

    private Optional<AnnotationInstance> annotation(FieldInfo field, String name) {
        return field.annotations().stream()
                .filter(ai -> ai.name().equals(DotName.createSimple(name)))
                .findAny();
    }

    private Optional<AnnotationInstance> annotation(ClassInfo classElement, String name) {
        if (classElement != null) {
            Map<DotName, List<AnnotationInstance>> annotations = classElement.annotations();
            DotName n = DotName.createSimple(name);
            if (annotations.containsKey(n)) {
                return annotations.get(n)
                        .stream()
                        .filter(ai -> ai.target().kind() == Kind.CLASS)
                        .findAny();
            }
        }
        return Optional.empty();
    }

    protected void findClassProperties(ComponentModel componentModel,
                                       Set<EndpointPathModel> endpointPaths, Set<EndpointOptionModel> endpointOptions,
                                       ClassInfo classElement, String prefix, String excludeProperties) {
        while (true) {

            for (FieldInfo fieldElement : classElement.fields()) {

                AnnotationInstance metadata = annotation(fieldElement, Metadata.class.getName()).orElse(null);
                boolean deprecated = annotation(fieldElement, Deprecated.class.getName()).isPresent();
                String deprecationNote = null;
                if (metadata != null) {
                    deprecationNote = string(metadata, "deprecationNote", "");
                }
                boolean secret = bool(metadata, "secret", false);

                AnnotationInstance path = annotation(fieldElement, UriPath.class.getName()).orElse(null);
                String fieldName = fieldElement.name();
                if (path != null) {
                    String name = prefix + string(path, "name").orElse(fieldName);

                    // should we exclude the name?
                    if (excludeProperty(excludeProperties, name)) {
                        continue;
                    }

                    String defaultValue = string(path, "defaultValue", string(metadata, "defaultValue", ""));
                    boolean required = Boolean.parseBoolean(string(metadata, "required", "false"));
                    String label = string(path, "label", string(metadata, "label", ""));
                    String displayName = string(path, "displayName", string(metadata, "displayName", ""));

                    Type fieldType = fieldElement.type();
                    String fieldTypeName = fieldType.toString();
                    ClassInfo fieldTypeElement = findTypeElement(fieldTypeName);

                    String docComment = findJavaDoc(fieldName, name, classElement, false);
                    if (isNullOrEmpty(docComment)) {
                        docComment = string(path, "description", "");
                    }

                    // gather enums
                    Set<String> enums = new LinkedHashSet<>();

                    boolean isEnum;
                    String enumsStr = string(path, "enums", "");
                    if (!isNullOrEmpty(enumsStr)) {
                        isEnum = true;
                        Collections.addAll(enums, enumsStr.split(","));
                    } else if (isEnumClass(fieldTypeElement)) {
                        isEnum = true;
                        // find all the enum constants which has the possible enum value that can be used
                        fieldTypeElement.fields().stream()
                                .filter(PackageEndpointsMojo::isEnumConstant)
                                .map(FieldInfo::name)
                                .forEach(enums::add);
                    } else {
                        isEnum = false;
                    }

                    // the field type may be overloaded by another type
                    fieldTypeName = string(path, "javaType", fieldTypeName);

                    String group = EndpointHelper.labelAsGroupName(label, componentModel.isConsumerOnly(), componentModel.isProducerOnly());
                    EndpointPathModel ep = new EndpointPathModel(name, displayName, fieldTypeName, required, defaultValue, docComment, deprecated, deprecationNote,
                            secret, group, label, isEnum, enums);
                    endpointPaths.add(ep);
                }

                AnnotationInstance param = annotation(fieldElement, UriParam.class.getName()).orElse(null);
                fieldName = fieldElement.name() ;
                if (param != null) {
                    String name = string(param, "name").orElse(fieldName);
                    name = prefix + name;

                    // should we exclude the name?
                    if (excludeProperty(excludeProperties, name)) {
                        continue;
                    }

                    String paramOptionalPrefix = string(param, "optionalPrefix", "");
                    String paramPrefix = string(param, "prefix", "");
                    boolean multiValue = bool(param, "multiValue", false);
                    String defaultValue = string(param, "defaultValue", string(metadata, "defaultValue", ""));
                    String defaultValueNote = string(param, "defaultValueNote", "");
                    boolean required = Boolean.parseBoolean(string(metadata, "required", "false"));
                    String label = string(param, "label", string(metadata, "label", ""));
                    String displayName = string(param, "displayName", string(metadata, "displayName", ""));

                    // if the field type is a nested parameter then iterate through its fields
                    Type fieldType = fieldElement.type();
                    String fieldTypeName = fieldType.toString();
                    ClassInfo fieldTypeElement = findTypeElement(fieldTypeName);
                    AnnotationInstance fieldParams = annotation(fieldTypeElement, UriParams.class.getName()).orElse(null);
                    if (fieldParams != null) {
                        String nestedPrefix = prefix;
                        String extraPrefix = string(fieldParams, "prefix", "");
                        if (!isNullOrEmpty(extraPrefix)) {
                            nestedPrefix += extraPrefix;
                        }
                        findClassProperties(componentModel, endpointPaths, endpointOptions, fieldTypeElement, nestedPrefix, excludeProperties);
                    } else {
                        String docComment = findJavaDoc(fieldName, name, classElement, false);
                        if (isNullOrEmpty(docComment)) {
                            docComment = string(param, "description", "");
                        }
                        if (isNullOrEmpty(docComment)) {
                            docComment = "";
                        }

                        // gather enums
                        Set<String> enums = new LinkedHashSet<>();
                        boolean isEnum;
                        String enumsStr = string(param, "enums", "");
                        if (!isNullOrEmpty(enumsStr)) {
                            isEnum = true;
                            Collections.addAll(enums, enumsStr.split(","));
                        } else if (isEnumClass(fieldTypeElement)) {
                            isEnum = true;
                            // find all the enum constants which has the possible enum value that can be used
                            fieldTypeElement.fields().stream()
                                    .filter(PackageEndpointsMojo::isEnumConstant)
                                    .map(FieldInfo::name)
                                    .forEach(enums::add);
                        } else {
                            isEnum = false;
                        }

                        // the field type may be overloaded by another type
                        fieldTypeName = string(param, "javaType", fieldTypeName);

                        String group = EndpointHelper.labelAsGroupName(label, componentModel.isConsumerOnly(), componentModel.isProducerOnly());
                        EndpointOptionModel option = new EndpointOptionModel();
                        option.setName(name);
                        option.setDisplayName(displayName);
                        option.setJavaType(fieldTypeName);
                        option.setRequired(required);
                        option.setDefaultValue(defaultValue);
                        option.setDefaultValueNote(defaultValueNote);
                        option.setDescription(docComment.trim());
                        option.setOptionalPrefix(paramOptionalPrefix);
                        option.setPrefix(paramPrefix);
                        option.setMultiValue(multiValue);
                        option.setDeprecated(deprecated);
                        option.setDeprecationNote(deprecationNote);
                        option.setSecret(secret);
                        option.setGroup(group);
                        option.setLabel(label);
                        option.setEnumType(isEnum);
                        option.setEnums(enums);

                        endpointOptions.add(option);
                    }
                }
            }

            // check super classes which may also have @UriParam fields
            if (classElement.superName() != null) {
                classElement = findTypeElement(classElement.superName());
                if (classElement == null) {
                    break;
                }
            } else {
                break;
            }
        }
    }

    private ClassInfo findTypeElement(String javaType) {
        return findTypeElement(DotName.createSimple(javaType));
    }

    private ClassInfo findTypeElement(DotName javaType) {
        return index.getClassByName(javaType);
    }

    private static boolean excludeProperty(String excludeProperties, String name) {
        String[] excludes = excludeProperties.split(",");
        for (String exclude : excludes) {
            if (name.equals(exclude)) {
                return true;
            }
        }
        return false;
    }

    static final int ENUM      = 0x00004000;

    private static boolean isEnumClass(ClassInfo clazz) {
        return clazz != null && (clazz.flags() & ENUM) != 0;
    }

    private static boolean isEnumConstant(FieldInfo field) {
        return (field.flags() & ENUM) != 0;
    }

    private static Map<String, String> parseAsMap(String data) {
        Map<String, String> answer = new HashMap<>();
        String[] lines = data.split("\n");
        for (String line : lines) {
            if (!line.isEmpty()) {
                int idx = line.indexOf('=');
                String key = line.substring(0, idx);
                String value = line.substring(idx + 1);
                // remove ending line break for the values
                value = value.trim().replaceAll("\n", "");
                answer.put(key.trim(), value);
            }
        }
        return answer;
    }

    private static boolean secureAlias(String scheme, String alias) {
        if (scheme.equals(alias)) {
            return false;
        }

        // if alias is like scheme but with ending s its secured
        if ((scheme + "s").equals(alias)) {
            return true;
        }

        return false;
    }

    // CHECKSTYLE:ON

    private static boolean isGroovyMetaClassProperty(final MethodInfo method) {
        final String methodName = method.name();

        if (!"setMetaClass".equals(methodName)) {
            return false;
        }

        if (method.returnType() instanceof DeclaredType) {
            final DeclaredType returnType = (DeclaredType) method.returnType();

            return "groovy.lang.MetaClass".equals(returnType.asElement().getSimpleName());
        } else {
            // Eclipse (Groovy?) compiler returns javax.lang.model.type.NoType, no other way to check but to look at toString output
            return method.toString().contains("(groovy.lang.MetaClass)");
        }
    }

    private SourceLibrary sourceLibrary = new SourceLibrary(null);

    private JavaClass getJavaClass(String className) {
        JavaClass javaClass = sourceLibrary.getJavaClass(className, false);
        if (javaClass == null) {
            Path source = getCompileSourceRoots()
                    .stream()
                    .map(Paths::get)
                    .map(p -> p.resolve(className.replace('.', '/') + ".java"))
                    .filter(Files::isRegularFile)
                    .findAny()
                    .orElse(null);
            if (source != null) {
                try {
                    JavaSource javaSource = sourceLibrary.addSource(source.toFile());
                    javaClass = javaSource.getClassByName(className);
                } catch (IOException e) {
                    throw new IOError(e);
                }
            }
        }
        return javaClass;
    }

    private String getDocComment(ClassInfo typeElement) {
        JavaClass javaClass = getJavaClass(typeElement.name().toString());
        return javaClass != null ? javaClass.getComment() : null;
    }

    public String findJavaDoc(String fieldName, String name, ClassInfo classElement, boolean builderPattern) {
        String className = classElement.name().toString();
        JavaClass javaClass = getJavaClass(className);
        String javadoc = null;
        if (javaClass != null) {
            JavaField field = javaClass.getFieldByName(fieldName);
            if (field != null && !isNullOrEmpty(field.getComment())) {
                javadoc = field.getComment();
            }
            if (javadoc == null) {
                javadoc = javaClass.getMethods().stream()
                        .filter(jm -> Objects.equals(jm.getPropertyName(), name))
                        .filter(JavaMethod::isPropertyMutator)
                        .filter(jm -> !isNullOrEmpty(jm.getComment()))
                        .findFirst()
                        .map(JavaMethod::getComment)
                        .orElse(null);
            }
            if (javadoc == null) {
                javadoc = javaClass.getMethods().stream()
                        .filter(jm -> Objects.equals(jm.getPropertyName(), name))
                        .filter(JavaMethod::isPropertyAccessor)
                        .filter(jm -> !isNullOrEmpty(jm.getComment()))
                        .findFirst()
                        .map(JavaMethod::getComment)
                        .orElse(null);
            }
            if (javadoc == null && builderPattern) {
                javadoc = javaClass.getMethods().stream()
                        .filter(jm -> Objects.equals(jm.getName(), name))
                        .filter(jm -> jm.getParameters().size() == 1)
                        .filter(jm -> !isNullOrEmpty(jm.getComment()))
                        .findFirst()
                        .map(JavaMethod::getComment)
                        .orElse(null);
            }
            if (javadoc == null && builderPattern) {
                javadoc = javaClass.getMethods().stream()
                        .filter(jm -> Objects.equals(jm.getName(), name))
                        .filter(jm -> jm.getParameters().size() == 0)
                        .filter(jm -> !isNullOrEmpty(jm.getComment()))
                        .findFirst()
                        .map(JavaMethod::getComment)
                        .orElse(null);
            }
            if (javadoc == null) {
                javadoc = javaClass.getMethods().stream()
                        .filter(jm -> Objects.equals(jm.getName(), fieldName))
                        .filter(jm -> jm.getParameters().size() == 1)
                        .filter(jm -> !isNullOrEmpty(jm.getComment()))
                        .findFirst()
                        .map(JavaMethod::getComment)
                        .orElse(null);
            }
            if (javadoc == null) {
                javadoc = javaClass.getMethods().stream()
                        .filter(jm -> Objects.equals(jm.getName(), fieldName))
                        .filter(jm -> jm.getParameters().size() == 0)
                        .filter(jm -> !isNullOrEmpty(jm.getComment()))
                        .findFirst()
                        .map(JavaMethod::getComment)
                        .orElse(null);
            }
        }
        return javadoc;
    }

    protected List<String> getCompileSourceRoots() {
        return project.getCompileSourceRoots();
    }

}
