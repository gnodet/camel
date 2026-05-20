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
package org.apache.camel.dsl.jbang.core.commands.mcp;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.mcp.server.ToolCallException;
import org.apache.camel.dsl.jbang.core.common.CommandLineHelper;
import org.apache.camel.dsl.jbang.core.common.PathUtils;
import org.apache.camel.dsl.jbang.core.common.ProcessHelper;
import org.apache.camel.support.PatternHelper;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.StopWatch;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;

/**
 * Shared service for discovering running Camel processes and communicating with them via the file-based IPC protocol.
 * <p>
 * Uses the multi-client action file protocol: each request gets a unique ID, so concurrent MCP clients don't interfere
 * with each other.
 */
@ApplicationScoped
public class RuntimeService {

    private static final long ACTION_TIMEOUT_MS = 10_000;
    private static final long POLL_INTERVAL_MS = 100;

    public record ProcessInfo(long pid, String name, String contextName) {
    }

    public List<ProcessInfo> discoverProcesses() {
        List<ProcessInfo> result = new ArrayList<>();
        Path camelDir = CommandLineHelper.getCamelDir();
        File dir = camelDir.toFile();
        if (!dir.isDirectory()) {
            return result;
        }

        File[] statusFiles = dir.listFiles((d, name) -> name.matches("\\d+-status\\.json"));
        if (statusFiles == null) {
            return result;
        }

        for (File sf : statusFiles) {
            String fileName = sf.getName();
            long pid = Long.parseLong(fileName.substring(0, fileName.indexOf('-')));

            if (!ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) {
                continue;
            }

            try {
                JsonObject root = readStatusFromFile(sf.toPath());
                if (root != null) {
                    String name = ProcessHelper.extractName(root, ProcessHandle.of(pid).orElse(null));
                    String contextName = null;
                    JsonObject context = (JsonObject) root.get("context");
                    if (context != null) {
                        contextName = context.getString("name");
                    }
                    result.add(new ProcessInfo(pid, name, contextName));
                }
            } catch (Exception e) {
                // skip unreadable status files
            }
        }

        return result;
    }

    public ProcessInfo findSingleProcess(String nameOrPid) {
        List<ProcessInfo> processes = discoverProcesses();

        if (processes.isEmpty()) {
            throw new ToolCallException("No running Camel processes found", null);
        }

        if (nameOrPid != null && !nameOrPid.isBlank()) {
            if (nameOrPid.matches("\\d+")) {
                long pid = Long.parseLong(nameOrPid);
                return processes.stream()
                        .filter(p -> p.pid() == pid)
                        .findFirst()
                        .orElseThrow(() -> new ToolCallException("No Camel process found with PID: " + nameOrPid, null));
            }
            String pattern = nameOrPid.endsWith("*") ? nameOrPid : nameOrPid + "*";
            List<ProcessInfo> matched = processes.stream()
                    .filter(p -> (p.name() != null && PatternHelper.matchPattern(FileUtil.onlyName(p.name()), pattern))
                            || (p.contextName() != null && PatternHelper.matchPattern(p.contextName(), pattern)))
                    .toList();
            if (matched.isEmpty()) {
                throw new ToolCallException("No Camel process found matching: " + nameOrPid, null);
            }
            if (matched.size() > 1) {
                throw new ToolCallException(
                        "Multiple Camel processes match '" + nameOrPid + "': "
                                            + matched.stream().map(p -> p.name() + " (PID " + p.pid() + ")").toList()
                                            + ". Specify a more specific name or PID.",
                        null);
            }
            return matched.get(0);
        }

        if (processes.size() > 1) {
            throw new ToolCallException(
                    "Multiple Camel processes running: "
                                        + processes.stream().map(p -> p.name() + " (PID " + p.pid() + ")").toList()
                                        + ". Specify nameOrPid to select one.",
                    null);
        }

        return processes.get(0);
    }

    public JsonObject readStatus(long pid) {
        Path statusFile = CommandLineHelper.getCamelDir().resolve(pid + "-status.json");
        return readStatusFromFile(statusFile);
    }

    public JsonObject readStatusSection(long pid, String section) {
        JsonObject root = readStatus(pid);
        if (root == null) {
            throw new ToolCallException("No status available for PID " + pid, null);
        }
        Object value = root.get(section);
        if (value instanceof JsonObject jo) {
            return jo;
        }
        if (value != null) {
            JsonObject wrapper = new JsonObject();
            wrapper.put(section, value);
            return wrapper;
        }
        return new JsonObject();
    }

    public JsonObject executeAction(long pid, String action, Consumer<JsonObject> configure) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        Path camelDir = CommandLineHelper.getCamelDir();
        Path outputFile = camelDir.resolve(pid + "-output-" + requestId + ".json");
        PathUtils.deleteFile(outputFile);

        JsonObject root = new JsonObject();
        root.put("action", action);
        if (configure != null) {
            configure.accept(root);
        }

        Path actionFile = camelDir.resolve(pid + "-action-" + requestId + ".json");
        PathUtils.writeTextSafely(root.toJson(), actionFile);

        try {
            StopWatch watch = new StopWatch();
            while (watch.taken() < ACTION_TIMEOUT_MS) {
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                    if (Files.exists(outputFile) && outputFile.toFile().length() > 0) {
                        String text = Files.readString(outputFile);
                        return (JsonObject) Jsoner.deserialize(text);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // retry
                }
            }
            throw new ToolCallException("Timeout waiting for response from PID " + pid + " for action: " + action, null);
        } finally {
            PathUtils.deleteFile(outputFile);
            PathUtils.deleteFile(actionFile);
        }
    }

    private JsonObject readStatusFromFile(Path path) {
        try {
            if (Files.exists(path) && path.toFile().length() > 0) {
                String text = Files.readString(path);
                return (JsonObject) Jsoner.deserialize(text);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
