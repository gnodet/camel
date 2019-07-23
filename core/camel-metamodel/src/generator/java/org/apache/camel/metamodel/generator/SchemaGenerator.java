package org.apache.camel.metamodel.generator;/*
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
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

import de.odysseus.staxon.json.JsonXMLConfig;
import de.odysseus.staxon.json.JsonXMLConfigBuilder;
import de.odysseus.staxon.json.JsonXMLInputFactory;
import de.odysseus.staxon.xml.util.PrettyXMLEventWriter;
import de.odysseus.staxon.xml.util.PrettyXMLStreamWriter;

public class SchemaGenerator {

    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
    private static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();

    public static void main(String[] args) throws Exception {
        Path cur = Paths.get(".").toAbsolutePath().normalize();
        while (!Files.isDirectory(cur.resolve("core/camel-metamodel"))) {
            cur = cur.getParent();
        }
        cur = cur.resolve("core/camel-metamodel");
        Path catalog = cur.resolve("src/generator/resources");

        // Compute models list
        Map<String, Map<String, Path>> models = new LinkedHashMap<>();
        for (String root : Arrays.asList("components", "dataformats", "languages", "models")) {
            models.put(root, Files.list(catalog.resolve(root)).collect(Collectors.toMap(
                    SchemaGenerator::modelName, p -> p, (u, v) -> v, TreeMap::new)));
        }
        Path json = cur.resolve("target/model.json");
        if (needsRegen(json, models.values().stream().map(Map::values).flatMap(Collection::stream))) {
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
        Path output = cur.resolve("target/temp.xml");
        if (needsRegen(output, Stream.of(input))) {
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
                output = cur.resolve("target/generated/org/apache/camel/metamodel/metamodel.xml");
                Files.createDirectories(output.getParent());
            } else {
                output = input.resolveSibling("step" + step + ".xml");
            }
            Path xslt = catalog.resolve(stylesheets.get(step));
            if (needsRegen(output, Stream.of(input, xslt))) {
                try (StaxInput source = new StaxInput(input);
                     StaxOutput result = new StaxOutput(output, true);
                     StaxInput transformer = new StaxInput(xslt)) {
                    transformerFactory.newTransformer(transformer).transform(source, result);
                }
            }
        }
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
        FileTime inputLastModified = inputs.map(SchemaGenerator::lastModified).max(Comparator.naturalOrder()).orElse(null);
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

    public static String substringBefore(String s, String c) {
        int idx = s.indexOf(c);
        return idx > 0 ? s.substring(0, idx) : s;
    }

}
