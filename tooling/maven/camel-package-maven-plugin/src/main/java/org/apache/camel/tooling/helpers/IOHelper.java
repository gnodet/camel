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

import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Stream;

public class IOHelper {

    public static Path asFolder(String p) {
        if (p.endsWith(".jar")) {
            URI uri = URI.create("jar:file:" + p + "!/");
            FileSystem fs;
            try {
                fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
            } catch (FileSystemAlreadyExistsException e) {
                fs = FileSystems.getFileSystem(uri);
            } catch (IOException e) {
                throw new IOError(e);
            }
            return fs.getPath("/");
        } else {
            return Paths.get(p);
        }
    }

    public static Stream<Path> walk(Path p) {
        try {
            return Files.walk(p);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static Stream<String> lines(Path p) {
        try {
            return Files.lines(p);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
}
