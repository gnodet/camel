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

                    String text = """
                            You are debugging a Camel application. Follow this structured workflow:

                            ## Step 1: Health Check
                            Call `camel_health` to check overall application health. Report any failing checks.

                            ## Step 2: Route Status
                            Call `camel_routes`%s to see route statistics. Look for:
                            - Routes with high `exchangesFailed` counts
                            - Routes in non-Started state
                            - Routes with unusually high mean processing times

                            ## Step 3: Error Details
                            Call `camel_errors` with `stackTrace=true`%s to get recent errors. Classify each error:
                            - Missing component/endpoint → dependency issue
                            - Type conversion → data format mismatch
                            - Connection refused → external service down
                            - Expression evaluation → DSL/expression error

                            ## Step 4: Enable Tracing
                            Call `camel_trace` with `enabled=true` to start tracing messages.
                            Then call `camel_trace` with `dump=true` to retrieve traced messages.
                            Analyze the message flow to identify where processing fails or diverges.

                            ## Step 5: Performance Hotspots
                            Call `camel_top` with `processors=true` to find the slowest processors.
                            Cross-reference with the error data from Step 3.

                            ## Step 6: Root Cause Analysis
                            Synthesize findings from all steps into a diagnosis:
                            - What is failing and why
                            - Which component/processor is the root cause
                            - Suggested fix with specific actions

                            Present a clear summary with the root cause and recommended fix.\
                            """.formatted(routeFilter, routeFilter);

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
                    String text = """
                            You are analyzing the performance of a Camel application. Follow this workflow:

                            ## Step 1: Context Overview
                            Call `camel_context` to get uptime, version, and memory stats.

                            ## Step 2: Route Statistics
                            Call `camel_routes` to get all route statistics. Sort by:
                            - Highest `exchangesTotal` (busiest routes)
                            - Highest `meanProcessingTime` (slowest routes)
                            - Highest `exchangesFailed` / `exchangesTotal` ratio (most error-prone)

                            ## Step 3: Processor Hotspots
                            Call `camel_top` with `processors=true` to find the slowest individual processors.
                            Identify which route each slow processor belongs to.

                            ## Step 4: Queue Depths
                            Call `camel_browse` to check queue depths on seda/stub endpoints.
                            High queue depths indicate consumers can't keep up with producers.

                            ## Step 5: Stuck Exchanges
                            Call `camel_inflight` to check for exchanges that have been processing for a long time.
                            Call `camel_blocked` to check for exchanges waiting on replies.

                            ## Step 6: Summary
                            Present a performance report:
                            - Top 3 bottleneck processors with their routes
                            - Queue depth warnings
                            - Any stuck or long-running exchanges
                            - Specific recommendations for improvement \
                            (e.g., increase consumers, add threading, optimize processor X)\
                            """;

                    return new McpSchema.GetPromptResult(
                            "Analyze Camel application performance",
                            List.of(new McpSchema.PromptMessage(
                                    McpSchema.Role.USER,
                                    new McpSchema.TextContent(text))));
                }));

        return prompts;
    }

}
