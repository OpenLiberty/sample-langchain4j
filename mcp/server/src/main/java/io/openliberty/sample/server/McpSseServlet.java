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

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import jakarta.servlet.ServletException;

public class McpSseServlet extends HttpServletSseServerTransportProvider {

    private McpAsyncServer server;

    public McpSseServlet() {
        super(new ObjectMapper(), "/mcp/message");
    }

    @Override
    public void init() throws ServletException {
        super.init();

        var stackOverflowService = new StackOverflowService();
        var tools = new StackOverflowAsyncTools(stackOverflowService);

        server = McpServer.async(this)
            .serverInfo("mcp-stackoverflow-server", "1.0.0")
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .tools(true)
                    .logging()
                    .build()
            )
            .tools(
                tools.jakartaEETop(),
                tools.microProfileTop(),
                tools.langChain4jTop(),
                tools.search()
            )
            .build();
    }

    @Override
    public void destroy() {
        try {
            if (server != null) {
                try {
                    server.closeGracefully();
                } catch (Throwable t) {
                    server.close();
                }
            }
            try {
                super.closeGracefully();
            } catch (Throwable ignore) {}
        } finally {
            super.destroy();
        }
    }
}
