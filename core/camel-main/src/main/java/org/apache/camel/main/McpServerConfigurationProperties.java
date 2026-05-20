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
package org.apache.camel.main;

import org.apache.camel.spi.BootstrapCloseable;
import org.apache.camel.spi.Configurer;
import org.apache.camel.spi.Metadata;

/**
 * Configuration for embedded MCP server for standalone Camel applications (not Spring Boot / Quarkus).
 *
 * @since 4.21
 */
@Configurer(extended = true)
public class McpServerConfigurationProperties implements BootstrapCloseable {

    private MainConfigurationProperties parent;

    @Metadata(security = "insecure:dev")
    private boolean enabled;
    @Metadata(defaultValue = "stdio")
    private String transport = "stdio";
    @Metadata(defaultValue = "camel-mcp-server")
    private String serverName = "camel-mcp-server";
    @Metadata(defaultValue = "1.0.0")
    private String serverVersion = "1.0.0";
    @Metadata(defaultValue = "true")
    private boolean mcpDevConsoleEnabled = true;
    @Metadata
    private String includeTools;
    @Metadata
    private String excludeTools;

    public McpServerConfigurationProperties(MainConfigurationProperties parent) {
        this.parent = parent;
    }

    public MainConfigurationProperties end() {
        return parent;
    }

    @Override
    public void close() {
        parent = null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Whether embedded MCP server is enabled. By default, the server is not enabled.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTransport() {
        return transport;
    }

    /**
     * Transport to use for the MCP server. Currently only 'stdio' is supported.
     */
    public void setTransport(String transport) {
        this.transport = transport;
    }

    public String getServerName() {
        return serverName;
    }

    /**
     * Name of the MCP server reported to clients.
     */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    /**
     * Version of the MCP server reported to clients.
     */
    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public boolean isMcpDevConsoleEnabled() {
        return mcpDevConsoleEnabled;
    }

    /**
     * Whether to register MCP tools based on the DevConsole registry. By default, this is enabled.
     */
    public void setMcpDevConsoleEnabled(boolean mcpDevConsoleEnabled) {
        this.mcpDevConsoleEnabled = mcpDevConsoleEnabled;
    }

    public String getIncludeTools() {
        return includeTools;
    }

    /**
     * Comma-separated list of tool names to include. If set, only these tools will be registered.
     */
    public void setIncludeTools(String includeTools) {
        this.includeTools = includeTools;
    }

    public String getExcludeTools() {
        return excludeTools;
    }

    /**
     * Comma-separated list of tool names to exclude from registration.
     */
    public void setExcludeTools(String excludeTools) {
        this.excludeTools = excludeTools;
    }

}
