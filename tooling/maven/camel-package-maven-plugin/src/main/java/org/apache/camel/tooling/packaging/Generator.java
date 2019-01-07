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
package org.apache.camel.tooling.packaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.lang.model.type.DeclaredType;
import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.thoughtworks.qdox.library.SourceLibrary;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaSource;
import org.apache.camel.tooling.packaging.helpers.DocumentationHelper;
import org.apache.camel.tooling.packaging.helpers.EndpointHelper;
import org.apache.camel.tooling.packaging.helpers.IOHelper;
import org.apache.camel.tooling.packaging.helpers.JSonSchemaHelper;
import org.apache.camel.tooling.packaging.helpers.JandexHelper;
import org.apache.camel.tooling.packaging.helpers.PackageHelper;
import org.apache.camel.tooling.packaging.helpers.StringHelper;
import org.apache.camel.tooling.packaging.model.ComponentModel;
import org.apache.camel.tooling.packaging.model.ComponentOptionModel;
import org.apache.camel.tooling.packaging.model.DataFormatModel;
import org.apache.camel.tooling.packaging.model.EipModel;
import org.apache.camel.tooling.packaging.model.EipOptionModel;
import org.apache.camel.tooling.packaging.model.EndpointOptionModel;
import org.apache.camel.tooling.packaging.model.LanguageModel;
import org.apache.camel.tooling.packaging.model.OtherModel;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import static org.apache.camel.tooling.packaging.helpers.JSonSchemaHelper.getSafeBool;
import static org.apache.camel.tooling.packaging.helpers.JSonSchemaHelper.sanitizeDescription;
import static org.apache.camel.tooling.packaging.helpers.JandexHelper.annotation;
import static org.apache.camel.tooling.packaging.helpers.JandexHelper.bool;
import static org.apache.camel.tooling.packaging.helpers.JandexHelper.isEnumClass;
import static org.apache.camel.tooling.packaging.helpers.JandexHelper.string;
import static org.apache.camel.tooling.packaging.helpers.Strings.after;
import static org.apache.camel.tooling.packaging.helpers.Strings.between;
import static org.apache.camel.tooling.packaging.helpers.Strings.canonicalClassName;
import static org.apache.camel.tooling.packaging.helpers.Strings.getOrElse;
import static org.apache.camel.tooling.packaging.helpers.Strings.isNullOrEmpty;
import static org.apache.camel.tooling.packaging.helpers.Strings.safeNull;
import static org.w3c.dom.Node.ELEMENT_NODE;

public abstract class Generator {

    public static final DotName COMPONENT = DotName.createSimple("org.apache.camel.spi.annotations.Component");
    public static final DotName LANGUAGE = DotName.createSimple("org.apache.camel.spi.annotations.Language");
    public static final DotName DATAFORMAT = DotName.createSimple("org.apache.camel.spi.annotations.Dataformat");
    public static final DotName COMPONENT_SERVICE_FACTORY = DotName.createSimple("org.apache.camel.spi.annotations.ComponentServiceFactory");
    public static final DotName CLOUD_SERVICE_FACTORY = DotName.createSimple("org.apache.camel.spi.annotations.CloudServiceFactory");
    public static final DotName SEND_DYNAMIC = DotName.createSimple("org.apache.camel.spi.annotations.SendDynamic");
    public static final DotName AS_PREDICATE = DotName.createSimple("org.apache.camel.spi.AsPredicate");
    public static final DotName METADATA = DotName.createSimple("org.apache.camel.spi.Metadata");
    public static final DotName URI_ENDPOINT = DotName.createSimple("org.apache.camel.spi.UriEndpoint");
    public static final DotName URI_PARAM = DotName.createSimple("org.apache.camel.spi.UriParam");
    public static final DotName URI_PARAMS = DotName.createSimple("org.apache.camel.spi.UriParams");
    public static final DotName URI_PATH = DotName.createSimple("org.apache.camel.spi.UriPath");
    public static final DotName ASYNC_ENDPOINT = DotName.createSimple("org.apache.camel.AsyncEndpoint");
    public static final DotName CONVERTER = DotName.createSimple("org.apache.camel.Converter");
    public static final DotName FALLBACK_CONVERTER = DotName.createSimple("org.apache.camel.FallbackConverter");
    public static final DotName DEPRECATED = DotName.createSimple(Deprecated.class.getName());
    public static final DotName XML_ATTRIBUTE = DotName.createSimple(XmlAttribute.class.getName());
    public static final DotName XML_ENUM = DotName.createSimple(XmlEnum.class.getName());
    public static final DotName XML_ELEMENTS = DotName.createSimple(XmlElements.class.getName());
    public static final DotName XML_ELEMENT = DotName.createSimple(XmlElement.class.getName());
    public static final DotName XML_ELEMENT_REF = DotName.createSimple(XmlElementRef.class.getName());
    public static final DotName XML_ROOT_ELEMENT = DotName.createSimple(XmlRootElement.class.getName());
    public static final DotName XML_VALUE = DotName.createSimple(XmlValue.class.getName());

    public static final String GENERATED_MSG = "Generated by camel build tools";
    public static final String META_INF_SERVICES_ORG_APACHE_CAMEL = "META-INF/services/org/apache/camel/";
    public static final String NL = "\n";

    public static final String AP_ALTERNATIVE_SYNTAX = "alternativeSyntax";
    public static final String AP_CONSUMER_ONLY = "consumerOnly";
    public static final String AP_DEPRECATED = "deprecated";
    public static final String AP_DEPRECATION_NOTE = "deprecationNote";
    public static final String AP_DESCRIPTION = "description";
    public static final String AP_DISPLAY_NAME = "displayName";
    public static final String AP_DEFAULT_VALUE = "defaultValue";
    public static final String AP_DEFAULT_VALUE_NOTE = "defaultValueNote";
    public static final String AP_ENUMS = "enums";
    public static final String AP_EXCLUDE_PROPERTIES = "excludeProperties";
    public static final String AP_EXTENDS_SCHEME = "extendsScheme";
    public static final String AP_FIRST_VERSION = "firstVersion";
    public static final String AP_JAVA_TYPE = "javaType";
    public static final String AP_LABEL = "label";
    public static final String AP_LENIENT_PROPERTIES = "lenientProperties";
    public static final String AP_MULTI_VALUE = "multiValue";
    public static final String AP_NAME = "name";
    public static final String AP_OPTIONAL_PREFIX = "optionalPrefix";
    public static final String AP_PREFIX = "prefix";
    public static final String AP_PRODUCER_ONLY = "producerOnly";
    public static final String AP_REQUIRED = "required";
    public static final String AP_SCHEME = "scheme";
    public static final String AP_SYNTAX = "syntax";
    public static final String AP_TITLE = "title";
    public static final String AP_SECRET = "secret";

    private static final String HEADER_FILTER_STRATEGY_JAVADOC = "To use a custom HeaderFilterStrategy to filter header to and from Camel message.";

    final SourceLibrary sourceLibrary = new SourceLibrary(null);
    IndexView index;


    public void prepareLegal(Path legalOutDir) {
        // Only take care about camel legal stuff
        if (!"org.apache.camel".equals(getGroupId())) {
            return;
        }
        boolean hasLicense = getResources().stream()
                .map(Paths::get)
                .map(p -> p.resolve("META-INF/LICENSE.txt"))
                .anyMatch(Files::isRegularFile);
        if (!hasLicense) {
            try (InputStream isLicense = getResourceAsStream("/camel-LICENSE.txt")) {
                String license = IOUtils.toString(isLicense, StandardCharsets.UTF_8);
                updateResource(legalOutDir.resolve("META-INF/LICENSE.txt"), license);
            } catch (IOException e) {
                throw new IOError(e);
            }
        }
        boolean hasNotice = getResources().stream()
                .map(Paths::get)
                .map(p -> p.resolve("META-INF/NOTICE.txt"))
                .anyMatch(Files::isRegularFile);
        if (!hasNotice) {
            try (InputStream isNotice = getResourceAsStream("/camel-NOTICE.txt")) {
                String notice = IOUtils.toString(isNotice, StandardCharsets.UTF_8);
                updateResource(legalOutDir.resolve("META-INF/NOTICE.txt"), notice);
            } catch (IOException e) {
                throw new IOError(e);
            }
        }
        addResourceDirectory(legalOutDir);
    }

    private InputStream getResourceAsStream(String path) throws IOException {
        if (path.charAt(0) != '/') {
            path = "/" + path;
        }
        URL url = getClass().getResource(path);
        if (url.toString().startsWith("jar:")) {
            String p = new URL(url.getPath()).getPath();
            ZipFile f = new ZipFile(p.substring(0, p.indexOf('!')));
            ZipEntry e = f.getEntry(path.substring(1));
            return f.getInputStream(e);
        }
        throw new FileNotFoundException(path);
    }

    public void prepareServices(Path serviceOutDir) {
        Path camelMetaDir = serviceOutDir.resolve(META_INF_SERVICES_ORG_APACHE_CAMEL);

        IndexView index = getIndex();
        Stream.of(COMPONENT, LANGUAGE, DATAFORMAT)
                .map(index::getAnnotations)
                .flatMap(Collection::stream)
                .filter(this::isLocalClass)
                .forEach(ai -> {
                    String names = ai.value().asString();
                    for (String name : names.split(",")) {
                        Path out = camelMetaDir.resolve(ai.name().local().toLowerCase()).resolve(name);
                        String clazz = ai.target().asClass().name().toString();
                        StringBuilder sb = new StringBuilder();
                        sb.append("# " + GENERATED_MSG + NL);
                        sb.append("class=").append(clazz).append(NL);
                        ai.target().asClass().classAnnotations().forEach(ani -> {
                            ClassInfo annotationClass = findTypeElement(ani.name());
                            AnnotationInstance factory = annotation(annotationClass, COMPONENT_SERVICE_FACTORY).orElse(null);
                            if (factory != null) {
                                String key = factory.value().asString();
                                String val = ani.value().asClass().asClassType().name().toString();
                                sb.append(key).append(".class=").append(val).append(NL);
                            }
                        });
                        String data = sb.toString();
                        updateResource(out, data);
                    }
                });

        index.getAnnotations(CLOUD_SERVICE_FACTORY).stream()
                .filter(this::isLocalClass)
                .forEach(ai -> {
                    String names = ai.value().asString();
                    for (String name : names.split(",")) {
                        Path out = camelMetaDir.resolve("cloud").resolve(name);
                        String clazz = ai.target().asClass().name().toString();
                        String data = "# " + GENERATED_MSG + NL
                                + "class=" + clazz + NL;
                        updateResource(out, data);
                    }
                });

        index.getAnnotations(SEND_DYNAMIC).stream()
                .filter(this::isLocalClass)
                .forEach(ai -> {
                    String names = ai.value().asString();
                    for (String name : names.split(",")) {
                        Path out = camelMetaDir.resolve("cloud").resolve(name);
                        String clazz = ai.target().asClass().name().toString();
                        String data = "# " + GENERATED_MSG + NL
                                + "class=" + clazz + NL;
                        updateResource(out, data);
                    }
                });

        if (!"camel-core".equals(getArtifactId())) {
            String typeConverter = Stream.of(CONVERTER)
                    .map(index::getAnnotations)
                    .flatMap(Collection::stream)
                    .map(ai -> ai.target().kind() == Kind.METHOD
                            ? ai.target().asMethod().declaringClass().name().toString()
                            : ai.target().asClass().name().toString())
                    .filter(this::isLocalClass)
                    .sorted()
                    .distinct()
                    .collect(Collectors.joining(NL));
            if (isNullOrEmpty(typeConverter)) {
                updateResource(camelMetaDir.resolve("TypeConverter"), null);
            } else {
                String data = "# " + GENERATED_MSG + NL + typeConverter;
                updateResource(camelMetaDir.resolve("TypeConverter"), data);
            }
        }

        addResourceDirectory(serviceOutDir);
    }

    public void prepareLanguage(Path languageOutDir, Path schemaOutDir) {

        Map<String, String> languages = getResources().stream()
                .map(Paths::get)
                .map(p -> p.resolve(META_INF_SERVICES_ORG_APACHE_CAMEL + "language"))
                .filter(Files::isDirectory)
                .flatMap(IOHelper::list)
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().charAt(0) != '.')
                .collect(Collectors.toMap(
                        p -> p.getFileName().toString(),
                        DocumentationHelper::readClassFromCamelResource
                ));

