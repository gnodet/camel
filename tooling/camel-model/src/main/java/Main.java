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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
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
import model.Model;
import model.Model.Endpoint;
import model.Model.Property;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class Main {

    static final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
    static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
    static final TransformerFactory transformerFactory = TransformerFactory.newInstance();

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

        List<String> stylesheets = Arrays.asList("model.xslt", "fixes.xslt" /*, "generator.xslt"*/);
        for (int step = 0; step < stylesheets.size(); step++) {
            input = output;
            output = input.resolveSibling("step" + step + ".xml");
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

        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.init();

        Template t = velocityEngine.getTemplate("src/main/resources/endpoint.vm");
        VelocityContext context = new VelocityContext();
        context.put("model", model);
        for (Endpoint endpoint : model.getEndpoints()) {
            String name = substringBefore(endpoint.getJavaType().substring(endpoint.getJavaType().lastIndexOf('.') + 1), "Component");
            List<String> schemes = model.getEndpoints().stream().filter(e -> Objects.equals(e.getJavaType(), endpoint.getJavaType()))
                    .map(Endpoint::getName)
                    .collect(Collectors.toList());
            context.put("endpoint", endpoint);
            context.put("name", name);
            context.put("schemes", schemes);
            context.put("main", Main.class);
            Path file = Paths.get("target/generated/org/apache/camel/model/endpoints/" + name + "EndpointBuilderFactory.java");
            try (StringWriter w = new StringWriter()) {
                t.merge(context, w);
                w.flush();
                updateResource(file, w.toString());
            }
        }

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
            if ("class".equals(s)) {
                s = "clazz";
            }
        }
        return s;
    }

    public static Map<String, List<String>> getEnums(Model.AbstractData data) {
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
                List<String> vl = new ArrayList<>();
                for (String v : l.split(",")) {
//                    if (v.indexOf(':') > 0) {
//                        vl.add(v.substring(0, v.indexOf(':')));
//                    } else {
                        vl.add(v);
//                    }
                }
                enums.put(n, vl);
            }
        }
        return enums;
    }

    public static List<String> getImports(Model.AbstractData data) {
        return data.getProperties().stream().map(Property::getType)
                .flatMap(Main::getSubTypes)
                .map(Main::getImport)
                .filter(s -> !s.startsWith("java.lang."))
                .filter(s -> !s.startsWith("org.apache.camel.model.endpoints."))
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
            String t = s.substring("java:".length() + 1);
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
        } else {
            return Stream.of(s);
        }
    }

    public static String getType(String s) {
        try {
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
                String t = s.substring("java:".length());
                if (s.contains("<") && s.contains(">")) {
                    String t0 = s.substring(0, s.indexOf('<'));
                    String t1 = s.substring(s.indexOf('<') + 1, s.lastIndexOf('>'));
                    if (isVisible(t0)) {
                        t0 = t0.substring(t0.lastIndexOf('.') + 1);
                        if (isVisible(t1)) {
                            t1 = t1.substring(t1.lastIndexOf('.') + 1);
                            return t0 + "<" + t1 + ">";
                        } else {
                            return t0 + "<Object>";
                        }
                    } else {
                        return "Object";
                    }
                } else {
                    return isVisible(t) ? t : "Object";
                }
            } else if (s.startsWith("enum:")) {
                String t = s.substring("enum:".length(), s.indexOf('('));
                return t.lastIndexOf('.') >= 0 ? t.substring(t.lastIndexOf('.') + 1) : t;
            } else if (s.endsWith("[]")) {
                return getType(s.substring(0, s.length() - 2)) + "[]";
            } else {
                return s;
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Unable to compute getType for '" + s + "'");
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
                || t.startsWith("org.apache.camel.converter.")) {
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

    private static String substringBefore(String s, String c) {
        return s.substring(0, s.indexOf(c));
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
