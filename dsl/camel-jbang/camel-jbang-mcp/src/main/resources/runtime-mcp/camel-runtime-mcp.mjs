#!/usr/bin/env node
//
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const baseUrl = (process.env.CAMEL_DEV_CONSOLE_URL || "http://localhost:8080").replace(/\/+$/, "");

async function callDevConsole(consoleId, params = {}) {
  const url = new URL(`${baseUrl}/q/dev/${consoleId}`);
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null) {
      url.searchParams.set(key, String(value));
    }
  }
  const res = await fetch(url, {
    headers: { Accept: "application/json" },
  });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`Dev console returned ${res.status}: ${body || res.statusText}`);
  }
  return await res.text();
}

function textResult(text) {
  return { content: [{ type: "text", text }] };
}

async function devConsoleTool(consoleId, params) {
  try {
    return textResult(await callDevConsole(consoleId, params));
  } catch (err) {
    return { content: [{ type: "text", text: `Error: ${err.message}` }], isError: true };
  }
}

// ---------------------------------------------------------------------------
// Server
// ---------------------------------------------------------------------------

const server = new McpServer({
  name: "camel-runtime",
  version: "1.0.0",
});

// ---------------------------------------------------------------------------
// Context & Routes
// ---------------------------------------------------------------------------

server.tool(
  "camel_context",
  "Get Camel context information: name, version, state, uptime, memory, and thread stats.",
  {},
  async () => devConsoleTool("context"),
);

server.tool(
  "camel_routes",
  "List routes with state and statistics (total/failed exchanges, mean/max processing times, throughput). Use filter to match route IDs by pattern.",
  {
    filter: z.string().optional().describe("Filter route IDs by wildcard pattern (e.g. 'order*')"),
    limit: z.number().optional().describe("Maximum number of routes to return"),
  },
  async (params) => devConsoleTool("route", params),
);

server.tool(
  "camel_route_control",
  "Start, stop, suspend, or resume a route by its route ID.",
  {
    id: z.string().describe("Route ID"),
    command: z.enum(["start", "stop", "suspend", "resume"]).describe("Action to perform"),
  },
  async (params) => devConsoleTool("route", params),
);

server.tool(
  "camel_route_source",
  "Get the source code of a route (YAML, XML, or Java DSL).",
  {
    id: z.string().optional().describe("Route ID (omit for all routes)"),
  },
  async (params) => devConsoleTool("source", params),
);

server.tool(
  "camel_route_structure",
  "Get the structural tree of a route showing all EIPs and processors.",
  {
    id: z.string().optional().describe("Route ID (omit for all routes)"),
  },
  async (params) => devConsoleTool("route-structure", params),
);

server.tool(
  "camel_route_dump",
  "Dump the full route model definition as XML or YAML.",
  {
    id: z.string().optional().describe("Route ID (omit for all routes)"),
    format: z.enum(["xml", "yaml"]).optional().describe("Output format (default: xml)"),
  },
  async (params) => devConsoleTool("route-dump", params),
);

// ---------------------------------------------------------------------------
// Observability
// ---------------------------------------------------------------------------

server.tool(
  "camel_health",
  "Get health check status: overall up/down, readiness, liveness, and individual check results with details.",
  {},
  async () => devConsoleTool("health"),
);

server.tool(
  "camel_errors",
  "Get recent exchange errors including exception type, message, route ID, endpoint URI, and optionally stack traces.",
  {
    routeId: z.string().optional().describe("Filter errors by route ID"),
    limit: z.number().optional().describe("Maximum number of errors to return"),
    stackTrace: z.boolean().optional().describe("Include full stack traces (default: false)"),
  },
  async (params) => devConsoleTool("errors", params),
);

server.tool(
  "camel_inflight",
  "Get currently in-flight exchanges with exchange ID, route ID, current node, and elapsed time.",
  {},
  async () => devConsoleTool("inflight"),
);

server.tool(
  "camel_top",
  "Get routes or processors sorted by slowest processing time. Useful for identifying bottlenecks.",
  {
    processors: z.boolean().optional().describe("Show individual processors instead of routes"),
  },
  async ({ processors }) => devConsoleTool(processors ? "top/*" : "top"),
);

