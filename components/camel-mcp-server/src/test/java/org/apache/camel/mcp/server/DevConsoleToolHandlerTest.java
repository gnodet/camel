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

import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema;
import org.apache.camel.CamelContext;
import org.apache.camel.console.DevConsoleRegistry;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevConsoleToolHandlerTest {

    private CamelContext context;

    @BeforeEach
    void setUp() throws Exception {
        context = new DefaultCamelContext();
        context.setDevConsole(true);
        context.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (context != null) {
            context.stop();
        }
    }

    @Test
    void testHandleContextConsole() {
        DevConsoleToolHandler handler = new DevConsoleToolHandler(context);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("camel_context", Map.of());
        McpSchema.CallToolResult result = handler.handle("context", request);

        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(1, result.content().size());
        assertTrue(result.content().get(0) instanceof McpSchema.TextContent);
        String text = ((McpSchema.TextContent) result.content().get(0)).text();
        assertFalse(text.isBlank());
    }

    @Test
    void testHandleUnknownConsoleReturnsError() {
        DevConsoleToolHandler handler = new DevConsoleToolHandler(context);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("unknown", Map.of());
        McpSchema.CallToolResult result = handler.handle("does-not-exist", request);

        assertNotNull(result);
        assertTrue(result.isError());
        String text = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(text.contains("DevConsole not found: does-not-exist"));
    }

    @Test
    void testHandleHealthConsole() {
        DevConsoleToolHandler handler = new DevConsoleToolHandler(context);

        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("camel_health", Map.of());
        McpSchema.CallToolResult result = handler.handle("health", request);

        assertNotNull(result);
        assertFalse(result.isError());
    }

    @Test
    void testDevConsoleRegistryAvailable() {
        DevConsoleRegistry registry = context.getCamelContextExtension()
                .getContextPlugin(DevConsoleRegistry.class);
        assertNotNull(registry, "DevConsoleRegistry should be available when devConsole is enabled");
    }
}
