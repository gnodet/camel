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
import java.util.Map;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpPromptRegistryTest {

    @Test
    void testBuildPromptsReturnsTwoPrompts() {
        McpPromptRegistry registry = new McpPromptRegistry();
        List<McpServerFeatures.SyncPromptSpecification> prompts = registry.buildPrompts();

        assertEquals(2, prompts.size());
    }

    @Test
    void testDebugRoutePrompt() {
        McpPromptRegistry registry = new McpPromptRegistry();
        List<McpServerFeatures.SyncPromptSpecification> prompts = registry.buildPrompts();

        McpServerFeatures.SyncPromptSpecification debugPrompt = prompts.get(0);
        assertEquals("camel_debug_route", debugPrompt.prompt().name());
        assertNotNull(debugPrompt.prompt().description());

        List<McpSchema.PromptArgument> args = debugPrompt.prompt().arguments();
        assertEquals(1, args.size());
        assertEquals("routeId", args.get(0).name());
        assertFalse(args.get(0).required());
    }

    @Test
    void testDebugRoutePromptHandlerWithoutRouteId() {
        McpPromptRegistry registry = new McpPromptRegistry();
        McpServerFeatures.SyncPromptSpecification debugPrompt = registry.buildPrompts().get(0);

        McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest("camel_debug_route", null);
        McpSchema.GetPromptResult result = debugPrompt.promptHandler().apply(null, request);

        assertNotNull(result);
        assertEquals(1, result.messages().size());
        McpSchema.PromptMessage msg = result.messages().get(0);
        assertEquals(McpSchema.Role.USER, msg.role());
        assertTrue(msg.content() instanceof McpSchema.TextContent);
        String text = ((McpSchema.TextContent) msg.content()).text();
        assertTrue(text.contains("Step 1: Health Check"));
        assertTrue(text.contains("camel_health"));
        assertFalse(text.contains("Filter by routeId"));
    }

    @Test
    void testDebugRoutePromptHandlerWithRouteId() {
        McpPromptRegistry registry = new McpPromptRegistry();
        McpServerFeatures.SyncPromptSpecification debugPrompt = registry.buildPrompts().get(0);

        McpSchema.GetPromptRequest request
                = new McpSchema.GetPromptRequest("camel_debug_route", Map.of("routeId", "myRoute"));
        McpSchema.GetPromptResult result = debugPrompt.promptHandler().apply(null, request);

        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("Filter by routeId=\"myRoute\""));
    }

    @Test
    void testPerformanceAnalysisPrompt() {
        McpPromptRegistry registry = new McpPromptRegistry();
        McpServerFeatures.SyncPromptSpecification perfPrompt = registry.buildPrompts().get(1);

        assertEquals("camel_performance_analysis", perfPrompt.prompt().name());
        assertTrue(perfPrompt.prompt().arguments().isEmpty());

        McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest("camel_performance_analysis", null);
        McpSchema.GetPromptResult result = perfPrompt.promptHandler().apply(null, request);

        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("Step 1: Context Overview"));
        assertTrue(text.contains("camel_top"));
        assertTrue(text.contains("camel_inflight"));
    }
}