server.tool(
  "camel_endpoints",
  "List all active endpoints in the Camel context with their URIs and usage statistics.",
  {},
  async () => devConsoleTool("endpoint"),
);

// ---------------------------------------------------------------------------
// Tracing & Debugging
// ---------------------------------------------------------------------------

server.tool(
  "camel_trace",
  "Control message tracing: enable/disable tracing and retrieve traced messages showing the path each exchange takes through the route.",
  {
    enabled: z.boolean().optional().describe("Set true to enable tracing, false to disable"),
    dump: z.boolean().optional().describe("Set true to retrieve traced messages"),
  },
  async (params) => devConsoleTool("trace", params),
);

server.tool(
  "camel_debug",
  "Control the Camel route debugger: enable/disable debugging, add/remove breakpoints at node IDs, resume/step through suspended exchanges, and inspect exchange state at breakpoints.",
  {
    command: z
      .enum(["enable", "disable", "add", "remove", "resume", "step", "stepover", "skipover"])
      .describe("Debugger command"),
    breakpoint: z.string().optional().describe("Node ID for add/remove breakpoint commands"),
    history: z.boolean().optional().describe("Include message history at breakpoints"),
  },
  async (params) => devConsoleTool("debug", params),
);

server.tool(
  "camel_message_history",
  "Get the message history showing the path exchanges have taken through processors, with timing information.",
  {},
  async () => devConsoleTool("message-history"),
);

// ---------------------------------------------------------------------------
// Interaction
// ---------------------------------------------------------------------------

server.tool(
  "camel_send",
  "Send a test message to an endpoint in the running Camel context. Returns the exchange result and any response body for InOut exchanges.",
  {
    endpoint: z.string().describe("Endpoint URI to send to (e.g., 'direct:myRoute', 'seda:queue')"),
    body: z.string().optional().describe("Message body to send"),
    exchangePattern: z
      .enum(["InOnly", "InOut"])
      .optional()
      .describe("Exchange pattern (default: InOnly)"),
  },
  async (params) => devConsoleTool("send", params),
);

server.tool(
  "camel_browse",
  "Browse pending messages on browsable endpoints (seda, mock, stub, browse). Shows queued messages with headers and body.",
  {
    filter: z.string().optional().describe("Endpoint URI pattern to filter (e.g., 'seda:*')"),
    limit: z.number().optional().describe("Maximum number of messages to return"),
    dump: z.boolean().optional().describe("Include full message body content"),
  },
  async (params) => devConsoleTool("browse", params),
);

server.tool(
  "camel_eval",
  "Evaluate a Camel expression (Simple, JsonPath, XPath, etc.) against an optional test message body. Returns the expression result or predicate boolean.",
  {
    language: z.string().optional().describe("Expression language (default: simple)"),
    template: z.string().describe("Expression to evaluate"),
    body: z.string().optional().describe("Test message body to evaluate against"),
    predicate: z.boolean().optional().describe("Evaluate as a predicate (returns true/false)"),
  },
  async (params) => devConsoleTool("eval-language", params),
);

// ---------------------------------------------------------------------------
// Additional consoles
// ---------------------------------------------------------------------------

server.tool(
  "camel_variables",
  "List Camel variables currently set in the context.",
  {},
  async () => devConsoleTool("variables"),
);

server.tool(
  "camel_beans",
  "List beans registered in the Camel registry.",
  {
    filter: z.string().optional().describe("Filter beans by name pattern"),
  },
  async (params) => devConsoleTool("bean", params),
);

server.tool(
  "camel_properties",
  "Show resolved configuration properties.",
  {
    filter: z.string().optional().describe("Filter properties by key pattern"),
  },
  async (params) => devConsoleTool("properties", params),
);

server.tool(
  "camel_consumers",
  "List all consumers (input endpoints) in the Camel context.",
  {},
  async () => devConsoleTool("consumer"),
);

server.tool(
  "camel_services",
  "List services managed by the Camel context.",
  {},
  async () => devConsoleTool("service"),
);

