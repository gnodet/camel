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
import java.util.List;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Builds MCP prompt specifications for guided Camel debugging and analysis workflows.
 */
public class McpPromptRegistry {

    public List<McpServerFeatures.SyncPromptSpecification> buildPrompts() {
        List<McpServerFeatures.SyncPromptSpecification> prompts = new ArrayList<>();

        prompts.add(new McpServerFeatures.SyncPromptSpecification(
                new McpSchema.Prompt(
                        "camel_debug_route",
                        "Guided workflow to debug a failing Camel route: discover health issues, identify errors, enable tracing, and diagnose the root cause.",
                        List.of(new McpSchema.PromptArgument("routeId", "Route ID to focus on (optional)", false))),
                (exchange, request) -> {
                    String routeId = request.arguments() != null ? (String) request.arguments().get("routeId") : null;
                    String routeFilter = routeId != null ? " Filter by routeId=\"" + routeId + "\"." : "";

                    String text = "You are debugging a Camel application. Follow this structured workflow:\n\n"
                                  + "## Step 1: Health Check\n"
                                  + "Call `camel_health` to check overall application health. Report any failing checks.\n\n"
                                  + "## Step 2: Route Status\n"
                                  + "Call `camel_routes`" + routeFilter + " to see route statistics. Look for:\n"
                                  + "- Routes with high `exchangesFailed` counts\n"
                                  + "- Routes in non-Started state\n"
                                  + "- Routes with unusually high mean processing times\n\n"
                                  + "## Step 3: Error Details\n"
                                  + "Call `camel_errors` with `stackTrace=true`" + routeFilter
                                  + " to get recent errors. Classify each error:\n"
                                  + "- Missing component/endpoint → dependency issue\n"
                                  + "- Type conversion → data format mismatch\n"
                                  + "- Connection refused → external service down\n"
                                  + "- Expression evaluation → DSL/expression error\n\n"
                                  + "## Step 4: Enable Tracing\n"
                                  + "Call `camel_trace` with `enabled=true` to start tracing messages.\n"
                                  + "Then call `camel_trace` with `dump=true` to retrieve traced messages.\n"
                                  + "Analyze the message flow to identify where processing fails or diverges.\n\n"
                                  + "## Step 5: Performance Hotspots\n"
                                  + "Call `camel_top` with `processors=true` to find the slowest processors.\n"
                                  + "Cross-reference with the error data from Step 3.\n\n"
                                  + "## Step 6: Root Cause Analysis\n"
                                  + "Synthesize findings from all steps into a diagnosis:\n"
                                  + "- What is failing and why\n"
                                  + "- Which component/processor is the root cause\n"
                                  + "- Suggested fix with specific actions\n\n"
                                  + "Present a clear summary with the root cause and recommended fix.";

                    return new McpSchema.GetPromptResult(
                            "Debug a failing Camel route",
                            List.of(new McpSchema.PromptMessage(
                                    McpSchema.Role.USER,
                                    new McpSchema.TextContent(text))));
                }));

        prompts.add(new McpServerFeatures.SyncPromptSpecification(
                new McpSchema.Prompt(
                        "camel_performance_analysis",
                        "Guided workflow to analyze performance of a running Camel application: identify slow routes, bottleneck processors, queue depths, and stuck exchanges.",
                        List.of()),
                (exchange, request) -> {
                    String text
                            = "You are analyzing the performance of a Camel application. Follow this workflow:\n\n"
                              + "## Step 1: Context Overview\n"
                              + "Call `camel_context` to get uptime, version, and memory stats.\n\n"
                              + "## Step 2: Route Statistics\n"
                              + "Call `camel_routes` to get all route statistics. Sort by:\n"
                              + "- Highest `exchangesTotal` (busiest routes)\n"
                              + "- Highest `meanProcessingTime` (slowest routes)\n"
                              + "- Highest `exchangesFailed` / `exchangesTotal` ratio (most error-prone)\n\n"
                              + "## Step 3: Processor Hotspots\n"
                              + "Call `camel_top` with `processors=true` to find the slowest individual processors.\n"
                              + "Identify which route each slow processor belongs to.\n\n"
                              + "## Step 4: Queue Depths\n"
                              + "Call `camel_browse` to check queue depths on seda/stub endpoints.\n"
                              + "High queue depths indicate consumers can't keep up with producers.\n\n"
                              + "## Step 5: Stuck Exchanges\n"
                              + "Call `camel_inflight` to check for exchanges that have been processing for a long time.\n"
                              + "Call `camel_blocked` to check for exchanges waiting on replies.\n\n"
                              + "## Step 6: Summary\n"
                              + "Present a performance report:\n"
                              + "- Top 3 bottleneck processors with their routes\n"
                              + "- Queue depth warnings\n"
                              + "- Any stuck or long-running exchanges\n"
                              + "- Specific recommendations for improvement "
                              + "(e.g., increase consumers, add threading, optimize processor X)";

                    return new McpSchema.GetPromptResult(
                            "Analyze Camel application performance",
                            List.of(new McpSchema.PromptMessage(
                                    McpSchema.Role.USER,
                                    new McpSchema.TextContent(text))));
                }));

        return prompts;
    }

}
