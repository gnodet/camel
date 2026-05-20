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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.StaticService;
import org.apache.camel.console.DevConsole;
import org.apache.camel.console.DevConsoleRegistry;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embedded MCP server service that exposes Camel DevConsole data as MCP tools.
 */
public class McpServerService extends ServiceSupport implements CamelContextAware, StaticService {

    private static final Logger LOG = LoggerFactory.getLogger(McpServerService.class);

    private CamelContext camelContext;
    private McpSyncServer syncServer;

    private String serverName = "camel-mcp-server";
    private String serverVersion = "1.0.0";
    private String transport = "stdio";
    private boolean devConsoleEnabled = true;
    private String includeTools;
    private String excludeTools;

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    protected void doInit() throws Exception {
        ObjectHelper.notNull(camelContext, "CamelContext", this);
    }

    @Override
    protected void doStart() throws Exception {
        ObjectHelper.notNull(camelContext, "CamelContext", this);

        McpServerTransportProvider transportProvider = createTransportProvider();

        McpServer.SyncSpecification<?> spec = McpServer.sync(transportProvider)
                .serverInfo(serverName, serverVersion)
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .prompts(true)
                        .resources(true, false)
                        .build());

        if (devConsoleEnabled) {
            McpToolRegistry toolRegistry = new McpToolRegistry(camelContext);
            List<McpServerFeatures.SyncToolSpecification> tools = toolRegistry.buildTools(includeTools, excludeTools);
            for (McpServerFeatures.SyncToolSpecification tool : tools) {
                spec = spec.tools(tool);
            }
            LOG.debug("Registered {} MCP tools from DevConsole", tools.size());
        }

        McpPromptRegistry promptRegistry = new McpPromptRegistry();
        List<McpServerFeatures.SyncPromptSpecification> prompts = promptRegistry.buildPrompts();
        for (McpServerFeatures.SyncPromptSpecification prompt : prompts) {
            spec = spec.prompts(prompt);
        }

        // register a resource listing available dev consoles
        DevConsoleRegistry dcr = camelContext.getCamelContextExtension()
                .getContextPlugin(DevConsoleRegistry.class);
        if (dcr != null) {
            McpSchema.Resource resource = new McpSchema.Resource(
                    "camel://consoles",
                    "camel-consoles",
                    null,
                    "Lists all available DevConsole endpoints in this Camel application",
                    "application/json",
                    null, null, null);
            spec = spec.resources(new McpServerFeatures.SyncResourceSpecification(
                    resource,
                    (exchange, request) -> {
                        JsonObject json = new JsonObject();
                        for (String id : dcr.getConsoleIDs()) {
                            DevConsole console = dcr.resolveById(id);
                            if (console != null) {
                                json.put(id, console.getDescription());
                            }
                        }
                        return new McpSchema.ReadResourceResult(
                                List.of(new McpSchema.TextResourceContents(
                                        request.uri(), "application/json", json.toJson())));
                    }));
        }

        syncServer = spec.build();
        LOG.info("MCP Server started (transport={}, tools={}, prompts={})",
                transport,
                devConsoleEnabled ? "enabled" : "disabled",
                prompts.size());
    }

    @Override
    protected void doStop() throws Exception {
        if (syncServer != null) {
            syncServer.close();
            syncServer = null;
            LOG.info("MCP Server stopped");
        }
    }

    private McpServerTransportProvider createTransportProvider() {
        if ("stdio".equals(transport)) {
            return new StdioServerTransportProvider(
                    new JacksonMcpJsonMapper(new ObjectMapper()));
        }
        throw new IllegalArgumentException(
                "Unsupported MCP transport: " + transport
                                           + ". Currently only 'stdio' is supported.");
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public boolean isDevConsoleEnabled() {
        return devConsoleEnabled;
    }

    public void setDevConsoleEnabled(boolean devConsoleEnabled) {
        this.devConsoleEnabled = devConsoleEnabled;
    }

    public String getIncludeTools() {
        return includeTools;
    }

    public void setIncludeTools(String includeTools) {
        this.includeTools = includeTools;
    }

    public String getExcludeTools() {
        return excludeTools;
    }

    public void setExcludeTools(String excludeTools) {
        this.excludeTools = excludeTools;
    }

}