server.tool(
  "camel_blocked",
  "List exchanges that are blocked waiting for a reply (e.g., InOut calls).",
  {},
  async () => devConsoleTool("blocked"),
);

server.tool(
  "camel_type_converters",
  "Show type converter statistics and available converters.",
  {},
  async () => devConsoleTool("type-converter"),
);

// ---------------------------------------------------------------------------
// Prompts
// ---------------------------------------------------------------------------

server.prompt(
  "camel_debug_route",
  "Guided workflow to debug a failing Camel route: discover health issues, identify errors, enable tracing, and diagnose the root cause.",
  {
    routeId: z.string().optional().describe("Route ID to focus on (optional)"),
  },
  ({ routeId }) => {
    const routeFilter = routeId ? ` Filter by routeId="${routeId}".` : "";
    return {
      messages: [
        {
          role: "user",
          content: {
            type: "text",
            text: `You are debugging a Camel application. Follow this structured workflow:

## Step 1: Health Check
Call \`camel_health\` to check overall application health. Report any failing checks.

## Step 2: Route Status
Call \`camel_routes\`${routeFilter} to see route statistics. Look for:
- Routes with high \`exchangesFailed\` counts
- Routes in non-Started state
- Routes with unusually high mean processing times

## Step 3: Error Details
Call \`camel_errors\` with \`stackTrace=true\`${routeFilter} to get recent errors. Classify each error:
- Missing component/endpoint → dependency issue
- Type conversion → data format mismatch
- Connection refused → external service down
- Expression evaluation → DSL/expression error

## Step 4: Enable Tracing
Call \`camel_trace\` with \`enabled=true\` to start tracing messages.
Then call \`camel_trace\` with \`dump=true\` to retrieve traced messages.
Analyze the message flow to identify where processing fails or diverges.

## Step 5: Performance Hotspots
Call \`camel_top\` with \`processors=true\` to find the slowest processors.
Cross-reference with the error data from Step 3.

## Step 6: Root Cause Analysis
Synthesize findings from all steps into a diagnosis:
- What is failing and why
- Which component/processor is the root cause
- Suggested fix with specific actions

Present a clear summary with the root cause and recommended fix.`,
          },
        },
      ],
    };
  },
);

server.prompt(
  "camel_performance_analysis",
  "Guided workflow to analyze performance of a running Camel application: identify slow routes, bottleneck processors, queue depths, and stuck exchanges.",
  {},
  () => ({
    messages: [
      {
        role: "user",
        content: {
          type: "text",
          text: `You are analyzing the performance of a Camel application. Follow this workflow:

## Step 1: Context Overview
Call \`camel_context\` to get uptime, version, and memory stats.

## Step 2: Route Statistics
Call \`camel_routes\` to get all route statistics. Sort by:
- Highest \`exchangesTotal\` (busiest routes)
- Highest \`meanProcessingTime\` (slowest routes)
- Highest \`exchangesFailed\` / \`exchangesTotal\` ratio (most error-prone)

## Step 3: Processor Hotspots
Call \`camel_top\` with \`processors=true\` to find the slowest individual processors.
Identify which route each slow processor belongs to.

## Step 4: Queue Depths
Call \`camel_browse\` to check queue depths on seda/stub endpoints.
High queue depths indicate consumers can't keep up with producers.

## Step 5: Stuck Exchanges
Call \`camel_inflight\` to check for exchanges that have been processing for a long time.
Call \`camel_blocked\` to check for exchanges waiting on replies.

## Step 6: Summary
Present a performance report:
- Top 3 bottleneck processors with their routes
- Queue depth warnings
- Any stuck or long-running exchanges
- Specific recommendations for improvement (e.g., increase consumers, add threading, optimize processor X)`,
        },
      },
    ],
  }),
);

// ---------------------------------------------------------------------------
// Resources
// ---------------------------------------------------------------------------

server.resource("camel-consoles", "camel://consoles", async (uri) => {
  const text = await callDevConsole("");
  return { contents: [{ uri: uri.href, mimeType: "application/json", text }] };
});

// ---------------------------------------------------------------------------
// Start
// ---------------------------------------------------------------------------

const transport = new StdioServerTransport();
await server.connect(transport);
