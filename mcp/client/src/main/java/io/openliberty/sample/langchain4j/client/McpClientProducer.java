/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.sample.langchain4j.client;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class McpClientProducer {

    private static Logger logger = Logger.getLogger(McpClientProducer.class.getName());

    @Inject
    @ConfigProperty(name = "mcp.base.url")
    String baseUrl;

    @Inject
    @ConfigProperty(name = "mcp.sse.path")
    String ssePath;

    private McpClient client;

    @PostConstruct
    void init() {
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl(baseUrl + ssePath)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();

        client = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
    }

    @Produces
    @ApplicationScoped
    public McpClient mcpClient() {
        return client;
    }

    @PreDestroy
    void shutdown() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to close MCP client: " + e.getMessage());
            }
        }
    }
}
