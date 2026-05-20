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

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Service;
import org.apache.camel.main.MainConstants;
import org.apache.camel.main.MainMcpServerFactory;
import org.apache.camel.main.McpServerConfigurationProperties;
import org.apache.camel.spi.annotations.JdkService;

/**
 * @since 4.21
 */
@JdkService(MainConstants.MCP_SERVER)
public class DefaultMainMcpServerFactory implements CamelContextAware, MainMcpServerFactory {

    private CamelContext camelContext;

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public Service newMcpServer(CamelContext camelContext, McpServerConfigurationProperties configuration) {
        McpServerService server = new McpServerService();
        server.setCamelContext(camelContext);
        server.setServerName(configuration.getServerName());
        server.setServerVersion(configuration.getServerVersion());
        server.setTransport(configuration.getTransport());
        server.setDevConsoleEnabled(configuration.isMcpDevConsoleEnabled());
        server.setIncludeTools(configuration.getIncludeTools());
        server.setExcludeTools(configuration.getExcludeTools());
        return server;
    }

}
