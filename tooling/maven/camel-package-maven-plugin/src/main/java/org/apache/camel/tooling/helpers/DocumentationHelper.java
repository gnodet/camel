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
package org.apache.camel.tooling.helpers;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.camel.tooling.Generator;

import static org.apache.camel.tooling.helpers.JSonSchemaHelper.parseJsonSchema;

/**
 * Helper to find documentation for inherited options when a component extends another.
 */
public final class DocumentationHelper {

    private DocumentationHelper() {
        //utility class, never constructed
    }

    public static String findComponentPropertyJavaDoc(Generator project, String name, String property) {
        return findJavaDoc(project, name, property, "componentProperties");
    }

    public static String findEndpointPropertyJavaDoc(Generator project, String name, String property) {
        return findJavaDoc(project, name, property, "properties");
    }

    public static String findJavaDoc(Generator project, String name, String property, String group) {
        Path path = jsonFile(project, "component", name);
        if (path != null) {
            try {
                String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                List<Map<String, String>> rows = parseJsonSchema(group, json, true);
                return getPropertyDescription(rows, property);
            } catch (IOException e) {
                throw new IOError(e);
            }
        }

        // not found
        return null;
    }

    private static String getPropertyDescription(List<Map<String, String>> rows, String name) {
        for (Map<String, String> row : rows) {
            String description = null;
            boolean found = false;
            if (row.containsKey("name")) {
                found = name.equals(row.get("name"));
            }
            if (row.containsKey("description")) {
                description = row.get("description");
            }
            if (found) {
                return description;
            }
        }
        return null;
    }

    public static Path jsonFile(Generator project, String type, String name) {
        Path cp = project.getRuntimeClasspathElements().stream()
                .map(IOHelper::asFolder)
                .map(p -> p.resolve("META-INF/services/org/apache/camel").resolve(type).resolve(name))
                .filter(Files::isRegularFile)
                .findAny()
                .orElse(null);
        if (cp != null) {
            String cc = readClassFromCamelResource(cp);
            if (cc != null) {
                int idx = cc.lastIndexOf('.');
                cc = cc.substring(0, idx);
                Path jp = cp.resolve("/").resolve(cc.replace('.', '/')).resolve(name + ".json");
                if (Files.isRegularFile(jp)) {
                    return jp;
                }
            }
        }
        return null;
    }

    public static String readClassFromCamelResource(Path cp) {
        return IOHelper.lines(cp)
            .filter(s -> s.startsWith("class="))
            .map(s -> s.substring("class=".length()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Loads the entire stream into memory as a String and returns it.
     * <p/>
     * <b>Notice:</b> This implementation appends a <tt>\n</tt> as line
     * terminator at the of the text.
     * <p/>
     * Warning, don't use for crazy big streams :)
     */
    private static String loadText(InputStream in) throws IOException {
        StringBuilder builder = new StringBuilder();
        InputStreamReader isr = new InputStreamReader(in);
        try {
            BufferedReader reader = new LineNumberReader(isr);
            while (true) {
                String line = reader.readLine();
                if (line != null) {
                    builder.append(line);
                    builder.append("\n");
                } else {
                    break;
                }
            }
            return builder.toString();
        } finally {
            isr.close();
            in.close();
        }
    }

}