        languages.forEach((name, javaType) -> {
            String modelName = asLanguageModelName(name);
            String json = getCamelCoreLocations().stream()
                    .map(IOHelper::asFolder)
                    .map(p -> p.resolve("org/apache/camel/model/language").resolve(modelName + ".json"))
                    .filter(Files::isRegularFile)
                    .findAny()
                    .map(path -> IOHelper.lines(path)
                            .filter(s -> !s.startsWith("//"))
                            .collect(Collectors.joining(NL)))
                    .orElseThrow(() -> new RuntimeException("Unable to find json for " + modelName));

            LanguageModel languageModel = new LanguageModel();
            languageModel.setName(name);
            languageModel.setTitle("");
            languageModel.setModelName(modelName);
            languageModel.setLabel("");
            languageModel.setDescription("");
            languageModel.setJavaType(javaType);
            languageModel.setGroupId(getGroupId());
            languageModel.setArtifactId(getArtifactId());
            languageModel.setVersion(getVersion());

            List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("model", json, false);
            for (Map<String, String> row : rows) {
                if (row.containsKey(AP_TITLE)) {
                    // title may be special for some
                    // languages
                    String title = asTitle(name, row.get(AP_TITLE));
                    languageModel.setTitle(title);
                }
                if (row.containsKey(AP_DESCRIPTION)) {
                    // description may be special for some
                    // languages
                    String desc = asDescription(name, row.get(AP_DESCRIPTION));
                    languageModel.setDescription(desc);
                }
                if (row.containsKey(AP_LABEL)) {
                    languageModel.setLabel(row.get(AP_LABEL));
                }
                if (row.containsKey(AP_DEPRECATED)) {
                    languageModel.setDeprecated(getSafeBool(AP_DEPRECATED, row));
                }
                if (row.containsKey(AP_DEPRECATION_NOTE)) {
                    languageModel.setDeprecationNote(row.get(AP_DEPRECATION_NOTE));
                }
                if (row.containsKey(AP_JAVA_TYPE)) {
                    languageModel.setModelJavaType(row.get(AP_JAVA_TYPE));
                }
                if (row.containsKey(AP_FIRST_VERSION)) {
                    languageModel.setFirstVersion(row.get(AP_FIRST_VERSION));
                }
            }
            debug("Model " + languageModel);

            // build json schema for the data format
            String properties = PackageHelper.after(json, "  \"properties\": {");
            String schema = createJsonSchema(languageModel, properties);
            debug("JSon schema\n" + schema);

            // write this to the directory
            Path dir = schemaOutDir.resolve(schemaSubDirectory(languageModel.getJavaType()));
            Path out = dir.resolve(name + ".json");
            updateResource(out, schema);
            if (isDebugEnabled()) {
                debug("Generated " + out + " containing JSon schema for " + name + " language");
            }
        });

        Path outFile = languageOutDir.resolve(META_INF_SERVICES_ORG_APACHE_CAMEL).resolve("language.properties");
        String data = null;
        if (!languages.isEmpty()) {
            String names = String.join(" ", languages.keySet());
            data = createProperties("languages", names);
            info("Generating " + outFile + " containing " + languages.size() + " Camel " + (languages.size() > 1 ? "languages: " : "language: ") + names);
        }
        updateResource(outFile, data);

