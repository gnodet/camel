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
package org.apache.camel.mcp.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.camel.CamelContext;

/**
 * Builds MCP tool specifications that map to Camel DevConsole endpoints.
 *
 * @since 4.21
 */
public class McpToolRegistry {

    private final CamelContext camelContext;

    public McpToolRegistry(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    public List<McpServerFeatures.SyncToolSpecification> buildTools(String includeTools, String excludeTools) {
        List<McpServerFeatures.SyncToolSpecification> tools = new ArrayList<>();
        DevConsoleToolHandler handler = new DevConsoleToolHandler(camelContext);

        // Context & Routes
        addTool(tools, handler, "camel_context", "context",
                "Get Camel context information: name, version, state, uptime, memory, and thread stats.",
                emptySchema());

        addTool(tools, handler, "camel_routes", "route",
                "List routes with state and statistics (total/failed exchanges, mean/max processing times, throughput). Use filter to match route IDs by pattern.",
                schema(
                        prop("filter", "string", "Filter route IDs by wildcard pattern (e.g. 'order*')"),
                        prop("limit", "integer", "Maximum number of routes to return")));

        addTool(tools, handler, "camel_route_control", "route",
                "Start, stop, suspend, or resume a route by its route ID.",
                schema(
                        prop("id", "string", "Route ID"),
                        propEnum("command", "Action to perform", "start", "stop", "suspend", "resume")),
                List.of("id", "command"));

        addTool(tools, handler, "camel_route_source", "source",
                "Get the source code of a route (YAML, XML, or Java DSL).",
                schema(prop("id", "string", "Route ID (omit for all routes)")));

        addTool(tools, handler, "camel_route_structure", "route-structure",
                "Get the structural tree of a route showing all EIPs and processors.",
                schema(prop("id", "string", "Route ID (omit for all routes)")));

        addTool(tools, handler, "camel_route_dump", "route-dump",
                "Dump the full route model definition as XML or YAML.",
                schema(
                        prop("id", "string", "Route ID (omit for all routes)"),
                        propEnum("format", "Output format (default: xml)", "xml", "yaml")));

        // Observability
        addTool(tools, handler, "camel_health", "health",
                "Get health check status: overall up/down, readiness, liveness, and individual check results with details.",
                emptySchema());

        addTool(tools, handler, "camel_errors", "errors",
                "Get recent exchange errors including exception type, message, route ID, endpoint URI, and optionally stack traces.",
                schema(
                        prop("routeId", "string", "Filter errors by route ID"),
                        prop("limit", "integer", "Maximum number of errors to return"),
                        prop("stackTrace", "boolean", "Include full stack traces (default: false)")));

        addTool(tools, handler, "camel_inflight", "inflight",
                "Get currently in-flight exchanges with exchange ID, route ID, current node, and elapsed time.",
                emptySchema());

        addToolWithCustomHandler(tools, handler, "camel_top",
                "Get routes or processors sorted by slowest processing time. Useful for identifying bottlenecks.",
                schema(prop("processors", "boolean", "Show individual processors instead of routes")));

        addTool(tools, handler, "camel_endpoints", "endpoint",
                "List all active endpoints in the Camel context with their URIs and usage statistics.",
                emptySchema());

        // Tracing & Debugging
        addTool(tools, handler, "camel_trace", "trace",
                "Control message tracing: enable/disable tracing and retrieve traced messages showing the path each exchange takes through the route.",
                schema(
                        prop("enabled", "boolean", "Set true to enable tracing, false to disable"),
                        prop("dump", "boolean", "Set true to retrieve traced messages")));

        addTool(tools, handler, "camel_debug", "debug",
                "Control the Camel route debugger: enable/disable debugging, add/remove breakpoints at node IDs, resume/step through suspended exchanges, and inspect exchange state at breakpoints.",
                schema(
                        propEnum("command", "Debugger command",
                                "enable", "disable", "add", "remove", "resume", "step", "stepover", "skipover"),
                        prop("breakpoint", "string", "Node ID for add/remove breakpoint commands"),
                        prop("history", "boolean", "Include message history at breakpoints")),
                List.of("command"));

        addTool(tools, handler, "camel_message_history", "message-history",
                "Get the message history showing the path exchanges have taken through processors, with timing information.",
                emptySchema());

        // Interaction
        addTool(tools, handler, "camel_send", "send",
                "Send a test message to an endpoint in the running Camel context. Returns the exchange result and any response body for InOut exchanges.",
                schema(
                        prop("endpoint", "string",
                                "Endpoint URI to send to (e.g., 'direct:myRoute', 'seda:queue')"),
                        prop("body", "string", "Message body to send"),
                        propEnum("exchangePattern", "Exchange pattern (default: InOnly)", "InOnly", "InOut")),
                List.of("endpoint"));

        addTool(tools, handler, "camel_browse", "browse",
                "Browse pending messages on browsable endpoints (seda, mock, stub, browse). Shows queued messages with headers and body.",
                schema(
                        prop("filter", "string", "Endpoint URI pattern to filter (e.g., 'seda:*')"),
                        prop("limit", "integer", "Maximum number of messages to return"),
                        prop("dump", "boolean", "Include full message body content")));

        addTool(tools, handler, "camel_eval", "eval-language",
                "Evaluate a Camel expression (Simple, JsonPath, XPath, etc.) against an optional test message body. Returns the expression result or predicate boolean.",
                schema(
                        prop("language", "string", "Expression language (default: simple)"),
                        prop("template", "string", "Expression to evaluate"),
                        prop("body", "string", "Test message body to evaluate against"),
                        prop("predicate", "boolean", "Evaluate as a predicate (returns true/false)")),
                List.of("template"));

        // Additional consoles
        addTool(tools, handler, "camel_variables", "variables",
                "List Camel variables currently set in the context.",
                emptySchema());

        addTool(tools, handler, "camel_beans", "bean",
                "List beans registered in the Camel registry.",
                schema(prop("filter", "string", "Filter beans by name pattern")));

        addTool(tools, handler, "camel_properties", "properties",
                "Show resolved configuration properties.",
                schema(prop("filter", "string", "Filter properties by key pattern")));

        addTool(tools, handler, "camel_consumers", "consumer",
                "List all consumers (input endpoints) in the Camel context.",
                emptySchema());

        addTool(tools, handler, "camel_services", "service",
                "List services managed by the Camel context.",
                emptySchema());

        addTool(tools, handler, "camel_blocked", "blocked",
                "List exchanges that are blocked waiting for a reply (e.g., InOut calls).",
                emptySchema());

        addTool(tools, handler, "camel_type_converters", "type-converter",
                "Show type converter statistics and available converters.",
                emptySchema());

        return filterTools(tools, includeTools, excludeTools);
    }

    private void addTool(
            List<McpServerFeatures.SyncToolSpecification> tools,
            DevConsoleToolHandler handler,
            String toolName, String consoleId,
            String description, McpSchema.JsonSchema inputSchema) {
        addTool(tools, handler, toolName, consoleId, description, inputSchema, List.of());
    }

    private void addTool(
            List<McpServerFeatures.SyncToolSpecification> tools,
            DevConsoleToolHandler handler,
            String toolName, String consoleId,
            String description, McpSchema.JsonSchema inputSchema,
            List<String> required) {

        if (!required.isEmpty()) {
            inputSchema = new McpSchema.JsonSchema(
                    inputSchema.type(), inputSchema.properties(), required,
                    inputSchema.additionalProperties(), inputSchema.defs(), inputSchema.definitions());
        }

        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name(toolName)
                .description(description)
                .inputSchema(inputSchema)
                .build();
        McpServerFeatures.SyncToolSpecification spec = McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> handler.handle(consoleId, request))
                .build();
        tools.add(spec);
    }

