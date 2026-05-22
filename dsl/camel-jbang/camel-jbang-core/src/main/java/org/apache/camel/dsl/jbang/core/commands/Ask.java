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
package org.apache.camel.dsl.jbang.core.commands;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.camel.dsl.jbang.core.common.CommandLineHelper;
import org.apache.camel.dsl.jbang.core.common.PathUtils;
import org.apache.camel.dsl.jbang.core.common.ProcessHelper;
import org.apache.camel.support.PatternHelper;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.StopWatch;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Ask a question about a running Camel application using AI with tool calling. The LLM can inspect the live runtime
 * (routes, health, traces, etc.) to provide informed answers.
 */
@Command(name = "ask",
         description = "Ask a question about a running Camel application using AI",
         sortOptions = false, showDefaultValues = true,
         footer = {
                 "%nExamples:",
                 "  camel ask \"what routes are running?\"",
                 "  camel ask \"why is my route failing?\" --name=myApp",
                 "  camel ask \"show me the route structure\" --api-type=anthropic",
                 "  camel ask \"are there any blocked exchanges?\" --model=gpt-4",
                 "  camel ask                                   (interactive chat)" })
public class Ask extends CamelCommand {

    private static final String DEFAULT_MODEL = "llama3.2";
    private static final long ACTION_TIMEOUT_MS = 10_000;
    private static final long POLL_INTERVAL_MS = 100;

    @Parameters(description = "Question to ask (omit for interactive chat mode)", arity = "0..*")
    List<String> question;

    @Option(names = { "--url" },
            description = "LLM API endpoint URL. Auto-detected if not specified.")
    String url;

    @Option(names = { "--api-type" },
            description = "API type: 'ollama', 'openai', or 'anthropic'")
    LlmClient.ApiType apiType;

    @Option(names = { "--api-key" },
            description = "API key. Also reads ANTHROPIC_API_KEY, OPENAI_API_KEY, or LLM_API_KEY env vars")
    String apiKey;

    @Option(names = { "--model" },
            description = "Model to use",
            defaultValue = DEFAULT_MODEL)
    String model = DEFAULT_MODEL;

    @Option(names = { "--timeout" },
            description = "Timeout in seconds for LLM response",
            defaultValue = "120")
    int timeout = 120;

    @Option(names = { "--name" },
            description = "Name or PID of the Camel process. Auto-detected when exactly one process is running")
    String nameOrPid;

    @Option(names = { "--max-iterations" },
            description = "Maximum number of tool-calling rounds",
            defaultValue = "10")
    int maxIterations = 10;

    @Option(names = { "--show-tools" },
            description = "Show tool calls and results as they happen")
    boolean showTools;

    private long targetPid;

    public Ask(CamelJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        LlmClient client = LlmClient.create()
                .withUrl(url)
                .withApiType(apiType)
                .withApiKey(apiKey)
                .withModel(model)
                .withTimeout(timeout)
                .withTemperature(0.3)
                .withStream(false)
                .withMaxTokens(4096)
                .withPrinter(printer());

        if (!client.detectEndpoint()) {
            printer().printErr("LLM service is not reachable.");
            printer().printErr("Options: --url=<endpoint>, --api-type=anthropic, or start Ollama with: camel infra run ollama");
            return 1;
        }

        ProcessInfo process = findProcess(nameOrPid);
        if (process == null) {
            return 1;
        }
        targetPid = process.pid;

        String systemPrompt = buildSystemPrompt(process);
        List<LlmClient.ToolDef> tools = buildToolDefinitions();

        if (question == null || question.isEmpty()) {
            return runInteractiveChat(client, process, systemPrompt, tools);
        }

        String userQuestion = String.join(" ", question);
        printer().println("Using " + client.model + " (" + client.apiType + ") to answer your question...");
        printer().println("Target: " + process.name + " (PID " + process.pid + ")");
        printer().println();

        List<LlmClient.Message> messages = new ArrayList<>();
        return runAgentLoop(client, systemPrompt, tools, messages, userQuestion);
    }