        // first we need to setup the output directory because the next check
        // can stop the build before the end and eclipse always needs to know about that directory
        addResourceDirectory(languageOutDir);
        addResourceDirectory(schemaOutDir);
    }

    public void prepareDataFormat(Path dataFormatOutDir, Path schemaOutDir) {

        Map<String, String> dataformats = getResources().stream()
                .map(Paths::get)
                .map(p -> p.resolve(META_INF_SERVICES_ORG_APACHE_CAMEL + "dataformat"))
                .filter(Files::isDirectory)
                .flatMap(IOHelper::list)
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().charAt(0) != '.')
                .collect(Collectors.toMap(
                        p -> p.getFileName().toString(),
                        DocumentationHelper::readClassFromCamelResource
                ));

        dataformats.forEach((name, javaType) -> {
            String modelName = asDataformatModelName(name);
            String json = getCamelCoreLocations().stream()
                    .map(IOHelper::asFolder)
                    .map(p -> p.resolve("org/apache/camel/model/dataformat").resolve(modelName + ".json"))
                    .filter(Files::isRegularFile)
                    .findAny()
                    .map(path -> IOHelper.lines(path)
                            .filter(s -> !s.startsWith("//"))
                            .collect(Collectors.joining(NL)))
                    .orElseThrow(() -> new RuntimeException("Unable to find json for " + modelName));

            DataFormatModel dataFormatModel = extractDataFormatModel(json, modelName, name, javaType);
            debug("Model " + dataFormatModel);

            // build json schema for the data format
            String properties = PackageHelper.after(json, "  \"properties\": {");

            // special prepare for bindy/json properties
            properties = prepareBindyProperties(name, properties);
            properties = prepareJsonProperties(name, properties);

            String schema = createJsonSchema(dataFormatModel, properties);
            debug("JSon schema\n" + schema);

            // write this to the directory
            Path dir = schemaOutDir.resolve(schemaSubDirectory(dataFormatModel.getJavaType()));
            Path out = dir.resolve(name + ".json");
            updateResource(out, schema);
            if (isDebugEnabled()) {
                debug("Generated " + out + " containing JSon schema for " + name + " data format");
            }
        });


        Path outFile = dataFormatOutDir.resolve(META_INF_SERVICES_ORG_APACHE_CAMEL).resolve("dataformat.properties");
        String data = null;
        if (!dataformats.isEmpty()) {
            String names = String.join(" ", dataformats.keySet());
            data = createProperties("dataFormats", names);
            info("Generating " + outFile + " containing " + dataformats.size() + " Camel " + (dataformats.size() > 1 ? "dataformats: " : "dataformat: ") + names);
        }
        updateResource(outFile, data);

        // first we need to setup the output directory because the next check
        // can stop the build before the end and eclipse always needs to know about that directory
        addResourceDirectory(dataFormatOutDir);
        addResourceDirectory(schemaOutDir);
    }

    public void processJaxb(Path jaxbIndexOutDir) {
        Map<String, Set<String>> byPackage = new HashMap<>();

        IndexView index = getIndex();
        Stream.of(XML_ROOT_ELEMENT, XML_ENUM)
                .map(index::getAnnotations)
                .flatMap(Collection::stream)
                .map(AnnotationInstance::target)
                .map(AnnotationTarget::asClass)
                .map(ClassInfo::name)
                .map(DotName::toString)
                .forEach(name -> {
                    int idx = name.lastIndexOf('.');
                    String p = name.substring(0, idx);
                    String c = name.substring(idx + 1);
                    byPackage.computeIfAbsent(p, s -> new TreeSet<>()).add(c);
                });

        for (Map.Entry<String, Set<String>> entry : byPackage.entrySet()) {
            Path file = jaxbIndexOutDir.resolve(entry.getKey().replace('.', '/')).resolve("jaxb.index");
            StringBuilder sb = new StringBuilder();
            sb.append("# " + GENERATED_MSG + NL);
            for (String s : entry.getValue()) {
                sb.append(s);
                sb.append(NL);
            }
            updateResource(file, sb.toString());
        }

        addResourceDirectory(jaxbIndexOutDir);
    }

    public void prepareModel(Path modelOutDir) {

        List<String> models = getResources().stream()
                .map(Paths::get)
                .map(p -> p.resolve("org/apache/camel/model"))
                .filter(Files::isDirectory)
                .flatMap(IOHelper::walk)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(p -> p.endsWith(".json"))
                .map(p -> p.substring(0, p.length() - 5))
                .sorted()
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("# " + GENERATED_MSG + NL);
        for (String name : models) {
            sb.append(name).append(NL);
        }

        Path outFile = modelOutDir.resolve(META_INF_SERVICES_ORG_APACHE_CAMEL + "model.properties");
        updateResource(outFile, sb.toString());
        info("Generated " + outFile + " containing " + models.size() + " Camel models");

        addResourceDirectory(modelOutDir);
    }

    public void prepareKarafCatalog(Path featuresDir) {
        Set<String> features = loadFeatureNames(featuresDir);
//        executeComponents(features);
//        executeDataFormats(features);
//        executeLanguages(features);
//        executeOthers(features);
    }

    public IndexView getIndex() {
        if (index == null) {
            index = JandexHelper.createIndex(getClasspath());
        }
        return index;
    }

    // load features.xml file and parse it
    private Set<String> loadFeatureNames(Path featuresDir) {
        Set<String> answer = new LinkedHashSet<>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringComments(true);
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setNamespaceAware(false);
            dbf.setValidating(false);
            dbf.setXIncludeAware(false);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            Document dom = dbf.newDocumentBuilder().parse(featuresDir.resolve("features.xml").toFile());

            NodeList children = dom.getElementsByTagName("features");
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == ELEMENT_NODE) {
                    NodeList children2 = child.getChildNodes();
                    for (int j = 0; j < children2.getLength(); j++) {
                        Node child2 = children2.item(j);
                        if ("feature".equals(child2.getNodeName())) {
                            String artifactId = child2.getAttributes().getNamedItem("name").getTextContent();
                            if (artifactId != null && artifactId.startsWith("camel-")) {
                                answer.add(artifactId);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading features.xml file", e);
        }

        return answer;
    }

    private boolean isLocalClass(AnnotationInstance ai) {
        String clazz = ai.target().asClass().name().toString();
        return isLocalClass(clazz);
    }

    private boolean isLocalClass(String clazz) {
        Path output = Paths.get(getOutputDirectory());
        Path file = output.resolve(clazz.replace('.', '/') + ".class");
        return Files.isRegularFile(file);
    }

    private String createProperties(String key, String val) {
        String data;
        StringBuilder properties = new StringBuilder();
        properties.append("# " + GENERATED_MSG + NL);
        properties.append(key).append("=").append(val).append(NL);
        properties.append("groupId=").append(getGroupId()).append(NL);
        properties.append("artifactId=").append(getArtifactId()).append(NL);
        properties.append("version=").append(getVersion()).append(NL);
        properties.append("projectName=").append(getName()).append(NL);
        if (getDescription() != null) {
            properties.append("projectDescription=").append(getDescription()).append(NL);
        }
        data = properties.toString();
        return data;
    }

    protected void updateResource(Path out, String data) {
        try {
            if (data == null) {
                if (Files.isRegularFile(out)) {
                    Files.delete(out);
                    refresh(out);
                }
            } else {
                if (Files.isRegularFile(out) && Files.isReadable(out)) {
                    String content = new String(Files.readAllBytes(out), StandardCharsets.UTF_8);
                    if (Objects.equals(content, data)) {
                        return;
                    }
                }
                Files.createDirectories(out.getParent());
                try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
                    w.append(data);
                }
                refresh(out);
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private static String asLanguageModelName(String name) {
        // special for some languages
        if ("bean".equals(name)) {
            return "method";
        } else if ("file".equals(name)) {
            return "simple";
        }
        return name;
    }

    private static String asDataformatModelName(String name) {
        // special for some data formats
        if ("json-gson".equals(name) || "json-jackson".equals(name)
                || "json-johnzon".equals(name) || "json-xstream".equals(name)
                || "json-fastjson".equals(name)) {
            return "json";
        } else if ("bindy-csv".equals(name) || "bindy-fixed".equals(name) || "bindy-kvp".equals(name)) {
            return "bindy";
        } else if ("zipfile".equals(name)) {
            // darn should have been lower case
            return "zipFile";
        } else if ("yaml-snakeyaml".equals(name)) {
            return "yaml";
        }
        return name;
    }

    private static String asTitle(String name, String title) {
        // special for some languages
        if ("file".equals(name)) {
            return "File";
        }
        return title;
    }

    private static String asDescription(String name, String description) {
        // special for some languages
        if ("file".equals(name)) {
            return "For expressions and predicates using the file/simple language";
        }
        return description;
    }

    private static String schemaSubDirectory(String javaType) {
        int idx = javaType.lastIndexOf('.');
        String pckName = javaType.substring(0, idx);
        return pckName.replace('.', '/');
    }

    public void prepareComponent(Path componentOutDir) {

        Set<String> components = getResources().stream()
                .map(Paths::get)
                .map(p -> p.resolve(META_INF_SERVICES_ORG_APACHE_CAMEL + "component"))
                .filter(Files::isDirectory)
                .flatMap(IOHelper::list)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(s -> s.charAt(0) != '.')
                .collect(Collectors.toSet());

        Path camelMetaDir = componentOutDir.resolve(META_INF_SERVICES_ORG_APACHE_CAMEL);
        Path outFile = camelMetaDir.resolve("component.properties");
        String data = null;
        if (!components.isEmpty()) {
            String names = String.join(" ", components);
            data = createProperties("components", names);
            info("Generating " + outFile + " containing " + components.size() + " Camel " + (components.size() > 1 ? "components: " : "component: ") + names);
        }
        updateResource(outFile, data);

        // first we need to setup the output directory because the next check
        // can stop the build before the end and eclipse always needs to know about that directory
        addResourceDirectory(componentOutDir);
    }

    public void processEndpoints(Path endpointsOutDir) {
        getIndex().getAnnotations(URI_ENDPOINT)
                .stream()
                .filter(this::isLocalClass)
                .forEach(ai -> processEndpointClass(endpointsOutDir, ai));

        addResourceDirectory(endpointsOutDir);
    }

    private void processEndpointClass(Path endpointsOutDir, AnnotationInstance uriEndpoint) {
        String scheme = string(uriEndpoint, AP_SCHEME, "");
        String extendsScheme = string(uriEndpoint, AP_EXTENDS_SCHEME).orElse("");
        String title = string(uriEndpoint, AP_TITLE, "");
        String label = string(uriEndpoint, AP_LABEL, "");
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
                Path dir = endpointsOutDir.resolve(packageName.replace('.', '/'));
                Path out = dir.resolve(alias + ".json");
                String json = writeJSonSchemeDocumentation(uriEndpoint, uriEndpoint.target().asClass(), aliasTitle, alias, extendsAlias, label, schemes);
                String data = "// " + GENERATED_MSG + NL + json + NL;
                updateResource(out, data);
            }
        }
    }

    protected String writeJSonSchemeDocumentation(AnnotationInstance uriEndpoint, ClassInfo classElement,
                                                  String title, String scheme, String extendsScheme, String label,
                                                  String[] schemes) {
        // gather component information
        ComponentModel componentModel = findComponentProperties(uriEndpoint, classElement, title, scheme, extendsScheme, label, schemes);

        Optional.ofNullable(componentModel.getJavaType())
                .map(this::findTypeElement)
                .ifPresent(ci -> findComponentClassProperties(componentModel, ci, ""));

        findClassProperties(componentModel, classElement, "", string(uriEndpoint, AP_EXCLUDE_PROPERTIES, ""));

        return createJsonSchema(componentModel);
    }

    protected ComponentModel findComponentProperties(AnnotationInstance uriEndpoint, ClassInfo endpointClassElement,
                                                     String title, String scheme, String extendsScheme, String label, String[] schemes) {
        boolean coreOnly = getArtifactId().equals("camel-core");
        ComponentModel model = new ComponentModel(coreOnly);
        model.setScheme(scheme);

        // if the scheme is an alias then replace the scheme name from the syntax with the alias
        String syntax = scheme + ":" + after(string(uriEndpoint, AP_SYNTAX).get(), ":");
        // alternative syntax is optional
        if (!isNullOrEmpty(string(uriEndpoint, AP_ALTERNATIVE_SYNTAX, ""))) {
            String alternativeSyntax = scheme + ":" + after(string(uriEndpoint, AP_ALTERNATIVE_SYNTAX, ""), ":");
            model.setAlternativeSyntax(alternativeSyntax);
        }

        if (schemes != null && schemes.length > 1) {
            model.setAlternativeSchemes(String.join(",", schemes));
        }

        model.setExtendsScheme(extendsScheme);
        model.setSyntax(syntax);
        model.setTitle(title);
        model.setLabel(label);
        model.setConsumerOnly(bool(uriEndpoint, AP_CONSUMER_ONLY, false));
        model.setProducerOnly(bool(uriEndpoint, AP_PRODUCER_ONLY, false));
        model.setLenientProperties(bool(uriEndpoint, AP_LENIENT_PROPERTIES, false));
        model.setAsync(implementsInterface(endpointClassElement, ASYNC_ENDPOINT));

        // what is the first version this component was added to Apache Camel
        String firstVersion = string(uriEndpoint, AP_FIRST_VERSION, "");
        if (isNullOrEmpty(firstVersion) && metadata(endpointClassElement) != null) {
            // fallback to @Metadata if not from @UriEndpoint
            firstVersion = string(metadata(endpointClassElement), AP_FIRST_VERSION, null);
        }
        if (!isNullOrEmpty(firstVersion)) {
            model.setFirstVersion(firstVersion);
        }

        String data = loadResource(META_INF_SERVICES_ORG_APACHE_CAMEL + "component", scheme);
        if (data != null) {
            Map<String, String> map = parseAsMap(data);
            model.setJavaType(map.get("class"));
        }

        data = loadResource(META_INF_SERVICES_ORG_APACHE_CAMEL, "component.properties");
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
            model.setDeprecated(deprecated(endpointClassElement)
                    || map.getOrDefault("projectName", "").contains("(deprecated)"));

            model.setDeprecationNote(annotation(endpointClassElement, METADATA)
                    .map(ai -> string(ai, AP_DEPRECATION_NOTE, ""))
                    .orElse(null));

            model.setGroupId(map.getOrDefault("groupId", ""));
            model.setArtifactId(map.getOrDefault("artifactId", ""));
            model.setVersion(map.getOrDefault("version", ""));
        }

        String className = endpointClassElement.name().toString();
        JavaClass javaClass = getJavaClass(className);
        if (javaClass != null) {
            String doc = javaClass.getComment();
            if (doc != null) {
                // need to sanitize the description first (we only want a summary)
                doc = sanitizeDescription(doc, true);
                // the javadoc may actually be empty, so only change the doc if we got something
                if (!isNullOrEmpty(doc)) {
                    model.setDescription(doc);
                }
            }
        }

        return model;
    }

    private boolean implementsInterface(ClassInfo endpointClassElement, DotName name) {
        return allImplementedInterfaces(endpointClassElement)
                .map(ClassInfo::name)
                .anyMatch(name::equals);
    }

    private Stream<ClassInfo> allImplementedInterfaces(ClassInfo clazz) {
        return allClasses(clazz)
                .map(ClassInfo::interfaceNames)
                .flatMap(List::stream)
                .map(this::findTypeElement)
                .filter(Objects::nonNull);
    }

    private Stream<ClassInfo> allClasses(ClassInfo clazz) {
        List<ClassInfo> classes = new ArrayList<>();
        while (true) {
            classes.add(clazz);
            DotName superName = clazz.superName();
            if (superName != null) {
                clazz = findTypeElement(superName);
                if (clazz == null) {
                    break;
                }
            } else {
                break;
            }
        }
        return classes.stream();
    }

    private String loadResource(String path, String name) {
        return loadResource(path, name, s -> !s.startsWith("#"));
    }

    private String loadResource(String path, String name, Predicate<String> filter) {
        return getResources().stream()
                .map(Paths::get)
                .map(p -> p.resolve(path).resolve(name))
                .filter(Files::isRegularFile)
                .findFirst()
                .map(p -> IOHelper.lines(p)
                        .filter(filter)
                        .collect(Collectors.joining(NL)))
                .orElse(null);
    }

    protected void findComponentClassProperties(ComponentModel componentModel, ClassInfo classElement, String prefix) {
        while (true) {
            AnnotationInstance componentAnnotation = metadata(classElement);
            if (componentAnnotation != null && Objects.equals("verifiers", string(componentAnnotation, AP_LABEL, ""))) {
                componentModel.setVerifiers(string(componentAnnotation, AP_ENUMS, ""));
            }

            for (MethodInfo method : getOrderedSetters(classElement)) {
                String methodName = method.name();
                boolean deprecated = deprecated(method);
                AnnotationInstance metadata = metadata(method);
                String deprecationNote = string(metadata, AP_DEPRECATION_NOTE, null);

                // must be the setter
                boolean isSetter = methodName.startsWith("set") && method.parameters().size() == 1 & method.returnType().kind() == Type.Kind.VOID;
                if (!isSetter) {
                    continue;
                }

                // skip unwanted methods as they are inherited from default component and are not intended for end users to configure
                if ("setEndpointClass".equals(methodName)
                        || "setCamelContext".equals(methodName)
                        || "setEndpointHeaderFilterStrategy".equals(methodName)
                        || "setApplicationContext".equals(methodName)) {
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
                    metadata = metadata(field);
                }

                boolean required = required(metadata, false);
                String label = string(metadata, AP_LABEL, null);
                boolean secret = bool(metadata, AP_SECRET, false);
                String displayName = string(metadata, AP_DISPLAY_NAME, null);

                // we do not yet have default values / notes / as no annotation support yet
                // String defaultValueNote = param.defaultValueNote();
                String defaultValue = string(metadata, AP_DEFAULT_VALUE, null);
                String defaultValueNote = null;

                String name = fieldName;
                name = prefix + name;
                Type fieldType = field != null ? field.type() : method.parameters().get(0);
                String fieldTypeName = fieldType.toString();
                ClassInfo fieldTypeElement = findTypeElement(fieldTypeName);

                String docComment = findJavaDoc(fieldName, name, classElement, false);
                if (isNullOrEmpty(docComment)) {
                    docComment = string(metadata, AP_DESCRIPTION, "");
                }
                if (isNullOrEmpty(docComment)) {
                    docComment = DocumentationHelper.findComponentPropertyJavaDoc(this, componentModel.getExtendsScheme(), name);
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
                Set<String> enums = getEnums(metadata, fieldTypeElement);
                boolean isEnum = !enums.isEmpty();

                // the field type may be overloaded by another type
                fieldTypeName = string(metadata, AP_JAVA_TYPE, fieldTypeName);
                fieldTypeName = fieldTypeName.replace('$', '.');

                String group = EndpointHelper.labelAsGroupName(label, componentModel.isConsumerOnly(), componentModel.isProducerOnly());
                ComponentOptionModel option = new ComponentOptionModel();
                option.setName(name);
                option.setDisplayName(displayName);
                option.setType(JSonSchemaHelper.getType(fieldTypeName, isEnum));
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

    protected void findClassProperties(ComponentModel componentModel,
                                       ClassInfo classElement, String prefix, String excludeProperties) {
        while (true) {
            for (FieldInfo fieldElement : getOrderedFields(classElement)) {

                String fieldName = fieldElement.name();

                AnnotationInstance metadata = metadata(fieldElement);
                boolean deprecated = deprecated(fieldElement);
                String deprecationNote = string(metadata, AP_DEPRECATION_NOTE, null);
                boolean secret = bool(metadata, AP_SECRET, false);

                AnnotationInstance path = annotation(fieldElement, URI_PATH).orElse(null);
                if (path != null) {
                    String name = prefix + string(path, AP_NAME).orElse(fieldName);

                    // should we exclude the name?
                    if (excludeProperty(excludeProperties, name)) {
                        continue;
                    }

                    String defaultValue = string(path, AP_DEFAULT_VALUE, string(metadata, AP_DEFAULT_VALUE, ""));
                    String defaultValueNote = string(path, AP_DEFAULT_VALUE_NOTE, "");
                    boolean required = required(metadata, false);
                    String label = string(path, AP_LABEL, string(metadata, AP_LABEL, ""));
                    String displayName = string(path, AP_DISPLAY_NAME, string(metadata, AP_DISPLAY_NAME, ""));

                    Type fieldType = fieldElement.type();
                    String fieldTypeName = fieldType.toString();
                    ClassInfo fieldTypeElement = findTypeElement(fieldTypeName);

                    String docComment = findJavaDoc(fieldName, name, classElement, false);
                    if (isNullOrEmpty(docComment)) {
                        docComment = string(path, AP_DESCRIPTION, "");
                    }
                    if (isNullOrEmpty(docComment)) {
                        docComment = DocumentationHelper.findEndpointPropertyJavaDoc(this, componentModel.getExtendsScheme(), name);
                    }
                    if (docComment != null) {
                        docComment = docComment.trim();
                    }

                    // gather enums
                    Set<String> enums = getEnums(path, fieldTypeElement);
                    boolean isEnum = !enums.isEmpty();

                    // the field type may be overloaded by another type
                    fieldTypeName = string(path, AP_JAVA_TYPE, fieldTypeName);
                    fieldTypeName = fieldTypeName.replace('$', '.');

                    String group = EndpointHelper.labelAsGroupName(label, componentModel.isConsumerOnly(), componentModel.isProducerOnly());
                    EndpointOptionModel option = new EndpointOptionModel();
                    option.setName(name);
                    option.setDisplayName(displayName);
                    option.setType(JSonSchemaHelper.getType(fieldTypeName, isEnum));
                    option.setJavaType(fieldTypeName);
                    option.setRequired(required);
                    option.setDefaultValue(defaultValue);
                    option.setDefaultValueNote(defaultValueNote);
                    option.setDescription(docComment);
                    option.setDeprecated(deprecated);
                    option.setDeprecationNote(deprecationNote);
                    option.setSecret(secret);
                    option.setGroup(group);
                    option.setLabel(label);
                    option.setEnumType(isEnum);
                    option.setEnums(enums);

                    componentModel.addEndpointPathOption(option);
                }

                AnnotationInstance param = annotation(fieldElement, URI_PARAM).orElse(null);
                fieldName = fieldElement.name() ;
                if (param != null) {
                    String name = string(param, AP_NAME).orElse(fieldName);
                    name = prefix + name;

                    // should we exclude the name?
                    if (excludeProperty(excludeProperties, name)) {
                        continue;
                    }

                    String paramOptionalPrefix = string(param, AP_OPTIONAL_PREFIX, "");
                    String paramPrefix = string(param, AP_PREFIX, "");
                    boolean multiValue = bool(param, AP_MULTI_VALUE, false);
                    String defaultValue = string(param, AP_DEFAULT_VALUE, string(metadata, AP_DEFAULT_VALUE, ""));
                    String defaultValueNote = string(param, AP_DEFAULT_VALUE_NOTE, "");
                    boolean required = required(metadata, false);
                    String label = string(param, AP_LABEL, string(metadata, AP_LABEL, ""));
                    String displayName = string(param, AP_DISPLAY_NAME, string(metadata, AP_DISPLAY_NAME, ""));

                    // if the field type is a nested parameter then iterate through its fields
                    Type fieldType = fieldElement.type();
                    String fieldTypeName = fieldType.toString();
                    ClassInfo fieldTypeElement = findTypeElement(fieldTypeName);
                    AnnotationInstance fieldParams = annotation(fieldTypeElement, URI_PARAMS).orElse(null);
                    if (fieldParams != null) {
                        String nestedPrefix = prefix;
                        String extraPrefix = string(fieldParams, AP_PREFIX, "");
                        if (!isNullOrEmpty(extraPrefix)) {
                            nestedPrefix += extraPrefix;
                        }
                        findClassProperties(componentModel, fieldTypeElement, nestedPrefix, excludeProperties);
                    } else {
                        String docComment = findJavaDoc(fieldName, name, classElement, false);
                        if (isNullOrEmpty(docComment)) {
                            docComment = string(param, AP_DESCRIPTION, "");
                        }
                        if (isNullOrEmpty(docComment)) {
                            docComment = DocumentationHelper.findEndpointPropertyJavaDoc(this, componentModel.getExtendsScheme(), name);
                        }
                        if (isNullOrEmpty(docComment)) {
                            docComment = "";
                        }

                        // gather enums
                        Set<String> enums = getEnums(param, fieldTypeElement);
                        boolean isEnum = !enums.isEmpty();

                        // the field type may be overloaded by another type
                        fieldTypeName = string(param, AP_JAVA_TYPE, fieldTypeName);
                        fieldTypeName = fieldTypeName.replace('$', '.');

                        String group = EndpointHelper.labelAsGroupName(label, componentModel.isConsumerOnly(), componentModel.isProducerOnly());
                        EndpointOptionModel option = new EndpointOptionModel();
                        option.setName(name);
                        option.setDisplayName(displayName);
                        option.setType(JSonSchemaHelper.getType(fieldTypeName, isEnum));
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

                        componentModel.addEndpointOption(option);
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

    private Set<String> getEnums(AnnotationInstance path, ClassInfo fieldTypeElement) {
        Set<String> enums = new LinkedHashSet<>();
        String enumsStr = string(path, AP_ENUMS, "");
        if (!isNullOrEmpty(enumsStr)) {
            Collections.addAll(enums, enumsStr.split(","));
        } else if (isEnumClass(fieldTypeElement)) {
            // find all the enum constants which has the possible enum value that can be used
            fieldTypeElement.fields().stream()
                    .filter(JandexHelper::isEnumConstant)
                    .map(FieldInfo::name)
                    .forEach(enums::add);
        }
        return enums;
    }

    private ClassInfo findTypeElement(String javaType) {
        return findTypeElement(DotName.createSimple(javaType));
    }

    private ClassInfo findTypeElement(DotName javaType) {
        ClassInfo info = getIndex().getClassByName(javaType);
        return info;
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

    private static Map<String, String> parseAsMap(String data) {
        Map<String, String> answer = new HashMap<>();
        String[] lines = data.split(NL);
        for (String line : lines) {
            if (!line.isEmpty() && !line.startsWith("#")) {
                int idx = line.indexOf('=');
                String key = line.substring(0, idx);
                String value = line.substring(idx + 1);
                // remove ending line break for the values
                value = value.trim().replaceAll(NL, "");
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

            return "groovy.lang.MetaClass".equals(returnType.asElement().getSimpleName().toString());
        } else {
            // Eclipse (Groovy?) compiler returns javax.lang.model.type.NoType, no other way to check but to look at toString output
            return method.toString().contains("(groovy.lang.MetaClass)");
        }
    }

    private JavaClass getJavaClass(String className) {
        JavaClass javaClass = sourceLibrary.getJavaClass(className, false);
        if (javaClass == null) {
            Path source = getCompileSourceRoots().stream()
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

//    private JavaClassSource getJavaClassSource(String className) {
//        Path source = getCompileSourceRoots()
//                .stream()
//                .map(Paths::get)
//                .map(p -> p.resolve(className.replace('.', '/') + ".java"))
//                .filter(Files::isRegularFile)
//                .findAny()
//                .orElse(null);
//        if (source != null) {
//            try {
//                JavaClassSource clazz = Roaster.parse(JavaClassSource.class, source.toFile());
//                return clazz;
//            } catch (IOException e) {
//                throw new IOError(e);
//            }
//        }
//        return null;
//    }

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
                        .filter(jm -> ("set" + fieldName).equalsIgnoreCase(jm.getName()))
                        .filter(jm -> jm.getParameters().size() == 1)
                        .filter(jm -> !isNullOrEmpty(jm.getComment()))
                        .findFirst()
                        .map(JavaMethod::getComment)
                        .orElse(null);
            }
            if (javadoc == null) {
                javadoc = javaClass.getMethods().stream()
                        .filter(jm -> ("get" + fieldName).equalsIgnoreCase(jm.getName()))
                        .filter(jm -> jm.getParameters().size() == 0)
                        .filter(jm -> !isNullOrEmpty(jm.getComment()))
                        .findFirst()
                        .map(JavaMethod::getComment)
                        .orElse(null);
            }
            if (javadoc == null) {
                javadoc = javaClass.getMethods().stream()
                        .filter(jm -> ("is" + fieldName).equalsIgnoreCase(jm.getName()))
                        .filter(jm -> jm.getParameters().size() == 0)
                        .filter(jm -> !isNullOrEmpty(jm.getComment()))
                        .findFirst()
                        .map(JavaMethod::getComment)
                        .orElse(null);
            }
            if (javadoc == null && builderPattern) {
                javadoc = javaClass.getMethods().stream()
                        .filter(jm -> fieldName.equalsIgnoreCase(jm.getName()))
                        .filter(jm -> jm.getParameters().size() == 1)
                        .filter(jm -> !isNullOrEmpty(jm.getComment()))
                        .findFirst()
                        .map(JavaMethod::getComment)
                        .orElse(null);
            }
            if (javadoc == null && builderPattern) {
                javadoc = javaClass.getMethods().stream()
                        .filter(jm -> fieldName.equalsIgnoreCase(jm.getName()))
                        .filter(jm -> jm.getParameters().size() == 0)
                        .filter(jm -> !isNullOrEmpty(jm.getComment()))
                        .findFirst()
                        .map(JavaMethod::getComment)
                        .orElse(null);
            }
            if (javadoc == null) {
                javadoc = javaClass.getMethods().stream()
                        .filter(jm -> fieldName.equalsIgnoreCase(jm.getName()))
                        .filter(jm -> jm.getParameters().size() == 1)
                        .filter(jm -> !isNullOrEmpty(jm.getComment()))
                        .findFirst()
                        .map(JavaMethod::getComment)
                        .orElse(null);
            }
            if (javadoc == null) {
                javadoc = javaClass.getMethods().stream()
                        .filter(jm -> fieldName.equalsIgnoreCase(jm.getName()))
                        .filter(jm -> jm.getParameters().size() == 0)
                        .filter(jm -> !isNullOrEmpty(jm.getComment()))
                        .findFirst()
                        .map(JavaMethod::getComment)
                        .orElse(null);
            }
        }
        return javadoc;
    }

    public void createConverter(Path converterOutDir) {
        Map<String, Map<Type, AnnotationInstance>> converters = new TreeMap<>();
        getIndex().getAnnotations(CONVERTER).stream()
                .filter(this::isMethodTarget)
                .forEach(ai -> {
                    Type to = ai.target().asMethod().returnType();
                    Type from = ai.target().asMethod().parameters().get(0);
                    converters.computeIfAbsent(toClassString(to), c -> new TreeMap<>(this::compare)).put(from, ai);
                });

        List<AnnotationInstance> fallbackConverters = getIndex().getAnnotations(FALLBACK_CONVERTER).stream()
                .filter(this::isMethodTarget)
                .sorted(Comparator.comparing(ai -> ai.target().asMethod().declaringClass().name()))
                .collect(Collectors.toList());

        StringBuilder source = new StringBuilder();
        try {
            doWriteConverter(source, converters, fallbackConverters);
        } catch (IOException e) {
            throw new IOError(e);
        }

        Path output = converterOutDir.resolve("org/apache/camel/impl/converter/CoreStaticTypeConverterLoader.java");
        updateResource(output, source.toString());

        addCompileSourceRoot(converterOutDir.toString());
    }

    private boolean isMethodTarget(AnnotationInstance ai) {
        return ai.target().kind() == Kind.METHOD;
    }

    private void doWriteConverter(Appendable writer, Map<String, Map<Type, AnnotationInstance>> converters, List<AnnotationInstance> fallbackConverters) throws IOException {

        Set<String> converterClasses = new LinkedHashSet<>();

        writer.append("//\n");
        writer.append("// " + GENERATED_MSG + NL);
        writer.append("//\n");
        writer.append("package org.apache.camel.impl.converter;\n");
        writer.append(NL);
        writer.append("import org.apache.camel.Exchange;\n");
        writer.append("import org.apache.camel.TypeConversionException;\n");
        writer.append("import org.apache.camel.TypeConverterLoaderException;\n");
        writer.append("import org.apache.camel.spi.TypeConverterLoader;\n");
        writer.append("import org.apache.camel.spi.TypeConverterRegistry;\n");
        writer.append("import org.apache.camel.support.TypeConverterSupport;\n");
        writer.append(NL);
        writer.append("@SuppressWarnings(\"unchecked\")\n");
        writer.append("public class CoreStaticTypeConverterLoader implements TypeConverterLoader {\n");
        writer.append(NL);
        writer.append("    public static final CoreStaticTypeConverterLoader INSTANCE = new CoreStaticTypeConverterLoader();\n");
        writer.append(NL);
        writer.append("    static abstract class SimpleTypeConverter extends TypeConverterSupport {\n");
        writer.append("        private final boolean allowNull;\n");
        writer.append(NL);
        writer.append("        public SimpleTypeConverter(boolean allowNull) {\n");
        writer.append("            this.allowNull = allowNull;\n");
        writer.append("        }\n");
        writer.append(NL);
        writer.append("        @Override\n");
        writer.append("        public boolean allowNull() {\n");
        writer.append("            return allowNull;\n");
        writer.append("        }\n");
        writer.append(NL);
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
        writer.append(NL);
        writer.append("    private DoubleMap<Class<?>, Class<?>, SimpleTypeConverter> converters = new DoubleMap<>(256);\n");
        writer.append(NL);
        writer.append("    private CoreStaticTypeConverterLoader() {\n");

        for (Map.Entry<String, Map<Type, AnnotationInstance>> toE : converters.entrySet()) {
            for (Map.Entry<Type, AnnotationInstance> fromE : toE.getValue().entrySet()) {
                String to = toE.getKey();
                Type from = fromE.getKey();
                AnnotationInstance ai = fromE.getValue();
                boolean allowNull = ai.value("allowNull") != null && ai.value("allowNull").asBoolean();
                writer.append("        converters.put(").append(to).append(".class").append(", ")
                        .append(toClassString(from)).append(".class, new SimpleTypeConverter(")
                        .append(Boolean.toString(allowNull)).append(") {\n");
                writer.append("            @Override\n");
                writer.append("            public Object doConvert(Exchange exchange, Object value) throws Exception {\n");
                writer.append("                return ").append(toJava(ai, converterClasses)).append(";\n");
                writer.append("            }\n");
                writer.append("        });\n");
            }
        }
        writer.append("    }\n");
        writer.append(NL);
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
        writer.append(NL);

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
        String type = toClassString(converter.parameters().get(0));
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
        return type.toString();
    }

    private String toClassString(Type type) {
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

    private DataFormatModel extractDataFormatModel(String json, String modelName, String name, String javaType) {
        DataFormatModel dataFormatModel = new DataFormatModel();
        dataFormatModel.setName(name);
        dataFormatModel.setTitle("");
        dataFormatModel.setModelName(modelName);
        dataFormatModel.setLabel("");
        dataFormatModel.setDescription(getDescription());
        dataFormatModel.setJavaType(javaType);
        dataFormatModel.setGroupId(getGroupId());
        dataFormatModel.setArtifactId(getArtifactId());
        dataFormatModel.setVersion(getVersion());

        List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("model", json, false);
        for (Map<String, String> row : rows) {
            if (row.containsKey(AP_TITLE)) {
                String title = row.get(AP_TITLE);
                dataFormatModel.setTitle(asModelTitle(name, title));
            }
            if (row.containsKey(AP_LABEL)) {
                dataFormatModel.setLabel(row.get(AP_LABEL));
            }
            if (row.containsKey(AP_DEPRECATED)) {
                dataFormatModel.setDeprecated(Boolean.parseBoolean(row.get(AP_DEPRECATED)));
            }
            if (row.containsKey(AP_DEPRECATION_NOTE)) {
                dataFormatModel.setDeprecationNote(row.get(AP_DEPRECATION_NOTE));
            }
            if (row.containsKey(AP_JAVA_TYPE)) {
                dataFormatModel.setModelJavaType(row.get(AP_JAVA_TYPE));
            }
            if (row.containsKey(AP_FIRST_VERSION)) {
                dataFormatModel.setFirstVersion(row.get(AP_FIRST_VERSION));
            }
            // favor description from the model schema
            if (row.containsKey(AP_DESCRIPTION)) {
                dataFormatModel.setDescription(row.get(AP_DESCRIPTION));
            }
        }

        // first version special for json
        String firstVersion = prepareJsonFirstVersion(name);
        if (firstVersion != null) {
            dataFormatModel.setFirstVersion(firstVersion);
        }

        return dataFormatModel;
    }

    private static String prepareBindyProperties(String name, String properties) {
        String bindy = "\"enum\": [ \"Csv\", \"Fixed\", \"KeyValue\" ], \"deprecated\": \"false\", \"secret\": \"false\"";
        String bindyCsv = "\"enum\": [ \"Csv\", \"Fixed\", \"KeyValue\" ], \"deprecated\": \"false\", \"secret\": \"false\", \"defaultValue\": \"Csv\"";
        String bindyFixed = "\"enum\": [ \"Csv\", \"Fixed\", \"KeyValue\" ], \"deprecated\": \"false\", \"secret\": \"false\", \"defaultValue\": \"Fixed\"";
        String bindyKvp = "\"enum\": [ \"Csv\", \"Fixed\", \"KeyValue\" ], \"deprecated\": \"false\", \"secret\": \"false\", \"defaultValue\": \"KeyValue\"";

        if ("bindy-csv".equals(name)) {
            properties = properties.replace(bindy, bindyCsv);
        } else if ("bindy-fixed".equals(name)) {
            properties = properties.replace(bindy, bindyFixed);
        } else if ("bindy-kvp".equals(name)) {
            properties = properties.replace(bindy, bindyKvp);
        }

        return properties;
    }

    private static String prepareJsonProperties(String name, String properties) {
        String json = "\"enum\": [ \"Gson\", \"Jackson\", \"Johnzon\", \"XStream\", \"Fastjson\" ], \"deprecated\": \"false\", \"secret\": \"false\", \"defaultValue\": \"XStream\"";
        String jsonGson = "\"enum\": [ \"Gson\", \"Jackson\", \"Johnzon\", \"XStream\", \"Fastjson\" ], \"deprecated\": \"false\", \"secret\": \"false\", \"defaultValue\": \"Gson\"";
        String jsonJackson = "\"enum\": [ \"Gson\", \"Jackson\", \"Johnzon\", \"XStream\", \"Fastjson\" ], \"deprecated\": \"false\", \"secret\": \"false\", \"defaultValue\": \"Jackson\"";
        String jsonJohnzon = "\"enum\": [ \"Gson\", \"Jackson\", \"Johnzon\", \"XStream\", \"Fastjson\" ], \"deprecated\": \"false\", \"secret\": \"false\", \"defaultValue\": \"Johnzon\"";
        String jsonXStream = "\"enum\": [ \"Gson\", \"Jackson\", \"Johnzon\", \"XStream\", \"Fastjson\" ], \"deprecated\": \"false\", \"secret\": \"false\", \"defaultValue\": \"XStream\"";
        String jsonFastjson = "\"enum\": [ \"Gson\", \"Jackson\", \"Johnzon\", \"XStream\", \"Fastjson\" ], \"deprecated\": \"false\", \"secret\": \"false\", \"defaultValue\": \"Fastjson\"";

        if ("json-gson".equals(name)) {
            properties = properties.replace(json, jsonGson);
        } else if ("json-jackson".equals(name)) {
            properties = properties.replace(json, jsonJackson);
        } else if ("json-johnzon".equals(name)) {
            properties = properties.replace(json, jsonJohnzon);
        } else if ("json-xstream".equals(name)) {
            properties = properties.replace(json, jsonXStream);
        } else if ("json-fastjson".equals(name)) {
            properties = properties.replace(json, jsonFastjson);
        }

        return properties;
    }

    private static String prepareJsonFirstVersion(String name) {
        if ("json-gson".equals(name)) {
            return "2.10.0";
        } else if ("json-jackson".equals(name)) {
            return "2.0.0";
        } else if ("json-johnzon".equals(name)) {
            return "2.18.0";
        } else if ("json-xstream".equals(name)) {
            return "2.0.0";
        } else if ("json-fastjson".equals(name)) {
            return "2.20.0";
        }

        return null;
    }

    private static String asModelTitle(String name, String title) {
        // special for some data formats
        if ("json-gson".equals(name)) {
            return "JSon GSon";
        } else if ("json-jackson".equals(name)) {
            return "JSon Jackson";
        } else if ("json-johnzon".equals(name)) {
            return "JSon Johnzon";
        } else if ("json-xstream".equals(name)) {
            return "JSon XStream";
        } else if ("json-fastjson".equals(name)) {
            return "JSon Fastjson";
        } else if ("bindy-csv".equals(name)) {
            return "Bindy CSV";
        } else if ("bindy-fixed".equals(name)) {
            return "Bindy Fixed Length";
        } else if ("bindy-kvp".equals(name)) {
            return "Bindy Key Value Pair";
        } else if ("yaml-snakeyaml".equals(name)) {
            return "YAML SnakeYAML";
        }
        return title;
    }

    public void prepareOthers(Path otherOutDir, Path schemaOutDir) {

        // are there any components, data formats or languages?
        if (getResources().stream()
                .map(Paths::get)
                .flatMap(d -> Stream.of(
                        d.resolve(META_INF_SERVICES_ORG_APACHE_CAMEL).resolve("component"),
                        d.resolve(META_INF_SERVICES_ORG_APACHE_CAMEL).resolve("dataformat"),
                        d.resolve(META_INF_SERVICES_ORG_APACHE_CAMEL).resolve("language")))
                .anyMatch(Files::isDirectory)) {
            return;
        }

        // okay none of those then this is a other kind of artifact

        String name = getArtifactId();
        // strip leading camel-
        if (name.startsWith("camel-")) {
            name = name.substring(6);
        }

        // create json model
        OtherModel otherModel = new OtherModel();
        otherModel.setName(name);
        otherModel.setGroupId(getGroupId());
        otherModel.setArtifactId(getArtifactId());
        otherModel.setVersion(getVersion());
        otherModel.setDescription(getDescription());
        otherModel.setDeprecated(getName() != null && getName().contains("(deprecated)"));
        otherModel.setFirstVersion(getProperty(AP_FIRST_VERSION));
        otherModel.setLabel(getProperty(AP_LABEL));
        String title = getProperty(AP_TITLE);
        if (title == null) {
            title = StringHelper.camelDashToTitle(name);
        }
        otherModel.setTitle(title);

        debug("Model " + otherModel);

        Path out = schemaOutDir.resolve(name + ".json");
        String json = createJsonSchema(otherModel);
        String data = "// " + GENERATED_MSG + NL + json + NL;
        updateResource(out, data);

        debug("Generated " + out + " containing JSon schema for " + name + " other");

        // now create properties file
        out = otherOutDir.resolve(META_INF_SERVICES_ORG_APACHE_CAMEL).resolve("other.properties");

        data = createProperties(AP_NAME, name);
        info("Generating " + out);
        updateResource(out, data);

        // first we need to setup the output directory because the next check
        // can stop the build before the end and eclipse always needs to know about that directory
        addResourceDirectory(otherOutDir);
        addResourceDirectory(schemaOutDir);
    }

    public void processModelDoc(Path modeldocOutDir) {
        getIndex().getAnnotations(XML_ROOT_ELEMENT)
                .stream()
                .filter(this::isConcrete)
                .filter(ai -> isCoreClass(ai) || isXmlClass(ai))
                .forEach(ai -> processClass(modeldocOutDir, ai));

        addResourceDirectory(modeldocOutDir);
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

    private void processClass(Path modeldocOutDir, AnnotationInstance xmlRootElement) {
        String javaTypeName = xmlRootElement.target().asClass().name().toString();
        String packageName = javaTypeName.substring(0, javaTypeName.lastIndexOf("."));

        if (ONE_OF_TYPE_NAME.equals(javaTypeName)) {
            return;
        }

        String name = string(xmlRootElement, AP_NAME, "##default");
        if ("##default".equals(name)) {
            name = xmlRootElement.target().asClass().annotations()
                    .get(DotName.createSimple(XmlType.class.getName()))
                    .stream()
                    .filter(ai -> ai.target() == xmlRootElement.target())
                    .findAny()
                    .map(ai -> string(ai, AP_NAME, "##default"))
                    .orElse("##default");
        }
        String fileName = "##default".equals(name) || isNullOrEmpty(name)
                ? xmlRootElement.target().asClass().simpleName() + ".json"
                : name + ".json";

        boolean core = isCoreClass(xmlRootElement);
        String json = writeJSonSchemeDocumentation(
                xmlRootElement.target().asClass(), javaTypeName, name, core);

        Path path = modeldocOutDir.resolve(packageName.replace('.', '/')).resolve(fileName);
        String data = "// " + GENERATED_MSG + NL + json + NL;
        updateResource(path, data);
    }

    protected String writeJSonSchemeDocumentation(ClassInfo classElement,
                                                  String javaTypeName,
                                                  String modelName,
                                                  boolean core) {
        // gather eip information
        EipModel eipModel = findEipModelProperties(classElement, javaTypeName, modelName);

        // get endpoint information which is divided into paths and options (though there should really only be one path)
        findClassProperties(eipModel.getEipOptions(), classElement, classElement, "", modelName, core);

        // after we have found all the options then figure out if the model accepts input/output
        if (core) {
            eipModel.setInput(hasInput(classElement));
            eipModel.setOutput(hasOutput(eipModel));
        }

        return createJsonSchema(eipModel);
    }

    protected EipModel findEipModelProperties(ClassInfo classElement,
                                              String javaTypeName,
                                              String name) {
        EipModel model = new EipModel();
        model.setJavaType(javaTypeName);
        model.setName(name);

        AnnotationInstance metadata = metadata(classElement);

        model.setDeprecated(deprecated(classElement));
        model.setLabel(string(metadata, AP_LABEL, null));
        model.setTitle(string(metadata, AP_TITLE, null));
        model.setFirstVersion(string(metadata, AP_FIRST_VERSION, null));

        // favor to use class javadoc of component as description
        if (model.getJavaType() != null) {
            ClassInfo typeElement = findTypeElement(model.getJavaType());
            if (typeElement != null) {
                String doc = getDocComment(typeElement);
                if (doc != null) {
                    // need to sanitize the description first (we only want a summary)
                    doc = sanitizeDescription(doc, true);
                    // the javadoc may actually be empty, so only change the doc if we got something
                    if (!isNullOrEmpty(doc)) {
                        model.setDescription(doc);
                    }
                }
            }
        }

        return model;
    }

    protected void findClassProperties(List<EipOptionModel> eipOptions,
                                       ClassInfo originalClassType,
                                       ClassInfo classElement,
                                       String prefix,
                                       String modelName,
                                       boolean core) {
        while (true) {
            for (FieldInfo fieldElement : getOrderedFields(classElement)) {

                String fieldName = fieldElement.name();

                AnnotationInstance attribute = annotation(fieldElement, XML_ATTRIBUTE).orElse(null);
                if (attribute != null) {
                    boolean skip = processAttribute(originalClassType, classElement, fieldElement, fieldName, attribute, eipOptions, prefix, modelName);
                    if (skip) {
                        continue;
                    }
                }

                if (core) {
                    AnnotationInstance value = annotation(fieldElement, XML_VALUE).orElse(null);
                    if (value != null) {
                        processValue(originalClassType, classElement, fieldElement, fieldName, value, eipOptions, prefix, modelName);
                    }
                }

                AnnotationInstance elements = annotation(fieldElement, XML_ELEMENTS).orElse(null);
                if (elements != null) {
                    processElements(classElement, elements, fieldElement, eipOptions, prefix);
                }

                AnnotationInstance element = annotation(fieldElement, XML_ELEMENT).orElse(null);
                if (element != null) {
                    processElement(classElement, element, fieldElement, eipOptions, prefix, core);
                }

                // special for eips which has outputs or requires an expressions
                AnnotationInstance elementRef = annotation(fieldElement, XML_ELEMENT_REF).orElse(null);
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

    private Iterable<FieldInfo> getOrderedFields(ClassInfo classElement) {
        JavaClass javaClass = getJavaClass(classElement.name().toString());
        if (javaClass != null) {
            return javaClass.getFields().stream()
                    .map(JavaField::getName)
                    .map(classElement::field)::iterator;
        } else {
            return classElement.fields();
        }
    }

    private Iterable<MethodInfo> getOrderedSetters(ClassInfo classElement) {
        List<MethodInfo> methods = classElement.methods().stream()
            .filter(mi -> mi.name().startsWith("set"))
            .filter(mi -> mi.parameters().size() == 1)
            .filter(mi -> mi.returnType().kind() == Type.Kind.VOID)
            .collect(Collectors.toList());
        JavaClass javaClass = getJavaClass(classElement.name().toString());
        Stream<MethodInfo> stream;
        if (javaClass != null) {
            stream = javaClass.getMethods().stream()
                .filter(ms -> ms.getName().startsWith("set"))
                .filter(ms -> ms.getParameters().size() == 1)
                .filter(ms -> "void".equals(ms.getReturnType().toString()))
                .flatMap(ms -> methods.stream()
                        .filter(mi -> Objects.equals(ms.getName(), mi.name())));
        } else {
            stream = classElement.methods().stream()
                .filter(mi -> mi.parameters().size() == 1)
                .filter(mi -> mi.returnType().kind() == Type.Kind.VOID);
        }
        List<MethodInfo> s = stream
                .sorted(Comparator.comparingInt(mi -> (mi.flags() & 0x1000) == 0 ? -1 : 1))
                .collect(Collectors.toList());
        return s;
    }

    private boolean processAttribute(ClassInfo originalClassType,
                                     ClassInfo classElement,
                                     FieldInfo fieldElement,
                                     String fieldName,
                                     AnnotationInstance attribute,
                                     List<EipOptionModel> eipOptions,
                                     String prefix,
                                     String modelName) {
        String name = string(attribute, AP_NAME, null);
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

        AnnotationInstance metadata = metadata(fieldElement);

        name = prefix + name;
        String fieldTypeName = toString(fieldElement.type());
        ClassInfo fieldTypeElement = findTypeElement(fieldTypeName);

        String defaultValue = findDefaultValue(fieldElement, fieldTypeName);
        String docComment = findJavaDoc(fieldName, name, classElement, true);
        boolean required = required(fieldElement, required(attribute, false));

        // gather enums
        Set<String> enums = getEnums(metadata, fieldTypeElement);
        boolean isEnum = !enums.isEmpty();

        String displayName = string(metadata, AP_DISPLAY_NAME, null);
        boolean deprecated = deprecated(fieldElement);
        String deprecationNote = null;
        if (metadata != null) {
            deprecationNote = string(metadata, AP_DEPRECATION_NOTE, null);
        }

        EipOptionModel ep = new EipOptionModel(name, displayName, "attribute", fieldTypeName, required, defaultValue, docComment, deprecated, deprecationNote, isEnum, enums, false, null, false);
        eipOptions.add(ep);

        return false;
    }

    private void processValue(ClassInfo originalClassType,
                              ClassInfo classElement,
                              FieldInfo fieldElement,
                              String fieldName,
                              AnnotationInstance value,
                              List<EipOptionModel> eipOptions,
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
        required = required(fieldElement, required);

        AnnotationInstance metadata = metadata(fieldElement);
        String displayName = string(metadata, AP_DISPLAY_NAME, null);
        boolean deprecated = deprecated(fieldElement);
        String deprecationNote = string(metadata, AP_DEPRECATION_NOTE, null);

        EipOptionModel ep = new EipOptionModel(name, displayName, "value", fieldTypeName, required, defaultValue, docComment, deprecated, deprecationNote, false, null, false, null, false);
        eipOptions.add(ep);
    }

    private void processElement(ClassInfo classElement,
                                AnnotationInstance element,
                                FieldInfo fieldElement,
                                List<EipOptionModel> eipOptions,
                                String prefix,
                                boolean core) {
        String fieldName = fieldElement.name();
        if (element != null) {

            AnnotationInstance metadata = metadata(fieldElement);

            String kind = "element";
            String name = string(element, AP_NAME, null);
            if (isNullOrEmpty(name) || "##default".equals(name)) {
                name = fieldName;
            }
            name = prefix + name;
            String fieldTypeName = toString(fieldElement.type());
            ClassInfo fieldTypeElement = findTypeElement(fieldTypeName);

            String defaultValue = findDefaultValue(fieldElement, fieldTypeName);
            String docComment = findJavaDoc(fieldName, name, classElement, true);
            boolean required = required(element, false);
            // metadata may overrule element required
            required = required(fieldElement, required);

            // is it used as predicate (check field first and then fallback to its class)
            boolean asPredicate = core
                    && (annotation(fieldElement, AS_PREDICATE).isPresent()
                    || annotation(classElement, AS_PREDICATE).isPresent());

            // gather enums
            Set<String> enums = new LinkedHashSet<>();
            boolean isEnum;
            String enumsStr = string(metadata, AP_ENUMS, "");
            if (!isNullOrEmpty(enumsStr)) {
                isEnum = true;
                Collections.addAll(enums, enumsStr.split(","));
            } else if (isEnumClass(fieldTypeElement)) {
                isEnum = true;
                // find all the enum constants which has the possible enum value that can be used
                fieldTypeElement.fields().stream()
                        .filter(JandexHelper::isEnumConstant)
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
                                AnnotationInstance rootElement = annotation(child, XML_ROOT_ELEMENT).orElse(null);
                                if (rootElement != null) {
                                    String childName = string(rootElement, AP_NAME, "##default");
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
                        AnnotationInstance rootElement = annotation(definitionClass, XML_ROOT_ELEMENT).orElse(null);
                        if (rootElement != null) {
                            String childName = string(rootElement, AP_NAME, null);
                            if (childName != null) {
                                oneOfTypes.add(childName);
                            }
                        }
                    }
                } else if (fieldTypeName.endsWith("Definition>") || fieldTypeName.endsWith("FactoryBean>")) {
                    // its a list so we need to load the generic type
                    String typeName = between(fieldTypeName, "<", ">");
                    ClassInfo definitionClass = findTypeElement(typeName);
                    if (definitionClass != null) {
                        AnnotationInstance rootElement = annotation(definitionClass, XML_ROOT_ELEMENT).orElse(null);
                        if (rootElement != null) {
                            String childName = string(rootElement, AP_NAME, null);
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
                displayName = string(metadata, AP_DISPLAY_NAME, null);
            }
            boolean deprecated = deprecated(fieldElement);
            String deprecationNote = null;
            if (metadata != null) {
                deprecationNote = string(metadata, AP_DEPRECATION_NOTE, null);
            }

            EipOptionModel ep = new EipOptionModel(name, displayName, kind, fieldTypeName, required, defaultValue, docComment, deprecated, deprecationNote, isEnum, enums, oneOf, oneOfTypes, asPredicate);
            eipOptions.add(ep);
        }
    }

    private void processElements(ClassInfo classElement,
                                 AnnotationInstance elements,
                                 FieldInfo fieldElement,
                                 List<EipOptionModel> eipOptions,
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
            required = required(fieldElement, required);

            // gather oneOf of the elements
            Set<String> oneOfTypes = new TreeSet<>();
            for (AnnotationInstance element : elements.value().asNestedArray()) {
                String child = string(element, AP_NAME, "##default");
                oneOfTypes.add(child);
            }

            AnnotationInstance metadata = metadata(fieldElement);
            String displayName = string(metadata, AP_DISPLAY_NAME, null);
            boolean deprecated = deprecated(fieldElement);
            String deprecationNote = string(metadata, AP_DEPRECATION_NOTE, null);

            EipOptionModel ep = new EipOptionModel(name, displayName, kind, fieldTypeName, required, defaultValue, docComment, deprecated, deprecationNote, false, null, true, oneOfTypes, false);
            eipOptions.add(ep);
        }
    }

    private void processRoute(ClassInfo originalClassType,
                              ClassInfo classElement,
                              List<EipOptionModel> eipOptions,
                              String prefix) {



        // group
        String docComment = findJavaDoc("group", null, classElement, true);
        EipOptionModel ep = new EipOptionModel("group", "Group", "attribute", "java.lang.String", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // group
        docComment = findJavaDoc("streamCache", null, classElement, true);
        ep = new EipOptionModel("streamCache", "Stream Cache", "attribute", "java.lang.String", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // trace
        docComment = findJavaDoc("trace", null, classElement, true);
        ep = new EipOptionModel("trace", "Trace", "attribute", "java.lang.String", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // message history
        docComment = findJavaDoc("messageHistory", null, classElement, true);
        ep = new EipOptionModel("messageHistory", "Message History", "attribute", "java.lang.String", false, "true", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // log mask
        docComment = findJavaDoc("logMask", null, classElement, true);
        ep = new EipOptionModel("logMask", "Log Mask", "attribute", "java.lang.String", false, "false", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // trace
        docComment = findJavaDoc("handleFault", null, classElement, true);
        ep = new EipOptionModel("handleFault", "Handle Fault", "attribute", "java.lang.String", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // delayer
        docComment = findJavaDoc("delayer", null, classElement, true);
        ep = new EipOptionModel("delayer", "Delayer", "attribute", "java.lang.String", false, "", docComment, false,
                null, false, null, false, null, false);
        eipOptions.add(ep);

        // autoStartup
        docComment = findJavaDoc("autoStartup", null, classElement, true);
        ep = new EipOptionModel("autoStartup", "Auto Startup", "attribute", "java.lang.String", false, "true", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // startupOrder
        docComment = findJavaDoc("startupOrder", null, classElement, true);
        ep = new EipOptionModel("startupOrder", "Startup Order", "attribute", "java.lang.Integer", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // errorHandlerRef
        docComment = findJavaDoc("errorHandlerRef", null, classElement, true);
        ep = new EipOptionModel("errorHandlerRef", "Error Handler", "attribute", "java.lang.String", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // routePolicyRef
        docComment = findJavaDoc("routePolicyRef", null, classElement, true);
        ep = new EipOptionModel("routePolicyRef", "Route Policy", "attribute", "java.lang.String", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // shutdownRoute
        Set<String> enums = new LinkedHashSet<>();
        enums.add("Default");
        enums.add("Defer");
        docComment = findJavaDoc("shutdownRoute", "Default", classElement, true);
        ep = new EipOptionModel("shutdownRoute", "Shutdown Route", "attribute", "org.apache.camel.ShutdownRoute", false, "", docComment,
                false, null, true, enums, false, null, false);
        eipOptions.add(ep);

        // shutdownRunningTask
        enums = new LinkedHashSet<>();
        enums.add("CompleteCurrentTaskOnly");
        enums.add("CompleteAllTasks");
        docComment = findJavaDoc("shutdownRunningTask", "CompleteCurrentTaskOnly", classElement, true);
        ep = new EipOptionModel("shutdownRunningTask", "Shutdown Running Task", "attribute", "org.apache.camel.ShutdownRunningTask", false, "", docComment,
                false, null, true, enums, false, null, false);
        eipOptions.add(ep);

        // inputs
        Set<String> oneOfTypes = new TreeSet<>();
        oneOfTypes.add("from");
        docComment = findJavaDoc("inputs", null, classElement, true);
        ep = new EipOptionModel("inputs", "Inputs", "element", "java.util.List<org.apache.camel.model.FromDefinition>", true, "", docComment,
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
            AnnotationInstance rootElement = annotation(child, XML_ROOT_ELEMENT).orElse(null);
            if (rootElement != null) {
                String childName = string(rootElement, AP_NAME, "##default");
                oneOfTypes.add(childName);
            }
        }

        // remove some types which are not intended as an output in eips
        oneOfTypes.remove("route");

        docComment = findJavaDoc("outputs", null, classElement, true);
        ep = new EipOptionModel("outputs", "Outputs", "element", "java.util.List<org.apache.camel.model.ProcessorDefinition<?>>", true, "", docComment,
                false, null, false, null, true, oneOfTypes, false);
        eipOptions.add(ep);
    }

    /**
     * Special for process the OptionalIdentifiedDefinition
     */
    private void processIdentified(ClassInfo originalClassType, ClassInfo classElement,
                                   List<EipOptionModel> eipOptions, String prefix) {



        // id
        String docComment = findJavaDoc("id", null, classElement, true);
        EipOptionModel ep = new EipOptionModel("id", "Id", "attribute", "java.lang.String", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // description
        docComment = findJavaDoc(AP_DESCRIPTION, null, classElement, true);
        ep = new EipOptionModel(AP_DESCRIPTION, "Description", "element", "org.apache.camel.model.DescriptionDefinition", false, "", docComment,
                false, null, false, null, false, null, false);
        eipOptions.add(ep);

        // lets skip custom id as it has no value for end users to configure
        if (!skipUnwanted) {
            // custom id
            docComment = findJavaDoc("customId", null, classElement, true);
            ep = new EipOptionModel("customId", "Custom Id", "attribute", "java.lang.String", false, "", docComment,
                    false, null, false, null, false, null, false);
            eipOptions.add(ep);
        }
    }

    /**
     * Special for processing an @XmlElementRef routes field
     */
    private void processRoutes(ClassInfo originalClassType, AnnotationInstance elementRef,
                               FieldInfo fieldElement, String fieldName, List<EipOptionModel> eipOptions, String prefix) {
        if ("routes".equals(fieldName)) {

            String fieldTypeName = toString(fieldElement.type());

            Set<String> oneOfTypes = new TreeSet<>();
            oneOfTypes.add("route");

            EipOptionModel ep = new EipOptionModel("routes", "Routes", "element", fieldTypeName, false, "", "Contains the Camel routes",
                    false, null, false, null, true, oneOfTypes, false);
            eipOptions.add(ep);
        }
    }

    /**
     * Special for processing an @XmlElementRef rests field
     */
    private void processRests(ClassInfo originalClassType, AnnotationInstance elementRef,
                              FieldInfo fieldElement, String fieldName, List<EipOptionModel> eipOptions, String prefix) {
        if ("rests".equals(fieldName)) {

            String fieldTypeName = toString(fieldElement.type());

            Set<String> oneOfTypes = new TreeSet<>();
            oneOfTypes.add("rest");

            EipOptionModel ep = new EipOptionModel("rests", "Rests", "element", fieldTypeName,
                    false, "", "Contains the rest services defined using the rest-dsl",
                    false, null, false, null, true,
                    oneOfTypes, false);
            eipOptions.add(ep);
        }
    }

    /**
     * Special for processing an @XmlElementRef outputs field
     */
    private void processOutputs(ClassInfo originalClassType, AnnotationInstance elementRef,
                                FieldInfo fieldElement, String fieldName, List<EipOptionModel> eipOptions, String prefix) {
        if ("outputs".equals(fieldName) && supportOutputs(originalClassType)) {
            String kind = "element";
            String name = string(elementRef, AP_NAME, null);
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
                AnnotationInstance rootElement = annotation(child, XML_ROOT_ELEMENT).orElse(null);
                if (rootElement != null) {
                    String childName = string(rootElement, AP_NAME, "##default");
                    oneOfTypes.add(childName);
                }
            }

            // remove some types which are not intended as an output in eips
            oneOfTypes.remove("route");

            AnnotationInstance metadata = metadata(fieldElement);
            String displayName = string(metadata, AP_DISPLAY_NAME, null);
            boolean deprecated = deprecated(fieldElement);
            String deprecationNote = string(metadata, AP_DEPRECATION_NOTE, null);

            EipOptionModel ep = new EipOptionModel(name, displayName, kind, fieldTypeName, true,
                    "", "", deprecated, deprecationNote, false, null,
                    true, oneOfTypes, false);
            eipOptions.add(ep);
        }
    }

    /**
     * Special for processing an @XmlElementRef verbs field (rest-dsl)
     */
    private void processVerbs(ClassInfo originalClassType, AnnotationInstance elementRef,
                              FieldInfo fieldElement, String fieldName, List<EipOptionModel> eipOptions, String prefix) {



        if ("verbs".equals(fieldName) && supportOutputs(originalClassType)) {
            String kind = "element";
            String name = string(elementRef, AP_NAME, null);
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
                AnnotationInstance rootElement = annotation(child, XML_ROOT_ELEMENT).orElse(null);
                if (rootElement != null) {
                    String childName = string(rootElement, AP_NAME, "##default");
                    oneOfTypes.add(childName);
                }
            }

            AnnotationInstance metadata = metadata(fieldElement);
            String displayName = string(metadata, AP_DISPLAY_NAME, null);
            boolean deprecated = deprecated(fieldElement);
            String deprecationNote = string(metadata, AP_DEPRECATION_NOTE, null);

            EipOptionModel ep = new EipOptionModel(name, displayName, kind, fieldTypeName, true,
                    "", docComment, deprecated, deprecationNote, false, null,
                    true, oneOfTypes, false);
            eipOptions.add(ep);
        }
    }

    /**
     * Special for processing an @XmlElementRef expression field
     */
    private void processRefExpression(ClassInfo originalClassType, ClassInfo classElement,
                                      AnnotationInstance elementRef, FieldInfo fieldElement,
                                      String fieldName, List<EipOptionModel> eipOptions, String prefix) {


        if ("expression".equals(fieldName)) {
            String kind = "expression";
            String name = string(elementRef, AP_NAME, null);
            if (isNullOrEmpty(name) || "##default".equals(name)) {
                name = fieldName;
            }
            name = prefix + name;
            String fieldTypeName = toString(fieldElement.type());

            // find javadoc from original class as it will override the setExpression method where we can provide the javadoc for the given EIP
            String docComment = findJavaDoc(fieldName, name, originalClassType, true);

            // is it used as predicate (check field first and then fallback to its class / original class)
            boolean asPredicate =
                    annotation(fieldElement, AS_PREDICATE).isPresent()
                            || annotation(classElement, AS_PREDICATE).isPresent()
                            || annotation(originalClassType, AS_PREDICATE).isPresent();

            // gather oneOf expression/predicates which uses language
            Set<String> oneOfTypes = new TreeSet<>();
            for (String language : ONE_OF_LANGUAGES) {
                ClassInfo languages = findTypeElement(language);
                // find all classes that has that superClassName
                for (ClassInfo child : index.getAllKnownSubclasses(languages.name())) {
                    AnnotationInstance rootElement = annotation(child, XML_ROOT_ELEMENT).orElse(null);
                    if (rootElement != null) {
                        String childName = string(rootElement, AP_NAME, "##default");
                        oneOfTypes.add(childName);
                    }
                }
            }

            AnnotationInstance metadata = metadata(fieldElement);
            String displayName = string(metadata, AP_DISPLAY_NAME, null);
            boolean deprecated = deprecated(fieldElement);
            String deprecationNote = string(metadata, AP_DEPRECATION_NOTE, null);

            EipOptionModel ep = new EipOptionModel(name, displayName, kind, fieldTypeName, true,
                    "", docComment, deprecated, deprecationNote, false, null,
                    true, oneOfTypes, asPredicate);
            eipOptions.add(ep);
        }
    }

    /**
     * Special for processing an @XmlElementRef when field
     */
    private void processRefWhenClauses(ClassInfo originalClassType, AnnotationInstance elementRef,
                                       FieldInfo fieldElement, String fieldName, List<EipOptionModel> eipOptions, String prefix) {
        if ("whenClauses".equals(fieldName)) {
            String kind = "element";
            String name = string(elementRef, AP_NAME, "");
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

            AnnotationInstance metadata = metadata(fieldElement);
            String displayName = string(metadata, AP_DISPLAY_NAME, null);
            boolean deprecated = deprecated(fieldElement);
            String deprecationNote = string(metadata, AP_DEPRECATION_NOTE, null);

            EipOptionModel ep = new EipOptionModel(name, displayName, kind, fieldTypeName, false, "", docComment, deprecated, deprecationNote, false, null, true, oneOfTypes, asPredicate);
            eipOptions.add(ep);
        }
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
        AnnotationInstance metadata = metadata(fieldElement);
        String defaultValue = string(metadata, AP_DEFAULT_VALUE, null);
        if (isNullOrEmpty(defaultValue)) {
            // if its a boolean type, then we use false as the default
            if ("boolean".equals(fieldTypeName) || "java.lang.Boolean".equals(fieldTypeName)) {
                defaultValue = "false";
            }
        }
        return defaultValue;
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

    private boolean hasOutput(EipModel model) {
        // if we are route/rest then we accept output
        if ("route".equals(model.getName()) || "rest".equals(model.getName())) {
            return true;
        }

        // special for transacted/policy which should not have output
        if ("policy".equals(model.getName()) || "transacted".equals(model.getName())) {
            return false;
        }

        for (EipOptionModel option : model.getEipOptions()) {
            if ("outputs".equals(option.getName())) {
                return true;
            }
        }
        return false;
    }


    void findTypeElementChildren(Set<ClassInfo> found, String superClassName) {
        found.addAll(getIndex().getAllKnownSubclasses(DotName.createSimple(superClassName)));
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

    private static boolean required(FieldInfo fieldElement, boolean defaultValue) {
        return required(metadata(fieldElement), defaultValue);
    }

    private static boolean required(AnnotationInstance ai, boolean defaultValue) {
        return bool(ai, AP_REQUIRED, defaultValue);
    }

    private static boolean deprecated(MethodInfo method) {
        return annotation(method, DEPRECATED).isPresent();
    }

    private static boolean deprecated(FieldInfo field) {
        return annotation(field, DEPRECATED).isPresent();
    }

    private static boolean deprecated(ClassInfo clazz) {
        return annotation(clazz, DEPRECATED).isPresent();
    }

    private static AnnotationInstance metadata(MethodInfo method) {
        return annotation(method, METADATA).orElse(null);
    }

    private static AnnotationInstance metadata(FieldInfo field) {
        return annotation(field, METADATA).orElse(null);
    }

    private static AnnotationInstance metadata(ClassInfo classElement) {
        return annotation(classElement, METADATA).orElse(null);
    }

    private static String createJsonSchema(OtherModel otherModel) {
        StringBuilder buffer = new StringBuilder("{");
        // language model
        buffer.append("\n  \"other\": {");
        buffer.append("\n    \"name\": \"").append(otherModel.getName()).append("\",");
        buffer.append("\n    \"kind\": \"").append("other").append("\",");
        if (otherModel.getTitle() != null) {
            buffer.append("\n    \"title\": \"").append(otherModel.getTitle()).append("\",");
        }
        if (otherModel.getDescription() != null) {
            buffer.append("\n    \"description\": \"").append(otherModel.getDescription()).append("\",");
        }
        buffer.append("\n    \"deprecated\": \"").append(otherModel.isDeprecated()).append("\",");
        if (otherModel.getFirstVersion() != null) {
            buffer.append("\n    \"firstVersion\": \"").append(otherModel.getFirstVersion()).append("\",");
        }
        if (otherModel.getLabel() != null) {
            buffer.append("\n    \"label\": \"").append(otherModel.getLabel()).append("\",");
        }
        buffer.append("\n    \"groupId\": \"").append(otherModel.getGroupId()).append("\",");
        buffer.append("\n    \"artifactId\": \"").append(otherModel.getArtifactId()).append("\",");
        buffer.append("\n    \"version\": \"").append(otherModel.getVersion()).append("\"");
        buffer.append("\n  }");
        buffer.append("\n}\n");
        return buffer.toString();
    }

    private static String createJsonSchema(EipModel eipModel) {
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
        for (EipOptionModel entry : eipModel.getEipOptions()) {
            if (!eipModel.isOutput() && "outputs".equals(entry.getName())) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                buffer.append(",");
            }
            buffer.append("\n    ");

            // as its json we need to sanitize the docs
            String doc = entry.getDescription();
            doc = sanitizeDescription(doc, false);
            String type = JSonSchemaHelper.getType(entry.getType(), entry.isEnumType());
            JSonSchemaHelper.toJson(buffer, entry.getName(), entry.getDisplayName(), entry.getKind(), entry.isRequired(), type, entry.getType(), entry.getDefaultValue(), doc,
                    entry.isDeprecated(), entry.getDeprecationNote(), false, null, null, entry.isEnumType(), entry.getEnums(), entry.isOneOf(), entry.getOneOfTypes(),
                    entry.isAsPredicate(), null, null, false);
        }
        buffer.append("\n  }");

        buffer.append("\n}\n");
        return buffer.toString();
    }

    private static String createJsonSchema(ComponentModel componentModel) {
        StringBuilder buffer = new StringBuilder("{");
        // component model
        buffer.append("\n  \"component\": {");
        buffer.append("\n    \"kind\": \"").append("component").append("\",");
        buffer.append("\n    \"scheme\": \"").append(componentModel.getScheme()).append("\",");
        if (!isNullOrEmpty(componentModel.getExtendsScheme())) {
            buffer.append("\n    \"extendsScheme\": \"").append(componentModel.getExtendsScheme()).append("\",");
        }
        if (!isNullOrEmpty(componentModel.getAlternativeSchemes())) {
            buffer.append("\n    \"alternativeSchemes\": \"").append(componentModel.getAlternativeSchemes()).append("\",");
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
        buffer.append("\n    \"consumerOnly\": ").append(componentModel.isConsumerOnly()).append(",");
        buffer.append("\n    \"producerOnly\": ").append(componentModel.isProducerOnly()).append(",");
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
            // as its json we need to sanitize the docs
            doc = JSonSchemaHelper.sanitizeDescription(doc, false);
            boolean required = entry.isRequired();
            String defaultValue = entry.getDefaultValue();
            if (isNullOrEmpty(defaultValue) && "boolean".equals(entry.getJavaType())) {
                // fallback as false for boolean types
                defaultValue = "false";
            }

            // component options do not have prefix
            String optionalPrefix = "";
            String prefix = "";
            boolean multiValue = false;
            boolean asPredicate = false;

            JSonSchemaHelper.toJson(buffer, entry.getName(), entry.getDisplayName(), "property", required, entry.getType(), entry.getJavaType(), defaultValue, doc,
                    entry.isDeprecated(), entry.getDeprecationNote(), entry.isSecret(), entry.getGroup(), entry.getLabel(), entry.isEnumType(), entry.getEnums(),
                    false, null, asPredicate, optionalPrefix, prefix, multiValue);
        }
        buffer.append("\n  },");

        buffer.append("\n  \"properties\": {");
        first = true;

        // sort the endpoint options in the standard order we prefer
        List<EndpointOptionModel> paths = new ArrayList<>(componentModel.getEndpointPathOptions());
        paths.sort(EndpointHelper.createPathComparator(componentModel.getSyntax()));

        // include paths in the top
        for (EndpointOptionModel entry : paths) {
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
            String doc = entry.getDescription();
            // as its json we need to sanitize the docs
            doc = JSonSchemaHelper.sanitizeDescription(doc, false);
            boolean required = entry.isRequired();
            String defaultValue = entry.getDefaultValue();
            if (isNullOrEmpty(defaultValue) && "boolean".equals(entry.getJavaType())) {
                // fallback as false for boolean types
                defaultValue = "false";
            }

            // @UriPath options do not have prefix
            String optionalPrefix = "";
            String prefix = "";
            boolean multiValue = false;
            boolean asPredicate = false;

            JSonSchemaHelper.toJson(buffer, entry.getName(), entry.getDisplayName(), "path", required, entry.getType(), entry.getJavaType(), defaultValue, doc,
                    entry.isDeprecated(), entry.getDeprecationNote(), entry.isSecret(), entry.getGroup(), entry.getLabel(), entry.isEnumType(), entry.getEnums(),
                    false, null, asPredicate, optionalPrefix, prefix, multiValue);
        }

        // sort the endpoint options in the standard order we prefer
        List<EndpointOptionModel> options = componentModel.getEndpointOptions();
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
            // as its json we need to sanitize the docs
            doc = JSonSchemaHelper.sanitizeDescription(doc, false);
            boolean required = entry.isRequired();
            String defaultValue = entry.getDefaultValue();
            if (isNullOrEmpty(defaultValue) && "boolean".equals(entry.getJavaType())) {
                // fallback as false for boolean types
                defaultValue = "false";
            }
            String optionalPrefix = entry.getOptionalPrefix();
            String prefix = entry.getPrefix();
            boolean multiValue = entry.isMultiValue();
            boolean asPredicate = false;

            JSonSchemaHelper.toJson(buffer, entry.getName(), entry.getDisplayName(), "parameter", required, entry.getType(), entry.getJavaType(), defaultValue,
                    doc, entry.isDeprecated(), entry.getDeprecationNote(), entry.isSecret(), entry.getGroup(), entry.getLabel(), entry.isEnumType(), entry.getEnums(),
                    false, null, asPredicate, optionalPrefix, prefix, multiValue);
        }
        buffer.append("\n  }");

        buffer.append("\n}\n");
        return buffer.toString();
    }

    private static String createJsonSchema(DataFormatModel dataFormatModel, String schema) {
        StringBuilder buffer = new StringBuilder("{");
        // dataformat model
        buffer.append("\n  \"dataformat\": {");
        buffer.append("\n    \"name\": \"").append(dataFormatModel.getName()).append("\",");
        buffer.append("\n    \"kind\": \"").append("dataformat").append("\",");
        buffer.append("\n    \"modelName\": \"").append(dataFormatModel.getModelName()).append("\",");
        if (dataFormatModel.getTitle() != null) {
            buffer.append("\n    \"title\": \"").append(dataFormatModel.getTitle()).append("\",");
        }
        if (dataFormatModel.getDescription() != null) {
            buffer.append("\n    \"description\": \"").append(dataFormatModel.getDescription()).append("\",");
        }
        buffer.append("\n    \"deprecated\": ").append(dataFormatModel.isDeprecated()).append(",");
        if (dataFormatModel.getFirstVersion() != null) {
            buffer.append("\n    \"firstVersion\": \"").append(dataFormatModel.getFirstVersion()).append("\",");
        }
        buffer.append("\n    \"label\": \"").append(dataFormatModel.getLabel()).append("\",");
        buffer.append("\n    \"javaType\": \"").append(dataFormatModel.getJavaType()).append("\",");
        if (dataFormatModel.getModelJavaType() != null) {
            buffer.append("\n    \"modelJavaType\": \"").append(dataFormatModel.getModelJavaType()).append("\",");
        }
        buffer.append("\n    \"groupId\": \"").append(dataFormatModel.getGroupId()).append("\",");
        buffer.append("\n    \"artifactId\": \"").append(dataFormatModel.getArtifactId()).append("\",");
        buffer.append("\n    \"version\": \"").append(dataFormatModel.getVersion()).append("\"");
        buffer.append("\n  },");

        buffer.append("\n  \"properties\": {");
        buffer.append(schema);
        return buffer.toString();
    }

    private static String createJsonSchema(LanguageModel languageModel, String schema) {
        StringBuilder buffer = new StringBuilder("{");
        // language model
        buffer.append("\n  \"language\": {");
        buffer.append("\n    \"name\": \"").append(languageModel.getName()).append("\",");
        buffer.append("\n    \"kind\": \"").append("language").append("\",");
        buffer.append("\n    \"modelName\": \"").append(languageModel.getModelName()).append("\",");
        if (languageModel.getTitle() != null) {
            buffer.append("\n    \"title\": \"").append(languageModel.getTitle()).append("\",");
        }
        if (languageModel.getDescription() != null) {
            buffer.append("\n    \"description\": \"").append(languageModel.getDescription()).append("\",");
        }
        buffer.append("\n    \"deprecated\": ").append(languageModel.isDeprecated()).append(",");
        if (languageModel.getFirstVersion() != null) {
            buffer.append("\n    \"firstVersion\": \"").append(languageModel.getFirstVersion()).append("\",");
        }
        buffer.append("\n    \"label\": \"").append(languageModel.getLabel()).append("\",");
        buffer.append("\n    \"javaType\": \"").append(languageModel.getJavaType()).append("\",");
        if (languageModel.getModelJavaType() != null) {
            buffer.append("\n    \"modelJavaType\": \"").append(languageModel.getModelJavaType()).append("\",");
        }
        buffer.append("\n    \"groupId\": \"").append(languageModel.getGroupId()).append("\",");
        buffer.append("\n    \"artifactId\": \"").append(languageModel.getArtifactId()).append("\",");
        buffer.append("\n    \"version\": \"").append(languageModel.getVersion()).append("\"");
        buffer.append("\n  },");

        buffer.append("\n  \"properties\": {");
        buffer.append(schema);
        return buffer.toString();
    }

    public abstract List<String> getClasspath();

    protected abstract List<String> getCamelCoreLocations();

    protected abstract List<String> getResources();

    protected abstract List<String> getCompileSourceRoots();

    protected abstract String getOutputDirectory();

    protected abstract String getGroupId();

    protected abstract String getArtifactId();

    protected abstract String getVersion();

    protected abstract String getName();

    protected abstract String getDescription();

    protected abstract String getProperty(String key);

    protected abstract void addResourceDirectory(Path resourceDirectory);

    protected abstract void addCompileSourceRoot(String dir);

    protected abstract void refresh(Path file);

    protected abstract boolean isDebugEnabled();

    protected abstract void debug(String message);

    protected abstract void info(String message);

    protected abstract void warn(String message);

}
