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

import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.memory.ChatMemoryAccess;

import io.openliberty.sample.langchain4j.client.util.ModelBuilder;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ChatAgent {

    @Inject
    private ModelBuilder modelBuilder;

    @Inject
    private McpClient mcpClient;

    @Inject
    @ConfigProperty(name = "chat.memory.max.messages")
    private Integer MAX_MESSAGES;

    interface Assistant extends ChatMemoryAccess {
        @SystemMessage("""
            You are a coding helper. Users will ask you questions related to coding.

            You have access to ONLY four MCP tools:
            - stackoverflow-search(query)
            - stackoverflow-jakarta-ee-top()
            - stackoverflow-microprofile-top()
            - stackoverflow-langchain4j-top()

            TOOL USAGE:
            - Use tools ONLY when necessary.
            - Use stackoverflow-search for specific errors or how-to questions.
            - Use a "top" tool for broad best-practice or resource requests.

            RESPONSE FORMAT:
            - Always respond in a structured format:
                1. **Problem Title**
                    Description of the issue
                    Link to the source
                2. **Next Problem Title**
                    Description of the issue
                    Link to the source
            - Do NOT place links at the bottom of the response. Each problem must include its link directly after the description.
            - Use numbered points for each problem-solution pair.

            RULES:
            - NEVER mention tool calls or internal processes.
            - ALWAYS follow tool parameters exactly—no extra or missing parameters.
            - NEVER fabricate information. If unsure, say so.
            - NEVER include unnecessary information.
            """)
        String chat(@MemoryId String sessionId, @UserMessage String userMessage);
    }

    private McpToolProvider toolProvider;

    private Assistant assistant;

    @PostConstruct
    void init() {
        toolProvider = McpToolProvider.builder()
            .mcpClients(List.of(mcpClient))
            .build();
    }

    public synchronized Assistant getAssistant() throws Exception {
        if (assistant == null) {
            ChatModel model = modelBuilder.getChatModel();
            assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .toolProvider(toolProvider)
                .hallucinatedToolNameStrategy(req ->
                    ToolExecutionResultMessage.from(req,
                        "Error: there is no tool with the following parameters called " + req.name()))
                .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(MAX_MESSAGES))
                .build();
        }
        return assistant;
    }

    public String chat(String sessionId, String message) throws Exception {
        return getAssistant().chat(sessionId, message).trim();
    }

    public void clearChatMemory(String sessionId) {
        if (assistant == null) {
            throw new IllegalStateException("assistant not yet initialized");
        }
        assistant.evictChatMemory(sessionId);
    }
}
