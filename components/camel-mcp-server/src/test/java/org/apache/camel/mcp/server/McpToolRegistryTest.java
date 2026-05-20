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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.modelcontextprotocol.server.McpServerFeatures;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolRegistryTest {

    private CamelContext context;

    @BeforeEach
    void setUp() throws Exception {
        context = new DefaultCamelContext();
        context.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (context != null) {
            context.stop();
        }
    }

    @Test
    void testBuildToolsReturnsAllTools() {
        McpToolRegistry registry = new McpToolRegistry(context);
        List<McpServerFeatures.SyncToolSpecification> tools = registry.buildTools(null, null);

        assertEquals(24, tools.size());

        Set<String> names = tools.stream()
                .map(t -> t.tool().name())
                .collect(Collectors.toSet());
        assertTrue(names.contains("camel_context"));
        assertTrue(names.contains("camel_routes"));
        assertTrue(names.contains("camel_health"));
        assertTrue(names.contains("camel_errors"));
        assertTrue(names.contains("camel_top"));
        assertTrue(names.contains("camel_send"));
        assertTrue(names.contains("camel_trace"));
        assertTrue(names.contains("camel_debug"));
        assertTrue(names.contains("camel_browse"));
        assertTrue(names.contains("camel_eval"));
    }

    @Test
    void testToolsHaveDescriptions() {
        McpToolRegistry registry = new McpToolRegistry(context);
        List<McpServerFeatures.SyncToolSpecification> tools = registry.buildTools(null, null);

        for (McpServerFeatures.SyncToolSpecification tool : tools) {
            assertNotNull(tool.tool().description(), "Tool " + tool.tool().name() + " should have a description");
            assertFalse(tool.tool().description().isBlank(), "Tool " + tool.tool().name() + " description should not be blank");
        }
    }

    @Test
    void testToolsHaveInputSchema() {
        McpToolRegistry registry = new McpToolRegistry(context);
        List<McpServerFeatures.SyncToolSpecification> tools = registry.buildTools(null, null);

        for (McpServerFeatures.SyncToolSpecification tool : tools) {
            assertNotNull(tool.tool().inputSchema(), "Tool " + tool.tool().name() + " should have input schema");
            assertEquals("object", tool.tool().inputSchema().type());
        }
    }

    @Test
    void testIncludeFilter() {
        McpToolRegistry registry = new McpToolRegistry(context);
        List<McpServerFeatures.SyncToolSpecification> tools = registry.buildTools("camel_context,camel_routes", null);

        assertEquals(2, tools.size());
        Set<String> names = tools.stream()
                .map(t -> t.tool().name())
                .collect(Collectors.toSet());
        assertTrue(names.contains("camel_context"));
        assertTrue(names.contains("camel_routes"));
    }

    @Test
    void testExcludeFilter() {
        McpToolRegistry registry = new McpToolRegistry(context);
        List<McpServerFeatures.SyncToolSpecification> tools = registry.buildTools(null, "camel_send,camel_debug");

        Set<String> names = tools.stream()
                .map(t -> t.tool().name())
                .collect(Collectors.toSet());
        assertFalse(names.contains("camel_send"));
        assertFalse(names.contains("camel_debug"));
        assertEquals(22, tools.size());
    }

    @Test
    void testRequiredFieldsOnRouteControl() {
        McpToolRegistry registry = new McpToolRegistry(context);
        List<McpServerFeatures.SyncToolSpecification> tools = registry.buildTools("camel_route_control", null);

        assertEquals(1, tools.size());
        List<String> required = tools.get(0).tool().inputSchema().required();
        assertNotNull(required);
        assertTrue(required.contains("id"));
        assertTrue(required.contains("command"));
    }

    @Test
    void testToolsHaveCallHandlers() {
        McpToolRegistry registry = new McpToolRegistry(context);
        List<McpServerFeatures.SyncToolSpecification> tools = registry.buildTools(null, null);

        for (McpServerFeatures.SyncToolSpecification tool : tools) {
            assertNotNull(tool.callHandler(), "Tool " + tool.tool().name() + " should have a call handler");
        }
    }
}
