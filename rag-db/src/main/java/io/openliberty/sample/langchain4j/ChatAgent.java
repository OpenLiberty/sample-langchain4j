/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.sample.langchain4j;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.memory.ChatMemoryAccess;
import io.openliberty.sample.langchain4j.util.ModelBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ChatAgent {

	@Inject
    private ModelBuilder modelBuilder;

    @Inject
    @ConfigProperty(name = "chat.memory.max.messages")
    private Integer MAX_MESSAGES;

    interface Assistant extends ChatMemoryAccess {
        @SystemMessage("You are a Java, Jakarta EE and MicroProfile coding helper, " +
            "people will go to you for questions around coding. " +
            "NEVER give the user unnecessary information. " + 
            "NEVER lie or make information up, if you are unsure say so.")
        String chat(@MemoryId String sessionId, @UserMessage String userMessage);
    }

    private Assistant assistant = null;

    public Assistant getAssistant() throws Exception {
        if (assistant == null) {
            ChatModel model = modelBuilder.getChatModel();
            assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(
                    sessionId -> MessageWindowChatMemory.withMaxMessages(MAX_MESSAGES))
                .build();
        }
        return assistant;
    }

    public String chat(String sessionId, String message) throws Exception {
        String reply = getAssistant().chat(sessionId, message).trim();
        return reply;
    }

    public void clearChatMemory(String sessionId) {
        if (assistant == null) {
            throw new IllegalStateException("assistant not yet initialized");
        }
        assistant.evictChatMemory(sessionId);
    }

}