    private void addToolWithCustomHandler(
            List<McpServerFeatures.SyncToolSpecification> tools,
            DevConsoleToolHandler handler,
            String toolName,
            String description, McpSchema.JsonSchema inputSchema) {

        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name(toolName)
                .description(description)
                .inputSchema(inputSchema)
                .build();
        McpServerFeatures.SyncToolSpecification spec = McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    Object processorsArg = request.arguments() != null
                            ? request.arguments().get("processors")
                            : null;
                    boolean processors = processorsArg instanceof Boolean b
                            ? b
                            : Boolean.parseBoolean(String.valueOf(processorsArg));
                    String consoleId = processors ? "top/*" : "top";
                    return handler.handle(consoleId, request);
                })
                .build();
        tools.add(spec);
    }

    private List<McpServerFeatures.SyncToolSpecification> filterTools(
            List<McpServerFeatures.SyncToolSpecification> tools,
            String includeTools, String excludeTools) {

        if (includeTools != null && !includeTools.isBlank()) {
            Set<String> include = Arrays.stream(includeTools.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());
            tools.removeIf(t -> !include.contains(t.tool().name()));
        }
        if (excludeTools != null && !excludeTools.isBlank()) {
            Set<String> exclude = Arrays.stream(excludeTools.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());
            tools.removeIf(t -> exclude.contains(t.tool().name()));
        }
        return tools;
    }

    // Schema helpers

    private static McpSchema.JsonSchema emptySchema() {
        return new McpSchema.JsonSchema("object", Map.of(), null, null, null, null);
    }

    @SafeVarargs
    private static McpSchema.JsonSchema schema(Map.Entry<String, Map<String, Object>>... properties) {
        Map<String, Object> props = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : properties) {
            props.put(entry.getKey(), entry.getValue());
        }
        return new McpSchema.JsonSchema("object", props, null, null, null, null);
    }

    private static Map.Entry<String, Map<String, Object>> prop(String name, String type, String description) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", type);
        p.put("description", description);
        return Map.entry(name, p);
    }

    private static Map.Entry<String, Map<String, Object>> propEnum(String name, String description, String... values) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "string");
        p.put("description", description);
        p.put("enum", List.of(values));
        return Map.entry(name, p);
    }

}
