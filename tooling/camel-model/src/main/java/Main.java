/*
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
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.odysseus.staxon.json.JsonXMLConfig;
import de.odysseus.staxon.json.JsonXMLConfigBuilder;
import de.odysseus.staxon.json.JsonXMLInputFactory;
import de.odysseus.staxon.xml.util.PrettyXMLEventWriter;
import de.odysseus.staxon.xml.util.PrettyXMLStreamWriter;
import org.apache.camel.metamodel.AbstractData;
import org.apache.camel.metamodel.Endpoint;
import org.apache.camel.metamodel.Model;
import org.apache.camel.metamodel.Property;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class Main {

    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
    private static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();

    private static final Map<String, AbstractData> fqns = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Path camelRoot = findCamelRoot(Paths.get(".").toAbsolutePath());
        Path catalog = camelRoot.resolve("catalog/camel-catalog/target/classes/org/apache/camel/catalog");

        // Compute models list
        System.out.println("Retrieving models list");
        Map<String, Map<String, Path>> models = new LinkedHashMap<>();
        for (String root : Arrays.asList("components", "dataformats", "languages", "models")) {
            models.put(root, Files.list(catalog.resolve(root)).collect(Collectors.toMap(
                    Main::modelName, p -> p, (u, v) -> v, TreeMap::new)));
        }
        Path json = Paths.get("target/model.json");
        if (needsRegen(json, models.values().stream().map(Map::values).flatMap(Collection::stream))) {
            System.out.println("Creating JSON model");
            try (Writer sw = Files.newBufferedWriter(json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                sw.append("{ \"model\": { ");
                boolean firstRoot = true;
                for (Map.Entry<String, Map<String, Path>> root : models.entrySet()) {
                    if (!firstRoot) {
                        sw.append(",\n");
                    } else {
                        firstRoot = false;
                    }
                    sw.append("\"").append(root.getKey()).append("\": { ");
                    boolean firstModel = true;
                    for (Map.Entry<String, Path> model : root.getValue().entrySet()) {
                        if (!firstModel) {
                            sw.append(",\n");
                        } else {
                            firstModel = false;
                        }
                        sw.append("\"").append(model.getKey()).append("\": ");
                        sw.append(new String(Files.readAllBytes(model.getValue())));
                    }
                    sw.append(" }");
                }
                sw.append(" } }\n");
            }
        }

        Path input = json;
        Path output = Paths.get("target/temp.xml");
        if (needsRegen(output, Stream.of(input))) {
            System.out.println("Converting to XML");
            try (InputStream is = Files.newInputStream(input); OutputStream os = Files.newOutputStream(output)) {
                JsonXMLConfig config = new JsonXMLConfigBuilder().multiplePI(false).build();
                XMLEventReader r = new JsonXMLInputFactory(config).createXMLEventReader(is);
                XMLEventWriter w = new PrettyXMLEventWriter(xmlOutputFactory.createXMLEventWriter(os));
                w.add(r);
                r.close();
                w.close();
            }
        }

        List<String> stylesheets = Arrays.asList("model.xslt", "fixes.xslt");
        for (int step = 0; step < stylesheets.size(); step++) {
            input = output;
            if (step == stylesheets.size() - 1) {
                output = input.resolveSibling("model.xml");
            } else {
                output = input.resolveSibling("step" + step + ".xml");
            }
            Path xslt = Paths.get("src/main/resources", stylesheets.get(step));
            if (needsRegen(output, Stream.of(input, xslt))) {
                System.out.println("Transforming using " + stylesheets.get(step));
                try (StaxInput source = new StaxInput(input);
                     StaxOutput result = new StaxOutput(output, true);
                     StaxInput transformer = new StaxInput(xslt)) {
                    transformerFactory.newTransformer(transformer).transform(source, result);
                }
            }
        }

        XmlMapper xmlMapper = new XmlMapper();
        Model model = xmlMapper.readValue(output.toFile(), Model.class);

//        for (Endpoint endpoint : model.getEndpoints()) {
//            String name = substringBeforeLast(endpoint.getJavaType().substring(endpoint.getJavaType().lastIndexOf('.') + 1), "Component");
//            fqns.put(camelCaseLower(name), "org.apache.camel.model.endpoints." + name + "EndpointBuilderFactory");
//        }

         Stream.of(model.getDataFormats(),
                   model.getLanguages(),
                   model.getVerbs(),
                   model.getLoadBalancers(),
                   model.getDefinitions(),
                   model.getProcessors())
                .flatMap(List::stream)
                .forEach(data -> fqns.put(data.getName(), data));

        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.init();

        Template t = velocityEngine.getTemplate("src/main/resources/endpoint.vm");
        VelocityContext context = new VelocityContext();
        context.put("model", model);
        for (Endpoint endpoint : model.getEndpoints()) {
            String name = substringBeforeLast(endpoint.getJavaType().substring(endpoint.getJavaType().lastIndexOf('.') + 1), "Component");
            String packageName = "org.apache.camel.model.endpoints";
            List<String> schemes = model.getEndpoints().stream().filter(e -> Objects.equals(e.getJavaType(), endpoint.getJavaType()))
                    .map(Endpoint::getName)
                    .collect(Collectors.toList());
            context.put("endpoint", endpoint);
            context.put("name", name);
            context.put("packageName", packageName);
            context.put("schemes", schemes);
            context.put("main", Main.class);
            Path file = Paths.get("target/generated/" + packageName.replace('.', '/') + "/" + name + "EndpointBuilderFactory.java");
            try (StringWriter w = new StringWriter()) {
                try {
                    t.merge(context, w);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to generate endpoint " + name, e);
                }
                w.flush();
                updateResource(file, w.toString());
            }
        }

        // Generate processors
        Template processorTemplate = velocityEngine.getTemplate("src/main/resources/processor.vm");
        model.getProcessors().stream()
                .filter(AbstractData::isGenerate)
                .forEach(data -> doGenerate(model, data, processorTemplate));

        // Generate dataFormats, languages, verbs, loadBalancers, structs
        Template structTemplate = velocityEngine.getTemplate("src/main/resources/struct.vm");
        Stream.of(model.getDataFormats(),
                  model.getLanguages(),
                  model.getVerbs(),
                  model.getLoadBalancers(),
                  model.getDefinitions())
                .flatMap(List::stream)
                .filter(AbstractData::isGenerate)
                .forEach(data -> doGenerate(model, data, structTemplate));

        t = velocityEngine.getTemplate("src/main/resources/stax.vm");
        context = new VelocityContext();
        context.put("model", model);
        context.put("packageName", "org.apache.camel.model.io");
        context.put("main", Main.class);
        Path file = Paths.get("target/generated/org/apache/camel/model/io/ModelParser.java");
        try (StringWriter w = new StringWriter()) {
            try {
                t.merge(context, w);
            } catch (Exception e) {
                throw new RuntimeException("Unable to generate XML parser", e);
            }
            w.flush();
            updateResource(file, w.toString());
        }
    }

    private static void doGenerate(Model model, AbstractData data, Template t) {
        String packageName = substringBeforeLast(data.getJavaType(), ".");
        String name = substringAfterLast(data.getJavaType(), ".");
        Path file = Paths.get("target/generated/" + packageName.replace('.', '/') + "/" + name + ".java");
        StringWriter w = new StringWriter();
        try {
            VelocityContext context = new VelocityContext();
            context.put("model", model);
            context.put("data", data);
            context.put("packageName", packageName);
            context.put("name", name);
            context.put("main", Main.class);
            t.merge(context, w);
        } catch (Exception e) {
            throw new RuntimeException("Unable to generate class " + name, e);
        }
        w.flush();
        updateResource(file, w.toString());
    }

    public static String javadoc(String desc, String indent) {
        StringBuilder sb = new StringBuilder();
        List<String> lines = new ArrayList<>();
        int len = 78 - indent.length();
        String rem = desc;
        if (rem != null) {
            while (rem.length() > 0) {
                int idx = rem.length() >= len ? rem.substring(0, len).lastIndexOf(' ') : -1;
                int idx2 = rem.indexOf('\n');
                if (idx2 >= 0 && (idx < 0 || idx2 < idx || idx2 < len)) {
                    idx = idx2;
                }
                if (idx >= 0) {
                    String s = rem.substring(0, idx);
                    while (s.endsWith(" ")) {
                        s = s.substring(0, s.length() - 1);
                    }
                    String l = rem.substring(idx + 1);
                    while (l.startsWith(" ")) {
                        l = l.substring(1);
                    }
                    lines.add(s);
                    rem = l;
                } else {
                    lines.add(rem);
                    rem = "";
                }
            }
            sb.append("/**\n");
            for (String line : lines) {
                sb.append(indent).append(" * ").append(line).append("\n");
            }
            sb.append(indent).append(" */");
        }
        return sb.toString();
    }

    public static String camelCaseLower(String s) {
        int i;
        while (s != null && (i = s.indexOf('-')) > 0) {
            s = s.substring(0, i) + s.substring(i + 1, i + 2).toUpperCase() + s.substring(i + 2);
        }
        if (s != null) {
            s = s.substring(0, 1).toLowerCase() + s.substring(1);
            switch (s) {
                case "class":
                    s = "clas";
                    break;
                case "package":
                    s = "packag";
                    break;
            }
        }
        return s;
    }

    public static String camelCaseUpper(String s) {
        int i;
        while (s != null && (i = s.indexOf('-')) > 0) {
            s = s.substring(0, i) + s.substring(i + 1, i + 2).toUpperCase() + s.substring(i + 2);
        }
        if (s != null) {
            s = s.substring(0, 1).toUpperCase() + s.substring(1);
        }
        return s;
    }

    public static List<?> newArrayList() {
        return new ArrayList<>();
    }

    public static TreeSet<?> newTreeSet() {
        return new TreeSet<>();
    }

    public static Map<?, ?> newTreeMap() {
        return new TreeMap<>();
    }

    public static String getRealType(String name) {
        AbstractData data = getData(name);
        return data != null ? data.getJavaType() : null;
    }

    public static AbstractData getData(String name) {
        String mname;
        if (name.startsWith("model:")) {
            mname = name.substring("model:".length());
        } else {
            mname = name;
        }
        return fqns.get(mname);
    }

    public static List<AbstractData> getHierarchy(AbstractData data) {
        List<AbstractData> parents = new ArrayList<>();
        for (;;) {
            String extend = data.getExtends();
            if (extend != null) {
                AbstractData parent = getData(extend);
                if (parent != null) {
                    parents.add(parent);
                    data = parent;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        return parents;
    }

    public static Map<String, List<String>> getEnums(AbstractData data) {
        if (data.getProperties() == null) {
            return Collections.emptyMap();
        }
        List<String> enumsStr = data.getProperties().stream().map(Property::getType)
                .flatMap(Main::getSubTypes)
                .filter(s -> s.startsWith("enum:"))
                .collect(Collectors.toList());
        Map<String, List<String>> enums = new HashMap<>();
        for (String s : enumsStr) {
            String t = s.substring("enum:".length(), s.indexOf('('));
            if (!isVisible(t)) {
                String n = t.indexOf('.') >= 0 ? t.substring(t.lastIndexOf('.') + 1) : t;
                String l = s.substring(s.indexOf('(') + 1, s.indexOf(')'));
                enums.put(n, Arrays.asList(l.split(",")));
            }
        }
        return enums;
    }

    public static List<String> getImports(AbstractData data, String packageName) {
        return doGetImports(data, packageName);
    }

    public static String getExtends(AbstractData data) {
        String extend = data.getExtends();
        if (extend == null) {
            extend = "org.apache.camel.model.BaseDefinition";
        } else if (extend.startsWith("model:")) {
            extend = getRealType(extend.substring("model:".length()));
            if (extend == null) {
                extend = "java.lang.Object";
            }
        } else if (extend.startsWith("java:")) {
            extend = extend.substring("java:".length());
        }
        return extend;
    }

    public static List<String> doGetImports(AbstractData data, String packageName) {
        List<String> additional = new ArrayList<>();
        additional.add(getExtends(data));

        return Stream.concat(Stream.concat(getHierarchy(data).stream(), Stream.of(data))
                .map(AbstractData::getProperties)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(Property::getType)
                .flatMap(Main::getSubTypes)
                .map(Main::getImport), additional.stream())
                .filter(Objects::nonNull)
                .filter(s -> !"java.lang".equals(substringBeforeLast(s, ".")))
                .filter(s -> !packageName.equals(substringBeforeLast(s, ".")))
                .filter(s -> s.contains("."))
                .sorted()
                .distinct()
                .collect(Collectors.toList());
    }

    public static String getImport(String s) {
        if ("string".equals(s) || "class".equals(s) || "object".equals(s)
                || "long".equals(s) || "boolean".equals(s) || "char".equals(s)) {
            return "java.lang." + s.substring(0, 1).toUpperCase() + s.substring(1);
        } else if ("int".equals(s)) {
            return "java.lang.Integer";
        } else if (s.startsWith("java:")) {
            String t = substringBefore(s.substring("java:".length()), "<");
            return isVisible(t) ? t : "java.lang.Object";
        } else if (s.startsWith("enum:")) {
            String t = s.substring("enum:".length(), s.indexOf('('));
            return isVisible(t) ? t : "java.lang.Object";
        } else {
            return s;
        }
    }

    public static Stream<String> getSubTypes(String s) {
        if (s.startsWith("list(")) {
            return Stream.concat(Stream.of("java.util.List"), getSubTypes(s.substring(s.indexOf('(') + 1, s.lastIndexOf(')'))));
        } else if (s.startsWith("set(")) {
            return Stream.concat(Stream.of("java.util.Set"), getSubTypes(s.substring(s.indexOf('(') + 1, s.lastIndexOf(')'))));
        } else if (s.startsWith("map(")) {
            String[] t = s.substring(s.indexOf('(') + 1, s.lastIndexOf(')')).split(",");
            return Stream.concat(Stream.of("java.util.Map"), Stream.concat(getSubTypes(t[0]), getSubTypes(t[1])));
        } else if (s.endsWith("[]")) {
            return getSubTypes(s.substring(0, s.length() - 2));
        } else if (s.startsWith("model:")) {
            String t = s.substring("model:".length());
            String rt = getRealType(t);
            if ("expression".equals(t)) {
                return rt != null ? Stream.of(rt, "org.apache.camel.Expression") : Stream.of("org.apache.camel.Expression");
            } else {
                return rt != null ? Stream.of(rt) : Stream.empty();
            }
        } else {
            return Stream.of(s);
        }
    }

    public static String getType(String s) {
        try {
            if (s == null) {
                throw new NullPointerException("Null argument for getType");
            }
            if ("string".equals(s)) {
                return "String";
            } else if ("class".equals(s)) {
                return "Class<?>";
            } else if ("object".equals(s)) {
                return "Object";
            } else if (s.startsWith("list(")) {
                String t = s.substring(s.indexOf('(') + 1, s.lastIndexOf(')'));
                return "List<" + wrap(getType(t)) + ">";
            } else if (s.startsWith("set(")) {
                String t = s.substring(s.indexOf('(') + 1, s.lastIndexOf(')'));
                return "Set<" + wrap(getType(t)) + ">";
            } else if (s.startsWith("map(")) {
                String[] t = s.substring(s.indexOf('(') + 1, s.lastIndexOf(')')).split(",");
                return "Map<" + wrap(getType(t[0])) + ", " + wrap(getType(t[1])) + ">";
            } else if (s.startsWith("java:")) {
                if (s.contains("<") && s.contains(">")) {
                    String t0 = s.substring(0, s.indexOf('<')).substring("java:".length());
                    String[] t1 = s.substring(s.indexOf('<') + 1, s.lastIndexOf('>')).split(",");
                    if (isVisible(t0)) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(t0, t0.lastIndexOf('.') + 1, t0.length())
                                .append("<");
                        for (int i =0; i < t1.length; i++) {
                            if (i > 0) {
                                sb.append(", ");
                            }
                            int idxDot = t1[i].lastIndexOf('.');
                            sb.append(idxDot < 0 ? t1[i] : isVisible(t1[i]) ? t1[i].substring(idxDot + 1) : "Object");
                        }
                        sb.append(">");
                        return sb.toString();
                    } else {
                        return "Object";
                    }
                } else {
                    String t = s.substring("java:".length());
                    return isVisible(t) ? substringAfterLast(t, ".") : "Object";
                }
            } else if (s.startsWith("enum:")) {
                String t = s.substring("enum:".length(), s.indexOf('('));
                return t.lastIndexOf('.') >= 0 ? t.substring(t.lastIndexOf('.') + 1) : t;
            } else if (s.endsWith("[]")) {
                return getType(s.substring(0, s.length() - 2)) + "[]";
            } else if (s.startsWith("model:")) {
                return substringAfterLast(getRealType(s.substring("model:".length())), ".");
            } else {
                return s;
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Unable to compute getType for '" + s + "'", e);
        }
    }

    public static String wrap(String s) {
        switch (s) {
            case "long":
                return "Long";
            case "int":
                return "Integer";
            case "boolean":
                return "Boolean";
            case "char":
                return "Char";
            default:
                return s;
        }
    }

    public static boolean isVisible(String t) {
        if (t.startsWith("java.")) {
            return true;
        }
        if (t.startsWith("org.apache.camel.component.")
                || t.startsWith("org.apache.camel.http")
                || t.startsWith("org.apache.camel.support.processor.validation.")
                || t.startsWith("org.apache.camel.converter.")
                || t.startsWith("org.apache.camel.dataformat.")) {
            return false;
        }
        if (t.startsWith("org.apache.camel.")) {
            return true;
        }
        return false;
    }

    public static String modelName(Path p) {
        return substringBefore(p.getFileName().toString(), ".").replace('+', '-');
    }

    private static FileTime lastModified(Path p) {
        try {
            return Files.getLastModifiedTime(p);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private static boolean needsRegen(Path output, Stream<Path> inputs) {
        FileTime inputLastModified = inputs.map(Main::lastModified).max(Comparator.naturalOrder()).orElse(null);
        if (Files.isRegularFile(output)) {
            if (inputLastModified != null) {
                FileTime outputLastModified = lastModified(output);
                return outputLastModified.compareTo(inputLastModified) < 0;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    static class StaxOutput extends StAXResult implements AutoCloseable {
        Path path;
        OutputStream stream;
        public StaxOutput(Path path, boolean prettyPrint) throws IOException, XMLStreamException {
            this(path, Files.newOutputStream(path), prettyPrint);
        }
        public StaxOutput(Path path, OutputStream stream, boolean prettyPrint) throws XMLStreamException {
            this(path, stream, xmlOutputFactory.createXMLStreamWriter(stream), prettyPrint);
        }
        public StaxOutput(Path path, OutputStream stream, XMLStreamWriter writer, boolean prettyPrint) {
            super(prettyPrint ? new PrettyXMLStreamWriter(writer) : writer);
            this.path = path;
            this.stream = stream;
        }
        @Override
        public void close() throws Exception {
            getXMLStreamWriter().close();
            this.stream.close();
        }
        @Override
        public String getSystemId() {
            return path.toUri().toString();
        }
    }

    static class StaxInput extends StAXSource implements AutoCloseable {
        Path path;
        InputStream stream;
        public StaxInput(Path path) throws IOException, XMLStreamException {
            this(path, Files.newInputStream(path));
        }
        public StaxInput(Path path, InputStream stream) throws XMLStreamException {
            this(path, stream, xmlInputFactory.createXMLStreamReader(path.toUri().toString(), stream));
        }
        public StaxInput(Path path, InputStream stream, XMLStreamReader reader) {
            super(reader);
            this.path = path;
            this.stream = stream;
        }
        @Override
        public void close() throws Exception {
            getXMLStreamReader().close();
            stream.close();
        }
        @Override
        public String getSystemId() {
            return path.toUri().toString();
        }
    }

    private static Path findCamelRoot(Path dir) {
        return Files.isDirectory(dir.resolve("core/camel-core"))
                ? dir : findCamelRoot(dir.getParent());
    }

    public static String substringBefore(String s, String c) {
        int idx = s.indexOf(c);
        return idx > 0 ? s.substring(0, idx) : s;
    }

    public static String substringBeforeLast(String s, String c) {
        int idx = s.lastIndexOf(c);
        return idx >= 0 ? s.substring(0, idx) : "";
    }

    public static String substringAfterLast(final String s, final String c) {
        return s.substring(s.lastIndexOf(c) + c.length());
    }
    public static void updateResource(Path out, String data) {
        try {
            if (data == null) {
                if (Files.isRegularFile(out)) {
                    Files.delete(out);
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
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

}
