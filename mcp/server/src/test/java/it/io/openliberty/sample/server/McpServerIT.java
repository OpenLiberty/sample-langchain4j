/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package it.io.openliberty.sample.server;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class McpServerIT {

    private static final String PORT = System.getProperty("http.port", "9081");
    private static final String BASE_URL = "http://localhost:" + PORT;

    private static McpSyncClient client;
    private static McpClientTransport transport;

    @BeforeAll
    static void startClientAndInitialize() {
        transport = HttpClientStreamableHttpTransport
                .builder(BASE_URL)
                .endpoint("/mcp")
                .build();

        client = McpClient.sync(transport).build();

        client.initialize();
        client.ping();
    }

    @AfterAll
    static void shutdown() {
        if (client != null) {
            try {
                client.closeGracefully();
            } catch (Throwable t) {
                client.close();
            }
        }
        if (transport != null) {
            try {
                transport.close();
            } catch (Throwable ignore) {
            }
        }
    }

    @Test
    @Order(1)
    void testListTools() {
        ListToolsResult tools = client.listTools();
        assertNotNull(tools, "No tools/list response");

        boolean hasSearch = tools.tools().stream()
                .anyMatch(t -> "stackoverflow-search".equals(t.name()));
        boolean hasJakartaTop = tools.tools().stream()
                .anyMatch(t -> "stackoverflow-jakarta-ee-top".equals(t.name()));
        boolean hasMpTop = tools.tools().stream()
                .anyMatch(t -> "stackoverflow-microprofile-top".equals(t.name()));
        boolean hasLangchainTop = tools.tools().stream()
                .anyMatch(t -> "stackoverflow-langchain4j-top".equals(t.name()));

        assertTrue(hasSearch, "Missing stackoverflow-search tool");
        assertTrue(hasJakartaTop, "Missing stackoverflow-jakarta-ee-top tool");
        assertTrue(hasMpTop, "Missing stackoverflow-microprofile-top tool");
        assertTrue(hasLangchainTop, "Missing stackoverflow-langchain4j-top tool");
    }

    @Test
    @Order(2)
    void testNoArgTool() {
        CallToolResult res = client.callTool(
                new CallToolRequest("stackoverflow-jakarta-ee-top", Map.of())
        );
        assertNotNull(res, "Null tool result");
        assertFalse(Boolean.TRUE.equals(res.isError()), "Tool returned isError=true");

        boolean hasText = res.content().stream()
                .filter(TextContent.class::isInstance)
                .map(TextContent.class::cast)
                .anyMatch(t -> t.text() != null && !t.text().isBlank());
        assertTrue(hasText, "Expected non-blank TextContent in result");
    }

    @Test
    @Order(3)
    void testParameterizedTool() {
        CallToolResult res = client.callTool(
                new CallToolRequest("stackoverflow-search", Map.of("query", "Ollama"))
        );
        assertNotNull(res, "Null tool result");
        assertFalse(Boolean.TRUE.equals(res.isError()), "Tool returned isError=true");

        boolean hasText = res.content().stream()
                .filter(TextContent.class::isInstance)
                .map(TextContent.class::cast)
                .anyMatch(t -> t.text() != null && !t.text().isBlank());
        assertTrue(hasText, "Expected non-blank TextContent in result");
    }
}
