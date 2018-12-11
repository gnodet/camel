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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.TypeElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.thoughtworks.qdox.library.SourceLibrary;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaSource;
import org.apache.camel.spi.AsPredicate;
import org.apache.camel.spi.Metadata;
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
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;
import org.sonatype.plexus.build.incremental.BuildContext;

import static org.apache.camel.maven.packaging.JSonSchemaHelper.sanitizeDescription;
import static org.apache.camel.maven.packaging.Strings.canonicalClassName;
import static org.apache.camel.maven.packaging.Strings.isNullOrEmpty;
import static org.apache.camel.maven.packaging.Strings.safeNull;

//import javax.lang.model.element.Element;
//import javax.lang.model.element.PackageElement;
//import javax.lang.model.element.TypeElement;
//import javax.lang.model.type.TypeMirror;

/**
 * Analyses the Camel plugins in a project and generates extra descriptor information for easier auto-discovery in Camel.
 */
@Mojo(name = "generate-model-doc", threadSafe = true, defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class PackageModelDocMojo extends AbstractMojo {

    /**
     * The maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    /**
     * The output directory for generated components file
     */
    @Parameter(defaultValue = "${project.build.directory}/generated/camel/modeldoc")
    protected File modeldocOutDir;

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
                .filter(Objects::nonNull)
                .forEach(f -> locations.add(f.toString()));

        processClasses(createIndex(locations));

        projectHelper.addResource(project, modeldocOutDir.getPath(), Collections.singletonList("org/apache/camel/**/*"), Collections.emptyList());
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
                return FileSystems.newFileSystem(URI.create("jar:file:" + p + "!/"), env).getPath("/");
            } catch (FileSystemAlreadyExistsException e) {
                return FileSystems.getFileSystem(URI.create("jar:file:" + p + "!/")).getPath("/");
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

    public void processClasses(Index index) {
        this.index = index;

        index.getAnnotations(DotName.createSimple(XmlRootElement.class.getName()))
                .stream()
                .filter(this::isConcrete)
                .filter(ai -> isCoreClass(ai) || isXmlClass(ai))
                .forEach(this::processClass);
    }

    private boolean isCoreClass(AnnotationInstance ai) {
        return ai.target().asClass().name().toString().startsWith("org.apache.camel.model.");
    }

    private boolean isXmlClass(AnnotationInstance ai) {
        String name = ai.target().asClass().name().toString();
        return name.startsWith("org.apache.camel.spring.")
                || name.startsWith("org.apache.camel.core.xml.");
    }

    private boolean isConcrete(AnnotationInstance ai) {
        return !Modifier.isAbstract(ai.target().asClass().flags());
    }


    // special when using expression/predicates in the model
    private static final String ONE_OF_TYPE_NAME = "org.apache.camel.model.ExpressionSubElementDefinition";

    private static final String[] ONE_OF_LANGUAGES = new String[]{
            "org.apache.camel.model.language.ExpressionDefinition",
            "org.apache.camel.model.language.NamespaceAwareExpression"
    };
    // special for inputs (these classes have sub classes, so we use this to find all classes)
    private static final String[] ONE_OF_INPUTS = new String[]{
            "org.apache.camel.model.ProcessorDefinition",
            "org.apache.camel.model.VerbDefinition"
    };
    // special for outputs (these classes have sub classes, so we use this to find all classes)
    private static final String[] ONE_OF_OUTPUTS = new String[]{
            "org.apache.camel.model.ProcessorDefinition",
            "org.apache.camel.model.NoOutputDefinition",
            "org.apache.camel.model.OutputDefinition",
            "org.apache.camel.model.ExpressionNode",
            "org.apache.camel.model.NoOutputExpressionNode",
            "org.apache.camel.model.SendDefinition",
            "org.apache.camel.model.InterceptDefinition",
            "org.apache.camel.model.WhenDefinition",
            "org.apache.camel.model.ToDynamicDefinition"
    };
    // special for verbs (these classes have sub classes, so we use this to find all classes)
    private static final String[] ONE_OF_VERBS = new String[]{
            "org.apache.camel.model.rest.VerbDefinition"
    };

    private boolean skipUnwanted = true;

    private void processClass(AnnotationInstance xmlRootElement) {
        String javaTypeName = xmlRootElement.target().asClass().name().toString();
        String packageName = javaTypeName.substring(0, javaTypeName.lastIndexOf("."));

        if (ONE_OF_TYPE_NAME.equals(javaTypeName)) {
            return;
        }

        String name = string(xmlRootElement, "name", "##default");
        if ("##default".equals(name)) {
            name = xmlRootElement.target().asClass().annotations()
                    .get(DotName.createSimple(XmlType.class.getName()))
                    .stream()
                    .filter(ai -> ai.target() == xmlRootElement.target())
                    .findAny()
                    .map(ai -> string(ai, "name", "##default"))
                    .orElse("##default");
        }
        String fileName = "##default".equals(name) || isNullOrEmpty(name)
                ? xmlRootElement.target().asClass().simpleName() + ".json"
                : name + ".json";

        boolean core = isCoreClass(xmlRootElement);
        String json = writeJSonSchemeDocumentation(
                xmlRootElement.target().asClass(), javaTypeName, name, core);

        Path path = modeldocOutDir.toPath().resolve(packageName.replace('.', '/')).resolve(fileName + ".json");
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(path, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                w.write("// Generated by camel annotation processor\n");
                w.append(json);
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    protected String writeJSonSchemeDocumentation(ClassInfo classElement,
                                                  String javaTypeName,
                                                  String modelName,
                                                  boolean core) {
        // gather eip information
        EipModel eipModel = findEipModelProperties(classElement, javaTypeName, modelName);

        // get endpoint information which is divided into paths and options (though there should really only be one path)
        Set<EipOption> eipOptions = new TreeSet<>(new EipOptionComparator(eipModel));
        findClassProperties(eipOptions, classElement, classElement, "", modelName, core);

        // after we have found all the options then figure out if the model accepts input/output
        if (core) {
            eipModel.setInput(hasInput(classElement));
            eipModel.setOutput(hasOutput(eipModel, eipOptions));
        }

        return createParameterJsonSchema(eipModel, eipOptions);
    }

    public String createParameterJsonSchema(EipModel eipModel,
                                            Set<EipOption> options) {
        StringBuilder buffer = new StringBuilder("{");
        // eip model
        buffer.append("\n  \"model\": {");
        buffer.append("\n    \"kind\": \"").append("model").append("\",");
        buffer.append("\n    \"name\": \"").append(eipModel.getName()).append("\",");
        if (eipModel.getTitle() != null) {
            buffer.append("\n    \"title\": \"").append(eipModel.getTitle()).append("\",");
        } else {
            // fallback and use name as title
            buffer.append("\n    \"title\": \"").append(asTitle(eipModel.getName())).append("\",");
        }
        buffer.append("\n    \"description\": \"").append(safeNull(eipModel.getDescription())).append("\",");
        if (eipModel.getFirstVersion() != null) {
            buffer.append("\n    \"firstVersion\": \"").append(safeNull(eipModel.getFirstVersion())).append("\",");
        }
        buffer.append("\n    \"javaType\": \"").append(eipModel.getJavaType()).append("\",");
        buffer.append("\n    \"label\": \"").append(safeNull(eipModel.getLabel())).append("\",");
        buffer.append("\n    \"deprecated\": ").append(eipModel.isDeprecated()).append(",");
        if (eipModel.getDeprecationNote() != null) {
            buffer.append("\n    \"deprecationNote\": \"").append(safeNull(eipModel.getDeprecationNote())).append("\",");
        }
        buffer.append("\n    \"input\": ").append(eipModel.getInput()).append(",");
        buffer.append("\n    \"output\": ").append(eipModel.getOutput());
        buffer.append("\n  },");

        buffer.append("\n  \"properties\": {");
        boolean first = true;

        if (!eipModel.isOutput()) {
            // filter out outputs if we do not support it (and preserve order so we need to use linked hash-set)
            options = options.stream().filter(o -> !"outputs".equals(o.getName())).collect(Collectors.toCollection(LinkedHashSet::new));
        }

        for (EipOption entry : options) {
            if (first) {
                first = false;
            } else {
                buffer.append(",");
            }
            buffer.append("\n    ");

            // as its json we need to sanitize the docs
            String doc = entry.getDocumentation();
            doc = sanitizeDescription(doc, false);
            buffer.append(JSonSchemaHelper.toJson(entry.getName(), entry.getDisplayName(), entry.getKind(), entry.isRequired(), entry.getType(), entry.getDefaultValue(), doc,
                    entry.isDeprecated(), entry.getDeprecationNode(), false, null, null, entry.isEnumType(), entry.getEnums(), entry.isOneOf(), entry.getOneOfTypes(),
                    entry.isAsPredicate(), null, null, false));
        }
        buffer.append("\n  }");

        buffer.append("\n}\n");
        return buffer.toString();
    }

    protected EipModel findEipModelProperties(ClassInfo classElement,
                                              String javaTypeName,
                                              String name) {
        EipModel model = new EipModel();
        model.setJavaType(javaTypeName);
        model.setName(name);

        AnnotationInstance metadata = annotation(classElement, Metadata.class.getName()).orElse(null);

        model.setDeprecated(annotation(classElement, Deprecated.class.getName()).isPresent());
        model.setLabel(string(metadata, "label", null));
        model.setTitle(string(metadata, "title", null));
        model.setFirstVersion(string(metadata, "firstVersion", null));

        // favor to use class javadoc of component as description
        if (model.getJavaType() != null) {
            ClassInfo typeElement = findTypeElement(model.getJavaType());
            if (typeElement != null) {
                String doc = getDocComment(typeElement);
                if (doc != null) {
                    // need to sanitize the description first (we only want a summary)
                    doc = sanitizeDescription(doc, true);
                    // the javadoc may actually be empty, so only change the doc if we got something
                    if (!Strings.isNullOrEmpty(doc)) {
                        model.setDescription(doc);
                    }
                }
            }
        }

        return model;
    }

    protected void findClassProperties(Set<EipOption> eipOptions,
                                       ClassInfo originalClassType,
                                       ClassInfo classElement,
                                       String prefix,
                                       String modelName,
                                       boolean core) {
        while (true) {

            for (FieldInfo fieldElement : classElement.fields()) {

                String fieldName = fieldElement.name();

                AnnotationInstance attribute = annotation(fieldElement, XmlAttribute.class.getName()).orElse(null);
                if (attribute != null) {
                    boolean skip = processAttribute(originalClassType, classElement, fieldElement, fieldName, attribute, eipOptions, prefix, modelName);
                    if (skip) {
                        continue;
                    }
                }

                if (core) {
                    AnnotationInstance value = annotation(fieldElement, XmlValue.class.getName()).orElse(null);
                    if (value != null) {
                        processValue(originalClassType, classElement, fieldElement, fieldName, value, eipOptions, prefix, modelName);
                    }
                }

                AnnotationInstance elements = annotation(fieldElement, XmlElements.class.getName()).orElse(null);
                if (elements != null) {
                    processElements(classElement, elements, fieldElement, eipOptions, prefix);
                }

                AnnotationInstance element = annotation(fieldElement, XmlElement.class.getName()).orElse(null);
                if (element != null) {
                    processElement(classElement, element, fieldElement, eipOptions, prefix, core);
                }

                // special for eips which has outputs or requires an expressions
                AnnotationInstance elementRef = annotation(fieldElement, XmlElementRef.class.getName()).orElse(null);
                if (elementRef != null) {

                    if (core) {
                        // special for routes
                        processRoutes(originalClassType, elementRef, fieldElement, fieldName, eipOptions, prefix);

                        // special for outputs
                        processOutputs(originalClassType, elementRef, fieldElement, fieldName, eipOptions, prefix);

                        // special for when clauses (choice eip)
                        processRefWhenClauses(originalClassType, elementRef, fieldElement, fieldName, eipOptions, prefix);

                        // special for rests (rest-dsl)
                        processRests(originalClassType, elementRef, fieldElement, fieldName, eipOptions, prefix);

                        // special for verbs (rest-dsl)
                        processVerbs(originalClassType, elementRef, fieldElement, fieldName, eipOptions, prefix);

                        // special for expression
                        processRefExpression(originalClassType, classElement, elementRef, fieldElement, fieldName, eipOptions, prefix);
                    }

                    else {
                        processElement(classElement, elementRef, fieldElement, eipOptions, prefix, core);
                    }
                }
            }

            // special when we process these nodes as they do not use JAXB annotations on fields, but on methods
            if ("OptionalIdentifiedDefinition".equals(classElement.name().local())) {
                processIdentified(originalClassType, classElement, eipOptions, prefix);
            } else if ("RouteDefinition".equals(classElement.name().local())) {
                processRoute(originalClassType, classElement, eipOptions, prefix);
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

    private boolean processAttribute(ClassInfo originalClassType,
                                     ClassInfo classElement,
                                     FieldInfo fieldElement,
                                     String fieldName,
                                     AnnotationInstance attribute,
                                     Set<EipOption> eipOptions,
                                     String prefix,
                                     String modelName) {
        String name = string(attribute, "name", null);
        if (isNullOrEmpty(name) || "##default".equals(name)) {
            name = fieldName;
        }

        // lets skip some unwanted attributes
        if (skipUnwanted) {
            // we want to skip inheritErrorHandler which is only applicable for the load-balancer
            boolean loadBalancer = "LoadBalanceDefinition".equals(originalClassType.name().local());
            if (!loadBalancer && "inheritErrorHandler".equals(name)) {
                return true;
            }
        }

        AnnotationInstance metadata = annotation(fieldElement, Metadata.class.getName()).orElse(null);

        name = prefix + name;
        String fieldTypeName = toString(fieldElement.type());
        ClassInfo fieldTypeElement = findTypeElement(fieldTypeName);

        String defaultValue = findDefaultValue(fieldElement, fieldTypeName);
        String docComment = findJavaDoc(fieldName, name, classElement, true);
        boolean required = bool(attribute, "required", false);
        // metadata may overrule element required
        required = findRequired(fieldElement, required);

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
                    .filter(PackageModelDocMojo::isEnumConstant)
                    .map(FieldInfo::name)
                    .forEach(enums::add);
        } else {
            isEnum = false;
        }

        String displayName = string(metadata, "displayName", (String) null);
        boolean deprecated = annotation(fieldElement, Deprecated.class.getName()).isPresent();
        String deprecationNote = null;
        if (metadata != null) {
            deprecationNote = string(metadata, "deprecationNote", (String) null);
        }

        EipOption ep = new EipOption(name, displayName, "attribute", fieldTypeName, required, defaultValue, docComment, deprecated, deprecationNote, isEnum, enums, false, null, false);
        eipOptions.add(ep);

        return false;
    }

    private void processValue(ClassInfo originalClassType,
                              ClassInfo classElement,
                              FieldInfo fieldElement,
                              String fieldName,
                              AnnotationInstance value,
                              Set<EipOption> eipOptions,
                              String prefix,
                              String modelName) {
        

        // XmlValue has no name attribute
        String name = fieldName;

        if ("method".equals(modelName) || "tokenize".equals(modelName) || "xtokenize".equals(modelName)) {
            // skip expression attribute on these three languages as they are solely configured using attributes
            if ("expression".equals(name)) {
                return;
            }
        }

        name = prefix + name;
        String fieldTypeName = toString(fieldElement.type());

        String defaultValue = findDefaultValue(fieldElement, fieldTypeName);
        String docComment = findJavaDoc(fieldName, name, classElement, true);
        boolean required = true;
        // metadata may overrule element required
        required = findRequired(fieldElement, required);

        AnnotationInstance metadata = annotation(fieldElement, Metadata.class.getName()).orElse(null);
        String displayName = string(metadata, "displayName", null);
        boolean deprecated = annotation(fieldElement, Deprecated.class.getName()).isPresent();
        String deprecationNote = string(metadata, "deprecationNote", null);

        EipOption ep = new EipOption(name, displayName, "value", fieldTypeName, required, defaultValue, docComment, deprecated, deprecationNote, false, null, false, null, false);
        eipOptions.add(ep);
    }

    private void processElement(ClassInfo classElement,
                                AnnotationInstance element,
                                FieldInfo fieldElement,
                                Set<EipOption> eipOptions,
                                String prefix,
                                boolean core) {
        String fieldName = fieldElement.name();
        if (element != null) {

            AnnotationInstance metadata = annotation(fieldElement, Metadata.class.getName()).orElse(null);

            String kind = "element";
            String name = string(element, "name", null);
            if (isNullOrEmpty(name) || "##default".equals(name)) {
                name = fieldName;
            }
            name = prefix + name;
            String fieldTypeName = toString(fieldElement.type());
            ClassInfo fieldTypeElement = findTypeElement(fieldTypeName);

            String defaultValue = findDefaultValue(fieldElement, fieldTypeName);
            String docComment = findJavaDoc(fieldName, name, classElement, true);
            boolean required = bool(element, "required", false);
            // metadata may overrule element required
            required = findRequired(fieldElement, required);

            // is it used as predicate (check field first and then fallback to its class)
            boolean asPredicate = core
                    && (annotation(fieldElement, AsPredicate.class.getName()).isPresent()
                        || annotation(classElement, AsPredicate.class.getName()).isPresent());

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
                        .filter(PackageModelDocMojo::isEnumConstant)
                        .map(FieldInfo::name)
                        .forEach(enums::add);
            } else {
                isEnum = false;
            }

            // gather oneOf expression/predicates which uses language
            Set<String> oneOfTypes = new TreeSet<>();
            if (core) {
                if (ONE_OF_TYPE_NAME.equals(fieldTypeName)) {
                    // okay its actually an language expression, so favor using that in the eip option
                    kind = "expression";
                    for (String language : ONE_OF_LANGUAGES) {
                        fieldTypeName = language;
                        ClassInfo languages = findTypeElement(language);
                        if (languages != null) {
                            // find all classes that has that superClassName
                            for (ClassInfo child : index.getAllKnownSubclasses(languages.name())) {
                                AnnotationInstance rootElement = annotation(child, XmlRootElement.class.getName()).orElse(null);
                                if (rootElement != null) {
                                    String childName = string(rootElement, "name", "##default");
                                    oneOfTypes.add(childName);
                                }
                            }
                        }
                    }
                }
                // special for otherwise as we want to indicate that the element is
                else if ("otherwise".equals(name)) {
                    oneOfTypes.add("otherwise");
                }
            }
            else {
                if (fieldTypeName.endsWith("Definition") || fieldTypeName.endsWith("FactoryBean")) {
                    ClassInfo definitionClass = findTypeElement(fieldTypeElement.asType().toString());
                    if (definitionClass != null) {
                        AnnotationInstance rootElement = annotation(definitionClass, XmlRootElement.class.getName()).orElse(null);
                        if (rootElement != null) {
                            String childName = string(rootElement, "name", null);
                            if (childName != null) {
                                oneOfTypes.add(childName);
                            }
                        }
                    }
                } else if (fieldTypeName.endsWith("Definition>") || fieldTypeName.endsWith("FactoryBean>")) {
                    // its a list so we need to load the generic type
                    String typeName = Strings.between(fieldTypeName, "<", ">");
                    ClassInfo definitionClass = findTypeElement(typeName);
                    if (definitionClass != null) {
                        AnnotationInstance rootElement = annotation(definitionClass, XmlRootElement.class.getName()).orElse(null);
                        if (rootElement != null) {
                            String childName = string(rootElement, "name", null);
                            if (childName != null) {
                                oneOfTypes.add(childName);
                            }
                        }
                    }
                }
            }
            boolean oneOf = !oneOfTypes.isEmpty();

            String displayName = null;
            if (metadata != null) {
                displayName = string(metadata, "displayName", null);
            }
            boolean deprecated = annotation(fieldElement, Deprecated.class.getName()).isPresent();
            String deprecationNote = null;
            if (metadata != null) {
                deprecationNote = string(metadata, "deprecationNote", null);
            }

            EipOption ep = new EipOption(name, displayName, kind, fieldTypeName, required, defaultValue, docComment, deprecated, deprecationNote, isEnum, enums, oneOf, oneOfTypes, asPredicate);
            eipOptions.add(ep);
        }
    }

    private void processElements(ClassInfo classElement,
                                 AnnotationInstance elements,
                                 FieldInfo fieldElement,
                                 Set<EipOption> eipOptions,
                                 String prefix) {
        

        String fieldName = fieldElement.name();
        if (elements != null) {
            String kind = "element";
            String name = fieldName;
            name = prefix + name;

            String fieldTypeName = toString(fieldElement.type());

            String defaultValue = findDefaultValue(fieldElement, fieldTypeName);
            String docComment = findJavaDoc(fieldName, name, classElement, true);

            boolean required = true;
            required = findRequired(fieldElement, required);

            // gather oneOf of the elements
            Set<String> oneOfTypes = new TreeSet<>();
            for (AnnotationInstance element : elements.value().asNestedArray()) {
                String child = string(element, "name", "##default");
                oneOfTypes.add(child);
            }

            AnnotationInstance metadata = annotation(fieldElement, Metadata.class.getName()).orElse(null);
            String displayName = string(metadata, "displayName", null);
            boolean deprecated = annotation(fieldElement, Deprecated.class.getName()).isPresent();
            String deprecationNote = string(metadata, "deprecationNote", null);

            EipOption ep = new EipOption(name, displayName, kind, fieldTypeName, required, defaultValue, docComment, deprecated, deprecationNote, false, null, true, oneOfTypes, false);
            eipOptions.add(ep);
        }
    }

    private void processRoute(ClassInfo originalClassType,
                              ClassInfo classElement,
                              Set<EipOption> eipOptions,
                              String prefix) {

        

        // group
        String docComment = findJavaDoc("group", null, classElement, true);
        EipOption ep = new EipOption("group", "Group", "attribute", "java.lang.String", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // group
        docComment = findJavaDoc("streamCache", null, classElement, true);
        ep = new EipOption("streamCache", "Stream Cache", "attribute", "java.lang.String", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // trace
        docComment = findJavaDoc("trace", null, classElement, true);
        ep = new EipOption("trace", "Trace", "attribute", "java.lang.String", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // message history
        docComment = findJavaDoc("messageHistory", null, classElement, true);
        ep = new EipOption("messageHistory", "Message History", "attribute", "java.lang.String", false, "true", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // log mask
        docComment = findJavaDoc("logMask", null, classElement, true);
        ep = new EipOption("logMask", "Log Mask", "attribute", "java.lang.String", false, "false", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // trace
        docComment = findJavaDoc("handleFault", null, classElement, true);
        ep = new EipOption("handleFault", "Handle Fault", "attribute", "java.lang.String", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // delayer
        docComment = findJavaDoc("delayer", null, classElement, true);
        ep = new EipOption("delayer", "Delayer", "attribute", "java.lang.String", false, "", docComment, false,
                null, false, null, false, null, false);
        eipOptions.add(ep);

        // autoStartup
        docComment = findJavaDoc("autoStartup", null, classElement, true);
        ep = new EipOption("autoStartup", "Auto Startup", "attribute", "java.lang.String", false, "true", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // startupOrder
        docComment = findJavaDoc("startupOrder", null, classElement, true);
        ep = new EipOption("startupOrder", "Startup Order", "attribute", "java.lang.Integer", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // errorHandlerRef
        docComment = findJavaDoc("errorHandlerRef", null, classElement, true);
        ep = new EipOption("errorHandlerRef", "Error Handler", "attribute", "java.lang.String", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // routePolicyRef
        docComment = findJavaDoc("routePolicyRef", null, classElement, true);
        ep = new EipOption("routePolicyRef", "Route Policy", "attribute", "java.lang.String", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // shutdownRoute
        Set<String> enums = new LinkedHashSet<>();
        enums.add("Default");
        enums.add("Defer");
        docComment = findJavaDoc("shutdownRoute", "Default", classElement, true);
        ep = new EipOption("shutdownRoute", "Shutdown Route", "attribute", "org.apache.camel.ShutdownRoute", false, "", docComment,
                false, null, true, enums, false, null, false);
        eipOptions.add(ep);

        // shutdownRunningTask
        enums = new LinkedHashSet<>();
        enums.add("CompleteCurrentTaskOnly");
        enums.add("CompleteAllTasks");
        docComment = findJavaDoc("shutdownRunningTask", "CompleteCurrentTaskOnly", classElement, true);
        ep = new EipOption("shutdownRunningTask", "Shutdown Running Task", "attribute", "org.apache.camel.ShutdownRunningTask", false, "", docComment,
                false, null, true, enums, false, null, false);
        eipOptions.add(ep);

        // inputs
        Set<String> oneOfTypes = new TreeSet<>();
        oneOfTypes.add("from");
        docComment = findJavaDoc("inputs", null, classElement, true);
        ep = new EipOption("inputs", "Inputs", "element", "java.util.List<org.apache.camel.model.FromDefinition>", true, "", docComment,
                false, null, false, null, true, oneOfTypes, false);
        eipOptions.add(ep);

        // outputs
        // gather oneOf which extends any of the output base classes
        oneOfTypes = new TreeSet<>();
        // find all classes that has that superClassName
        Set<ClassInfo> children = new LinkedHashSet<>();
        for (String superclass : ONE_OF_OUTPUTS) {
            findTypeElementChildren(children, superclass);
        }
        for (ClassInfo child : children) {
            AnnotationInstance rootElement = annotation(child, XmlRootElement.class.getName()).orElse(null);
            if (rootElement != null) {
                String childName = string(rootElement, "name", "##default");
                oneOfTypes.add(childName);
            }
        }

        // remove some types which are not intended as an output in eips
        oneOfTypes.remove("route");

        docComment = findJavaDoc("outputs", null, classElement, true);
        ep = new EipOption("outputs", "Outputs", "element", "java.util.List<org.apache.camel.model.ProcessorDefinition<?>>", true, "", docComment,
                false, null, false, null, true, oneOfTypes, false);
        eipOptions.add(ep);
    }

    /**
     * Special for process the OptionalIdentifiedDefinition
     */
    private void processIdentified(ClassInfo originalClassType, ClassInfo classElement,
                                   Set<EipOption> eipOptions, String prefix) {

        

        // id
        String docComment = findJavaDoc("id", null, classElement, true);
        EipOption ep = new EipOption("id", "Id", "attribute", "java.lang.String", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // description
        docComment = findJavaDoc("description", null, classElement, true);
        ep = new EipOption("description", "Description", "element", "org.apache.camel.model.DescriptionDefinition", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // lets skip custom id as it has no value for end users to configure
        if (!skipUnwanted) {
            // custom id
            docComment = findJavaDoc("customId", null, classElement, true);
            ep = new EipOption("customId", "Custom Id", "attribute", "java.lang.String", false, "", docComment,
                    false, null, false, null, false, null, false);
            eipOptions.add(ep);
        }
    }

    /**
     * Special for processing an @XmlElementRef routes field
     */
    private void processRoutes(ClassInfo originalClassType, AnnotationInstance elementRef,
                               FieldInfo fieldElement, String fieldName, Set<EipOption> eipOptions, String prefix) {
        if ("routes".equals(fieldName)) {

            String fieldTypeName = toString(fieldElement.type());

            Set<String> oneOfTypes = new TreeSet<>();
            oneOfTypes.add("route");

            EipOption ep = new EipOption("routes", "Routes", "element", fieldTypeName, false, "", "Contains the Camel routes",
                    false, null, false, null, true, oneOfTypes, false);
            eipOptions.add(ep);
        }
    }

    /**
     * Special for processing an @XmlElementRef rests field
     */
    private void processRests(ClassInfo originalClassType, AnnotationInstance elementRef,
                              FieldInfo fieldElement, String fieldName, Set<EipOption> eipOptions, String prefix) {
        if ("rests".equals(fieldName)) {

            String fieldTypeName = toString(fieldElement.type());

            Set<String> oneOfTypes = new TreeSet<>();
            oneOfTypes.add("rest");

            EipOption ep = new EipOption("rests", "Rests", "element", fieldTypeName, false, "", "Contains the rest services defined using the rest-dsl",
                    false, null, false, null, true, oneOfTypes, false);
            eipOptions.add(ep);
        }
    }

    /**
     * Special for processing an @XmlElementRef outputs field
     */
    private void processOutputs(ClassInfo originalClassType, AnnotationInstance elementRef,
                                FieldInfo fieldElement, String fieldName, Set<EipOption> eipOptions, String prefix) {
        if ("outputs".equals(fieldName) && supportOutputs(originalClassType)) {
            String kind = "element";
            String name = string(elementRef, "name", null);
            if (isNullOrEmpty(name) || "##default".equals(name)) {
                name = fieldName;
            }
            name = prefix + name;
            String fieldTypeName = toString(fieldElement.type());

            // gather oneOf which extends any of the output base classes
            Set<String> oneOfTypes = new TreeSet<>();
            // find all classes that has that superClassName
            Set<ClassInfo> children = new LinkedHashSet<>();
            for (String superclass : ONE_OF_OUTPUTS) {
                findTypeElementChildren(children, superclass);
            }
            for (ClassInfo child : children) {
                AnnotationInstance rootElement = annotation(child, XmlRootElement.class.getName()).orElse(null);
                if (rootElement != null) {
                    String childName = string(rootElement, "name", "##default");
                    oneOfTypes.add(childName);
                }
            }

            // remove some types which are not intended as an output in eips
            oneOfTypes.remove("route");

            AnnotationInstance metadata = annotation(fieldElement, Metadata.class.getName()).orElse(null);
            String displayName = string(metadata, "displayName", null);
            boolean deprecated = annotation(fieldElement, Deprecated.class.getName()).isPresent();
            String deprecationNote = string(metadata, "deprecationNote", null);

            EipOption ep = new EipOption(name, displayName, kind, fieldTypeName, true, "", "", deprecated, deprecationNote, false, null, true, oneOfTypes, false);
            eipOptions.add(ep);
        }
    }

    /**
     * Special for processing an @XmlElementRef verbs field (rest-dsl)
     */
    private void processVerbs(ClassInfo originalClassType, AnnotationInstance elementRef,
                              FieldInfo fieldElement, String fieldName, Set<EipOption> eipOptions, String prefix) {

        

        if ("verbs".equals(fieldName) && supportOutputs(originalClassType)) {
            String kind = "element";
            String name = string(elementRef, "name", null);
            if (isNullOrEmpty(name) || "##default".equals(name)) {
                name = fieldName;
            }
            name = prefix + name;
            String fieldTypeName = toString(fieldElement.type());

            String docComment = findJavaDoc(fieldName, name, originalClassType, true);

            // gather oneOf which extends any of the output base classes
            Set<String> oneOfTypes = new TreeSet<>();
            // find all classes that has that superClassName
            Set<ClassInfo> children = new LinkedHashSet<>();
            for (String superclass : ONE_OF_OUTPUTS) {
                findTypeElementChildren(children, superclass);
            }
            for (ClassInfo child : children) {
                AnnotationInstance rootElement = annotation(child, XmlRootElement.class.getName()).orElse(null);
                if (rootElement != null) {
                    String childName = string(rootElement, "name", "##default");
                    oneOfTypes.add(childName);
                }
            }

            AnnotationInstance metadata = annotation(fieldElement, Metadata.class.getName()).orElse(null);
            String displayName = string(metadata, "displayName", null);
            boolean deprecated = annotation(fieldElement, Deprecated.class.getName()).isPresent();
            String deprecationNote = string(metadata, "deprecationNote", null);

            EipOption ep = new EipOption(name, displayName, kind, fieldTypeName, true, "", docComment, deprecated, deprecationNote, false, null, true, oneOfTypes, false);
            eipOptions.add(ep);
        }
    }

    /**
     * Special for processing an @XmlElementRef expression field
     */
    private void processRefExpression(ClassInfo originalClassType, ClassInfo classElement,
                                      AnnotationInstance elementRef, FieldInfo fieldElement,
                                      String fieldName, Set<EipOption> eipOptions, String prefix) {
        

        if ("expression".equals(fieldName)) {
            String kind = "expression";
            String name = string(elementRef, "name", null);
            if (isNullOrEmpty(name) || "##default".equals(name)) {
                name = fieldName;
            }
            name = prefix + name;
            String fieldTypeName = toString(fieldElement.type());

            // find javadoc from original class as it will override the setExpression method where we can provide the javadoc for the given EIP
            String docComment = findJavaDoc(fieldName, name, originalClassType, true);

            // is it used as predicate (check field first and then fallback to its class / original class)
            boolean asPredicate =
                    annotation(fieldElement, AsPredicate.class.getName()).isPresent()
                    || annotation(classElement, AsPredicate.class.getName()).isPresent()
                    || annotation(originalClassType, AsPredicate.class.getName()).isPresent();

            // gather oneOf expression/predicates which uses language
            Set<String> oneOfTypes = new TreeSet<>();
            for (String language : ONE_OF_LANGUAGES) {
                ClassInfo languages = findTypeElement(language);
                // find all classes that has that superClassName
                for (ClassInfo child : index.getAllKnownSubclasses(languages.name())) {
                    AnnotationInstance rootElement = annotation(child, XmlRootElement.class.getName()).orElse(null);
                    if (rootElement != null) {
                        String childName = string(rootElement, "name", "##default");
                        oneOfTypes.add(childName);
                    }
                }
            }

            AnnotationInstance metadata = annotation(fieldElement, Metadata.class.getName()).orElse(null);
            String displayName = string(metadata, "displayName", null);
            boolean deprecated = annotation(fieldElement, Deprecated.class.getName()).isPresent();
            String deprecationNote = string(metadata, "deprecationNote", null);

            EipOption ep = new EipOption(name, displayName, kind, fieldTypeName, true, "", docComment, deprecated, deprecationNote, false, null, true, oneOfTypes, asPredicate);
            eipOptions.add(ep);
        }
    }

    /**
     * Special for processing an @XmlElementRef when field
     */
    private void processRefWhenClauses(ClassInfo originalClassType, AnnotationInstance elementRef,
                                       FieldInfo fieldElement, String fieldName, Set<EipOption> eipOptions, String prefix) {
        if ("whenClauses".equals(fieldName)) {
            String kind = "element";
            String name = string(elementRef, "name", "");
            if (isNullOrEmpty(name) || "##default".equals(name)) {
                name = fieldName;
            }
            name = prefix + name;
            String fieldTypeName = toString(fieldElement.type());

            // find javadoc from original class as it will override the setExpression method where we can provide the javadoc for the given EIP
            String docComment = findJavaDoc(fieldName, name, originalClassType, true);

            // indicate that this element is one of when
            Set<String> oneOfTypes = new HashSet<>();
            oneOfTypes.add("when");

            // when is predicate
            boolean asPredicate = true;

            AnnotationInstance metadata = annotation(fieldElement, Metadata.class.getName()).orElse(null);
            String displayName = string(metadata, "displayName", null);
            boolean deprecated = annotation(fieldElement, Deprecated.class.getName()).isPresent();
            String deprecationNote = string(metadata, "deprecationNote", null);

            EipOption ep = new EipOption(name, displayName, kind, fieldTypeName, false, "", docComment, deprecated, deprecationNote, false, null, true, oneOfTypes, asPredicate);
            eipOptions.add(ep);
        }
    }

    private void processSpringElement(ClassInfo originalClassType, ClassInfo classElement, AnnotationInstance elementRef, FieldInfo fieldElement, String fieldName, Set<EipOption> eipOptions, String prefix) {

    }

    /**
     * Whether the class supports outputs.
     * <p/>
     * There are some classes which does not support outputs, even though they have a outputs element.
     */
    private boolean supportOutputs(ClassInfo classElement) {
        String superclass = classElement.superName().toString();
        return !"org.apache.camel.model.NoOutputExpressionNode".equals(superclass);
    }

    private String findDefaultValue(FieldInfo fieldElement, String fieldTypeName) {
        AnnotationInstance metadata = annotation(fieldElement, Metadata.class.getName()).orElse(null);
        String defaultValue = string(metadata, "defaultValue", null);
        if (isNullOrEmpty(defaultValue)) {
            // if its a boolean type, then we use false as the default
            if ("boolean".equals(fieldTypeName) || "java.lang.Boolean".equals(fieldTypeName)) {
                defaultValue = "false";
            }
        }
        return defaultValue;
    }

    private boolean findRequired(FieldInfo fieldElement, boolean defaultValue) {
        AnnotationInstance ai = annotation(fieldElement, Metadata.class.getName()).orElse(null);
        return Boolean.parseBoolean(string(ai, "required", Boolean.toString(defaultValue)));
    }

    /**
     * Capitializes the name as a title
     *
     * @param name the name
     * @return as a title
     */
    private static String asTitle(String name) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            boolean upper = Character.isUpperCase(c);
            boolean first = sb.length() == 0;
            if (first) {
                sb.append(Character.toUpperCase(c));
            } else if (upper) {
                sb.append(' ');
                sb.append(c);
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString().trim();
    }

    private boolean hasInput(ClassInfo classElement) {
        for (String name : ONE_OF_INPUTS) {
            if (hasSuperClass(classElement, name)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOutput(EipModel model, Set<EipOption> options) {
        // if we are route/rest then we accept output
        if ("route".equals(model.getName()) || "rest".equals(model.getName())) {
            return true;
        }

        // special for transacted/policy which should not have output
        if ("policy".equals(model.getName()) || "transacted".equals(model.getName())) {
            return false;
        }

        for (EipOption option : options) {
            if ("outputs".equals(option.getName())) {
                return true;
            }
        }
        return false;
    }

    private static final class EipModel {

        private String name;
        private String title;
        private String javaType;
        private String label;
        private String description;
        private boolean deprecated;
        private String deprecationNote;
        private boolean input;
        private boolean output;
        private String firstVersion;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getJavaType() {
            return javaType;
        }

        public void setJavaType(String javaType) {
            this.javaType = javaType;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isDeprecated() {
            return deprecated;
        }

        public void setDeprecated(boolean deprecated) {
            this.deprecated = deprecated;
        }

        public String getDeprecationNote() {
            return deprecationNote;
        }

        public void setDeprecationNote(String deprecationNote) {
            this.deprecationNote = deprecationNote;
        }

        public boolean isInput() {
            return input;
        }

        public void setInput(boolean input) {
            this.input = input;
        }

        public String getInput() {
            return input ? "true" : "false";
        }

        public boolean isOutput() {
            return output;
        }

        public void setOutput(boolean output) {
            this.output = output;
        }

        public String getOutput() {
            return output ? "true" : "false";
        }

        public String getFirstVersion() {
            return firstVersion;
        }

        public void setFirstVersion(String firstVersion) {
            this.firstVersion = firstVersion;
        }
    }

    private static final class EipOption {

        private String name;
        private String displayName;
        private String kind;
        private String type;
        private boolean required;
        private String defaultValue;
        private String documentation;
        private boolean deprecated;
        private String deprecationNode;
        private boolean enumType;
        private Set<String> enums;
        private boolean oneOf;
        private Set<String> oneOfTypes;
        private boolean asPredicate;

        private EipOption(String name, String displayName, String kind, String type, boolean required, String defaultValue, String documentation,
                          boolean deprecated, String deprecationNode, boolean enumType, Set<String> enums, boolean oneOf, Set<String> oneOfTypes, boolean asPredicate) {
            this.name = name;
            this.displayName = displayName;
            this.kind = kind;
            this.type = type;
            this.required = required;
            this.defaultValue = defaultValue;
            this.documentation = documentation;
            this.deprecated = deprecated;
            this.deprecationNode = deprecationNode;
            this.enumType = enumType;
            this.enums = enums;
            this.oneOf = oneOf;
            this.oneOfTypes = oneOfTypes;
            this.asPredicate = asPredicate;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getKind() {
            return kind;
        }

        public String getType() {
            return type;
        }

        public boolean isRequired() {
            return required;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public String getDocumentation() {
            return documentation;
        }

        public boolean isDeprecated() {
            return deprecated;
        }

        public String getDeprecationNode() {
            return deprecationNode;
        }

        public boolean isEnumType() {
            return enumType;
        }

        public Set<String> getEnums() {
            return enums;
        }

        public boolean isOneOf() {
            return oneOf;
        }

        public Set<String> getOneOfTypes() {
            return oneOfTypes;
        }

        public boolean isAsPredicate() {
            return asPredicate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            EipOption that = (EipOption) o;

            if (!name.equals(that.name)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    private static final class EipOptionComparator implements Comparator<EipOption> {

        private final EipModel model;

        private EipOptionComparator(EipModel model) {
            this.model = model;
        }

        @Override
        public int compare(EipOption o1, EipOption o2) {
            int weigth = weigth(o1);
            int weigth2 = weigth(o2);

            if (weigth == weigth2) {
                // keep the current order
                return 1;
            } else {
                // sort according to weight
                return weigth2 - weigth;
            }
        }

        private int weigth(EipOption o) {
            String name = o.getName();

            // these should be first
            if ("expression".equals(name)) {
                return 10;
            }

            // these should be last
            if ("description".equals(name)) {
                return -10;
            } else if ("id".equals(name)) {
                return -9;
            } else if ("pattern".equals(name) && "to".equals(model.getName())) {
                // and pattern only for the to model
                return -8;
            }
            return 0;
        }
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

    static final int ENUM      = 0x00004000;

    private static boolean isEnumClass(ClassInfo clazz) {
        return clazz != null && (clazz.flags() & ENUM) != 0;
    }

    private static boolean isEnumConstant(FieldInfo field) {
        return (field.flags() & ENUM) != 0;
    }

    private ClassInfo findTypeElement(String javaType) {
        return findTypeElement(DotName.createSimple(javaType));
    }

    private ClassInfo findTypeElement(DotName javaType) {
        return index.getClassByName(javaType);
    }

    void findTypeElementChildren(Set<ClassInfo> found, String superClassName) {
        found.addAll(index.getAllKnownSubclasses(DotName.createSimple(superClassName)));
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

    public boolean hasSuperClass(ClassInfo classElement, String superClassName) {
        String aRootName = canonicalClassName(classElement.name().toString());

        // do not check the classes from JDK itself
        if (isNullOrEmpty(aRootName) || aRootName.startsWith("java.") || aRootName.startsWith("javax.")) {
            return false;
        }

        String aSuperClassName = classElement.superName() != null
                ? canonicalClassName(classElement.superName().toString()) : null;
        if (superClassName.equals(aSuperClassName)) {
            return true;
        }

        ClassInfo aSuperClass = findTypeElement(aSuperClassName);
        if (aSuperClass != null) {
            return hasSuperClass(aSuperClass, superClassName);
        } else {
            return false;
        }
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
