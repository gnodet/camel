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
package org.apache.camel.tooling.packaging.helpers;

import java.io.EOFException;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;

import static org.apache.camel.tooling.packaging.helpers.Strings.isNullOrEmpty;

public class JandexHelper {

    static final int ENUM      = 0x00004000;

    public static Optional<AnnotationValue> value(AnnotationInstance ai, String name) {
        return Optional.ofNullable(ai).map(a -> a.value(name));
    }

    public static Optional<String> string(AnnotationInstance ai, String name) {
        return value(ai, name).map(AnnotationValue::asString)
                .filter(s -> !isNullOrEmpty(s));
    }

    public static String string(AnnotationInstance ai, String name, String def) {
        return string(ai, name).orElse(def);
    }

    public static Optional<Boolean> bool(AnnotationInstance ai, String name) {
        try {
            return value(ai, name).map(AnnotationValue::asBoolean);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to retrieve value '" + name + "' from annotation " + ai.toString(), e);
        }
    }

    public static boolean bool(AnnotationInstance ai, String name, boolean def) {
        return bool(ai, name).orElse(def);
    }

    public static boolean isEnumClass(ClassInfo clazz) {
        return clazz != null && (clazz.flags() & ENUM) != 0;
    }

    public static boolean isEnumConstant(FieldInfo field) {
        return (field.flags() & ENUM) != 0;
    }

    public static Optional<AnnotationInstance> annotation(FieldInfo field, DotName name) {
        return field.annotations().stream()
                .filter(ai -> Objects.equals(ai.name(), name))
                .findAny();
    }

    public static Optional<AnnotationInstance> annotation(MethodInfo method, DotName name) {
        return Optional.ofNullable(method.annotation(name));
    }

    public static Optional<AnnotationInstance> annotation(ClassInfo classElement, DotName name) {
        if (classElement != null) {
            Map<DotName, List<AnnotationInstance>> annotations = classElement.annotations();
            if (annotations.containsKey(name)) {
                return annotations.get(name)
                        .stream()
                        .filter(ai -> ai.target().kind() == Kind.CLASS)
                        .findAny();
            }
        }
        return Optional.empty();
    }

    public static IndexView createIndex(List<String> locations) {
        return CompositeIndex.create(locations.stream()
                .map(JandexHelper::getIndex)
                .collect(Collectors.toList()));
    }

    static class TimeStampedIndex {
        final Path jar;
        final Path idx;

        public TimeStampedIndex(Path jar) {
            this.jar = jar;
            this.idx = Paths.get(System.getProperty("rootBuildDir")).resolve("index-cache")
                .resolve(jar.getFileName().toString() + ".idx");
        }

        public synchronized Index getIndex() {
            try {
                if (Files.exists(idx)) {
                    FileTime ft1 = Files.getLastModifiedTime(jar);
                    FileTime ft2 = Files.getLastModifiedTime(idx);
                    if (ft1.compareTo(ft2) < 0) {
                        try (InputStream is = Files.newInputStream(idx)) {
                            return new IndexReader(is).read();
                        } catch (Exception e) {
                            // Ignore
                            Files.deleteIfExists(idx);
                        }
                    }
                }
                // System.out.println("Creating index: " + idx.getFileName());
                Index index = createIndex(jar.toString());
                Files.createDirectories(idx.getParent());
                try (OutputStream os = Files.newOutputStream(idx)) {
                    new IndexWriter(os).write(index);
                } catch (Exception e) {
                    // ignore
                    Files.deleteIfExists(idx);
                }
                return index;
            } catch (IOException e) {
                throw new RuntimeException("Error indexing: " + jar.toString(), e);
            }
        }
    }

    private static final Map<Path, TimeStampedIndex> INDICES = new ConcurrentHashMap<>();

    private static Index getIndex(String location) {
        if (location.endsWith(".jar")) {
            Path loc = Paths.get(location);
            if (Files.isRegularFile(loc)) {
                return INDICES.computeIfAbsent(loc, TimeStampedIndex::new).getIndex();
            } else {
                return null;
            }
        } else {
            return createIndex(location);
        }
    }

    private static Index createIndex(String location) {
        Indexer indexer = new Indexer();
        Stream.of(IOHelper.asFolder(location))
                .filter(Files::exists)
                .flatMap(IOHelper::walk)
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".class"))
                .forEach(p -> index(indexer, p));
        return indexer.complete();
    }

    private static void index(Indexer indexer, Path p) {
        try (InputStream is = Files.newInputStream(p)) {
            indexer.index(is);
        } catch (IOException e) {
            throw new RuntimeException("Error indexing " + p.getFileSystem().toString() + "/" + p.toString(), e);
        }
    }
}