    private int runInteractiveChat(
            LlmClient client, ProcessInfo process,
            String systemPrompt, List<LlmClient.ToolDef> tools)
            throws Exception {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            printer().println("Camel AI Assistant (" + client.model + ", " + client.apiType + ")");
            printer().println("Target: " + process.name + " (PID " + process.pid + ")");
            printer().println("Type your question, or 'exit' to quit.");
            printer().println();

            List<LlmClient.Message> messages = new ArrayList<>();

            while (true) {
                String line;
                try {
                    line = reader.readLine("ask> ");
                } catch (UserInterruptException | EndOfFileException e) {
                    break;
                }
                if (line == null || line.isBlank() || "exit".equalsIgnoreCase(line.strip())) {
                    break;
                }

                int result = runAgentLoop(client, systemPrompt, tools, messages, line.strip());
                if (result != 0) {
                    printer().printErr("(error processing question, continuing...)");
                }
                printer().println();
            }
        }
        return 0;
    }

    private int runAgentLoop(
            LlmClient client, String systemPrompt,
            List<LlmClient.ToolDef> tools, List<LlmClient.Message> messages,
            String userQuestion) {
        messages.add(LlmClient.Message.user(userQuestion));

        for (int i = 0; i < maxIterations; i++) {
            LlmClient.ChatResponse response = client.chatWithTools(systemPrompt, messages, tools);
            if (response == null) {
                printer().printErr("Failed to get response from LLM");
                return 1;
            }

            if (response.toolCalls() != null && !response.toolCalls().isEmpty()) {
                messages.add(LlmClient.Message.assistantWithToolCalls(response.text(), response.toolCalls()));

                List<LlmClient.ToolResult> results = new ArrayList<>();
                for (LlmClient.ToolCall toolCall : response.toolCalls()) {
                    if (showTools) {
                        printer().println("[tool] " + toolCall.name() + "(" + toolCall.arguments().toJson() + ")");
                    }
                    String result = executeTool(toolCall.name(), toolCall.arguments());
                    if (showTools) {
                        printer().println("[result] " + truncate(result, 200));
                    }
                    results.add(new LlmClient.ToolResult(toolCall.id(), result));
                }
                messages.add(LlmClient.Message.toolResults(results));
            } else {
                if (response.text() != null) {
                    printer().println(response.text());
                }
                messages.add(LlmClient.Message.assistantWithToolCalls(response.text(), List.of()));
                return 0;
            }
        }

        printer().printErr("Reached maximum iterations (" + maxIterations + ") without a final answer.");
        return 1;
    }

    // ---- System prompt ----

    private String buildSystemPrompt(ProcessInfo process) {
        return "You are an Apache Camel runtime assistant. "
               + "You help users understand and troubleshoot their running Camel applications.\n\n"
               + "You have tools to inspect the live runtime of a Camel application. "
               + "Use them to gather information before answering the user's question.\n\n"
               + "Current target: " + process.name + " (PID " + process.pid + ")\n\n"
               + "Guidelines:\n"
               + "- Start by gathering relevant information using the available tools\n"
               + "- Be concise and actionable in your answers\n"
               + "- If something looks wrong, explain what it means and suggest fixes\n"
               + "- Format output as plain text for terminal display, do not use markdown\n";
    }

    // ---- Tool definitions ----

    private List<LlmClient.ToolDef> buildToolDefinitions() {
        List<LlmClient.ToolDef> tools = new ArrayList<>();

        // Status-file tools (no parameters needed)
        tools.add(new LlmClient.ToolDef(
                "get_context",
                "Get Camel context info: name, version, state, uptime, route count, exchange statistics.",
                emptyParams()));
        tools.add(new LlmClient.ToolDef(
                "get_routes",
                "List all routes with their state, uptime, messages processed, last error, and throughput.",
                emptyParams()));
        tools.add(new LlmClient.ToolDef(
                "get_health",
                "Get health check status for the Camel application.",
                emptyParams()));
        tools.add(new LlmClient.ToolDef(
                "get_endpoints",
                "List all endpoints registered in the Camel context with URIs and usage stats.",
                emptyParams()));
        tools.add(new LlmClient.ToolDef(
                "get_inflight",
                "Show currently in-flight exchanges (messages being processed).",
                emptyParams()));
        tools.add(new LlmClient.ToolDef(
                "get_blocked",
                "Show blocked exchanges that are stuck or waiting.",
                emptyParams()));
        tools.add(new LlmClient.ToolDef(
                "get_consumers",
                "Show consumer statistics (polling and event-driven consumers).",
                emptyParams()));
        tools.add(new LlmClient.ToolDef(
                "get_properties",
                "Show configuration properties of the running Camel application.",
                emptyParams()));

        // IPC action tools (with parameters)
        tools.add(new LlmClient.ToolDef(
                "get_route_source",
                "Get the source code of routes. Use filter to limit by filename (supports wildcards).",
                objectParams(Map.of(
                        "filter", stringProp("Filter source files by name (supports wildcards). Use * for all.")))));
        tools.add(new LlmClient.ToolDef(
                "get_route_dump",
                "Dump route definitions in XML or YAML format.",
                objectParams(Map.of(
                        "routeId", stringProp("Route ID to dump (use * for all routes)"),
                        "format", stringProp("Output format: xml or yaml (default: yaml)")))));
        tools.add(new LlmClient.ToolDef(
                "get_route_structure",
                "Show the route structure as a tree of processors.",
                objectParams(Map.of(
                        "routeId", stringProp("Route ID to inspect (use * for all routes)")))));
        tools.add(new LlmClient.ToolDef(
                "get_top_processors",
                "Show top processor statistics: which processors are slowest and most active.",
                emptyParams()));
        tools.add(new LlmClient.ToolDef(
                "trace_control",
                "Enable, disable, or dump message tracing.",
                objectParams(Map.of(
                        "action", stringProp("Action: enable, disable, or dump")))));

        return tools;
    }

    // ---- Tool execution ----

    private String executeTool(String name, JsonObject args) {
        try {
            return switch (name) {
                case "get_context" -> readSection("context");
                case "get_routes" -> readSection("routes");
                case "get_health" -> readSection("healthChecks");
                case "get_endpoints" -> readSection("endpoints");
                case "get_inflight" -> readSection("inflight");
                case "get_blocked" -> readSection("blocked");
                case "get_consumers" -> readSection("consumers");
                case "get_properties" -> readSection("properties");
                case "get_route_source" -> executeRouteSource(args);
                case "get_route_dump" -> executeRouteDump(args);
                case "get_route_structure" -> executeRouteStructure(args);
                case "get_top_processors" -> executeAction("top-processors", null);
                case "trace_control" -> executeTraceControl(args);
                default -> "Unknown tool: " + name;
            };
        } catch (Exception e) {
            return "Error executing " + name + ": " + e.getMessage();
        }
    }

    private String readSection(String section) {
        JsonObject status = readStatus(targetPid);
        if (status == null) {
            return "No status available for PID " + targetPid;
        }
        Object value = status.get(section);
        if (value instanceof JsonObject jo) {
            return jo.toJson();
        }
        if (value != null) {
            JsonObject wrapper = new JsonObject();
            wrapper.put(section, value);
            return wrapper.toJson();
        }
        return "{}";
    }

    private String executeRouteSource(JsonObject args) {
        String filter = args.getString("filter");
        return executeAction("source", root -> root.put("filter", filter != null ? filter : "*"));
    }

    private String executeRouteDump(JsonObject args) {
        String routeId = args.getString("routeId");
        String format = args.getString("format");
        return executeAction("route-dump", root -> {
            root.put("id", routeId != null ? routeId : "*");
            root.put("format", format != null ? format : "yaml");
        });
    }

    private String executeRouteStructure(JsonObject args) {
        String routeId = args.getString("routeId");
        return executeAction("route-structure", root -> root.put("id", routeId != null ? routeId : "*"));
    }

    private String executeTraceControl(JsonObject args) {
        String action = args.getString("action");
        if (action == null) {
            return "Error: action is required (enable, disable, dump)";
        }
        return executeAction("trace", root -> {
            switch (action.toLowerCase()) {
                case "enable" -> root.put("enabled", "true");
                case "disable" -> root.put("enabled", "false");
                case "dump" -> root.put("dump", "true");
                default -> root.put("enabled", action);
            }
        });
    }

    // ---- IPC implementation ----

    record ProcessInfo(long pid, String name, String contextName) {
    }

    private List<ProcessInfo> discoverProcesses() {
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
                // skip
            }
        }
        return result;
    }

    private ProcessInfo findProcess(String nameOrPid) {
        List<ProcessInfo> processes = discoverProcesses();
        if (processes.isEmpty()) {
            printer().printErr("No running Camel processes found.");
            printer().printErr("Start a Camel application first: camel run myRoute.yaml");
            return null;
        }

        if (nameOrPid != null && !nameOrPid.isBlank()) {
            if (nameOrPid.matches("\\d+")) {
                long pid = Long.parseLong(nameOrPid);
                return processes.stream()
                        .filter(p -> p.pid == pid)
                        .findFirst()
                        .orElseGet(() -> {
                            printer().printErr("No Camel process found with PID: " + nameOrPid);
                            return null;
                        });
            }
            String pattern = nameOrPid.endsWith("*") ? nameOrPid : nameOrPid + "*";
            List<ProcessInfo> matched = processes.stream()
                    .filter(p -> (p.name != null && PatternHelper.matchPattern(FileUtil.onlyName(p.name), pattern))
                            || (p.contextName != null && PatternHelper.matchPattern(p.contextName, pattern)))
                    .toList();
            if (matched.isEmpty()) {
                printer().printErr("No Camel process found matching: " + nameOrPid);
                return null;
            }
            if (matched.size() > 1) {
                printer().printErr("Multiple Camel processes match '" + nameOrPid + "':");
                matched.forEach(p -> printer().printErr("  " + p.name + " (PID " + p.pid + ")"));
                printer().printErr("Specify a more specific name or PID with --name");
                return null;
            }
            return matched.get(0);
        }

        if (processes.size() > 1) {
            printer().printErr("Multiple Camel processes running:");
            processes.forEach(p -> printer().printErr("  " + p.name + " (PID " + p.pid + ")"));
            printer().printErr("Specify which one with --name=<name-or-pid>");
            return null;
        }
        return processes.get(0);
    }

    private JsonObject readStatus(long pid) {
        Path statusFile = CommandLineHelper.getCamelDir().resolve(pid + "-status.json");
        return readStatusFromFile(statusFile);
    }

    private String executeAction(String action, Consumer<JsonObject> configure) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        Path camelDir = CommandLineHelper.getCamelDir();
        Path outputFile = camelDir.resolve(targetPid + "-output-" + requestId + ".json");
        PathUtils.deleteFile(outputFile);

        JsonObject root = new JsonObject();
        root.put("action", action);
        if (configure != null) {
            configure.accept(root);
        }

        Path actionFile = camelDir.resolve(targetPid + "-action-" + requestId + ".json");
        PathUtils.writeTextSafely(root.toJson(), actionFile);

        try {
            StopWatch watch = new StopWatch();
            while (watch.taken() < ACTION_TIMEOUT_MS) {
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                    if (Files.exists(outputFile) && outputFile.toFile().length() > 0) {
                        return Files.readString(outputFile);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // retry
                }
            }
            return "Timeout waiting for response from PID " + targetPid + " for action: " + action;
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

    // ---- JSON schema helpers for tool parameters ----

    private static JsonObject emptyParams() {
        JsonObject schema = new JsonObject();
        schema.put("type", "object");
        schema.put("properties", new JsonObject());
        return schema;
    }

    private static JsonObject objectParams(Map<String, JsonObject> properties) {
        JsonObject props = new JsonObject();
        // use LinkedHashMap ordering
        Map<String, JsonObject> ordered = new LinkedHashMap<>(properties);
        for (Map.Entry<String, JsonObject> entry : ordered.entrySet()) {
            props.put(entry.getKey(), entry.getValue());
        }
        JsonObject schema = new JsonObject();
        schema.put("type", "object");
        schema.put("properties", props);
        return schema;
    }

    private static JsonObject stringProp(String description) {
        JsonObject prop = new JsonObject();
        prop.put("type", "string");
        prop.put("description", description);
        return prop;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return "null";
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
