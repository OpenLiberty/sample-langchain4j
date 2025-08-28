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

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.logging.Level;

import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.microprofile.metrics.annotation.Timed;

import static dev.langchain4j.model.output.FinishReason.LENGTH;

import io.openliberty.sample.langchain4j.StreamingChatAgent.StreamingAssistant;

@ApplicationScoped
@ServerEndpoint("/streamingchat")
public class StreamingChatService {

    private static Logger logger = Logger.getLogger(StreamingChatService.class.getName());

    @Inject
    StreamingChatAgent agent;

    private StreamingAssistant assistant;

    @OnOpen
    public void onOpen(Session session) throws Exception {
        logger.info("Server connected to session: " + session.getId());
        assistant = agent.getStreamingAssistant();
    }

    @OnMessage
    @Timed(name = "chatProcessingTime", absolute = true,
           description = "Time needed chatting to the agent.")
    public void onMessage(String message, Session session) throws Exception {
        logger.info("Server received message \"" + message + "\" "
                    + "from session: " + session.getId());

        CompletableFuture<ChatResponse> response = new CompletableFuture<>();
        assistant.streamingChat(session.getId(), message)
            .onPartialResponse(token -> {
                if (token.isEmpty())
                    return;
                try {
                    session.getBasicRemote().sendText(token);
                    Thread.sleep(100);
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            })
            .onCompleteResponse(response::complete)
            .onError(response::completeExceptionally)
            .start();
        if (response.get().finishReason() == LENGTH)
            session.getBasicRemote().sendText(" ...");
	session.getBasicRemote().sendText("");

        logger.info("Server finished response to session: " + session.getId());
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) throws Exception {
        logger.info("Session " + session.getId() + " was closed with reason " + closeReason.getCloseCode());
	assistant.evictChatMemory(session.getId());
    }

    @OnError
    public void onError(Session session, Throwable error) throws Exception {
        logger.info("WebSocket error for " + session.getId() + " " + error.getMessage());
	session.getBasicRemote().sendText("My failure reason is:\n\n" + error.getMessage());
	session.getBasicRemote().sendText("");
    }

}
