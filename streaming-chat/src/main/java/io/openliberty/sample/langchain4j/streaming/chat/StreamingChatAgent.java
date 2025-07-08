/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.sample.langchain4j.streaming.chat;

import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.openliberty.sample.langchain4j.util.ModelBuilder;

@ApplicationScoped
public class StreamingChatAgent {

    @Inject
    private ModelBuilder modelBuilder;

    @Inject
    @ConfigProperty(name = "chat.memory.max.messages")
    private Integer MAX_MESSAGES;

    @FunctionalInterface
    interface PartialResponseHandler {
        void onPartialResponse(String token) throws Exception;
    }

    interface StreamingAssistant {
       TokenStream streamingChat(@MemoryId String sessionId, @UserMessage String userMessage);
    }

    private StreamingAssistant assistant = null;

    public StreamingAssistant getStreamingAssistant() throws Exception {
        if (assistant == null) {
            StreamingChatModel streamingModel = modelBuilder.getStreamingChatModel();
            assistant = AiServices.builder(StreamingAssistant.class)
                .streamingChatModel(streamingModel)
                .chatMemoryProvider(
                    sessionId -> MessageWindowChatMemory.withMaxMessages(MAX_MESSAGES))
                .build();
        }
        return assistant;
    }

    public FinishReason streamingChat(String sessionId, String message, PartialResponseHandler handler) throws Exception {
        CompletableFuture<ChatResponse> future = new CompletableFuture<>();
        getStreamingAssistant().streamingChat(sessionId, message)
            .onPartialResponse(token -> {
                try {
                    handler.onPartialResponse(token);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            })
            .onCompleteResponse(future::complete)
            .onError(future::completeExceptionally)
            .start();
        return future.get().finishReason();
    }

}
