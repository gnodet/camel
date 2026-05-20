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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema;
import org.apache.camel.CamelContext;
import org.apache.camel.console.DevConsole;
import org.apache.camel.console.DevConsoleRegistry;
import org.apache.camel.util.json.JsonObject;

/**
 * Bridges MCP tool calls to Camel DevConsole invocations.
 *
 * @since 4.21
 */
public class DevConsoleToolHandler {

    private final CamelContext camelContext;

    public DevConsoleToolHandler(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    public McpSchema.CallToolResult handle(String consoleId, McpSchema.CallToolRequest request) {
        DevConsoleRegistry registry = camelContext.getCamelContextExtension()
                .getContextPlugin(DevConsoleRegistry.class);
        if (registry == null) {
            return errorResult("DevConsoleRegistry not available");
        }

        DevConsole console = registry.resolveById(consoleId);
        if (console == null) {
            return errorResult("DevConsole not found: " + consoleId);
        }

        Map<String, Object> options = new HashMap<>();
        if (request.arguments() != null) {
            options.putAll(request.arguments());
        }

        try {
            Object result = console.call(DevConsole.MediaType.JSON, options);
            String jsonText;
            if (result instanceof JsonObject jo) {
                jsonText = jo.toJson();
            } else if (result instanceof Map<?, ?> map) {
                JsonObject jo = new JsonObject();
                map.forEach((k, v) -> jo.put(String.valueOf(k), v));
                jsonText = jo.toJson();
            } else if (result != null) {
                jsonText = result.toString();
            } else {
                jsonText = "{}";
            }
            return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(jsonText)), false, null, null);
        } catch (Exception e) {
            return errorResult("Error calling DevConsole " + consoleId + ": " + e.getMessage());
        }
    }

    private McpSchema.CallToolResult errorResult(String message) {
        return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(message)), true, null, null);
    }

}
