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

import java.util.logging.Logger;

import org.eclipse.microprofile.metrics.annotation.Timed;

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

@ApplicationScoped
@ServerEndpoint("/streamingchat")
public class StreamingChatService {

    private static Logger logger = Logger.getLogger(StreamingChatService.class.getName());

    @Inject
    StreamingChatAgent agent = null;

    @OnOpen
    public void onOpen(Session session) {
        logger.info("Server connected to session: " + session.getId());
    }

    @OnMessage
    @Timed(name = "chatProcessingTime", absolute = true,
           description = "Time needed chatting to the agent.")
    public void onMessage(String message, Session session) throws Exception {

        logger.info("Server received message \"" + message + "\" "
                    + "from session: " + session.getId());

        RemoteEndpoint.Basic remote = session.getBasicRemote();

        try {
            String sessionId = session.getId();
            switch (agent.streamingChat(sessionId, message, token -> {
                remote.sendText(token);
                Thread.sleep(100);
            })) {
            case STOP:
                break;
            default:
                remote.sendText(" ...");
            }
        } catch (Exception e) {
            remote.sendText("My failure reason is:\n\n" + e.getMessage());
        }

        remote.sendText("");
        logger.info("Server finished response to session: " + session.getId());

    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info("Session " + session.getId()
                    + " was closed with reason " + closeReason.getCloseCode());
        agent.clearChatMemory(session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.info("WebSocket error for " + session.getId() + " "
                    + throwable.getMessage());
    }

}
