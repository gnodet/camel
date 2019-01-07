package org.apache.camel.tooling.packaging.helpers;

import java.io.Closeable;
import java.io.IOError;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.json.simple.JsonObject;
import org.json.simple.Jsoner;

public class IOHelper {

    private static final ThreadLocal<Map<Path, FileSystem>> fileSystems
            = ThreadLocal.withInitial(ConcurrentHashMap::new);

    public static JsonObject toJson(Path p) {
        try (Reader r = Files.newBufferedReader(p)) {
            return (JsonObject) Jsoner.deserialize(r);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void closeJarFileSystems() {
        List<FileSystem> fs = new ArrayList<>(fileSystems.get().values());
        fileSystems.remove();
        fs.forEach(IOHelper::close);
    }

    public static Path asFolder(String p) {
        Path path = Paths.get(p);
        if (p.endsWith(".jar")) {
            FileSystem fs = fileSystems.get().computeIfAbsent(path, IOHelper::newJarFileSystem);
            return fs.getPath("/");
        } else {
            return Paths.get(p);
        }
    }

    public static FileSystem newJarFileSystem(Path path) {
        try {
            return FileSystems.newFileSystem(path, null);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static Stream<Path> list(Path p) {
        try {
            return Files.list(p);
        } catch (IOException e) {
            throw new IOError(e);
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

    public static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

}
