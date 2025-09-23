/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.sample.server;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.json.McpJsonMapper;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration.Dynamic;

public class McpServerInitializer implements ServletContextListener {

    @Inject
    private StackOverflowAsyncTools stackOverflowAsyncTools;

    private McpAsyncServer server;
    private HttpServletStreamableServerTransportProvider transport;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();

        transport = HttpServletStreamableServerTransportProvider.builder()
            .jsonMapper(McpJsonMapper.getDefault())
            .mcpEndpoint("/mcp")
            .build();

        Dynamic registration = ctx.addServlet("mcp-streamable", transport);
        registration.addMapping("/mcp");
        registration.setAsyncSupported(true);
        registration.setLoadOnStartup(1);

        server = McpServer.async(transport)
            .serverInfo("mcp-stackoverflow-server", "1.0.0")
            .capabilities(ServerCapabilities.builder()
                .tools(true)
                .logging()
                .build())
            .tools(
                stackOverflowAsyncTools.jakartaEETop(),
                stackOverflowAsyncTools.microProfileTop(),
                stackOverflowAsyncTools.langChain4jTop(),
                stackOverflowAsyncTools.search()
            )
            .build();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            if (server != null) {
                try {
                    server.closeGracefully();
                } catch (Throwable t) {
                    server.close();
                }
            }
            if (transport != null) {
                try {
                    transport.closeGracefully().block();
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable ignore) {
        }
    }
}
