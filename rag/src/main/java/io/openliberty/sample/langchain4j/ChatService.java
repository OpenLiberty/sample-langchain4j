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



import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.microprofile.metrics.annotation.Timed;

import io.openliberty.sample.langchain4j.mongo.AtlasMongoDB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ApplicationScoped
@ServerEndpoint(value = "/chat", encoders = { ChatMessageEncoder.class })
public class ChatService {
    private static Logger logger = Logger.getLogger(ChatService.class.getName());

    @Inject
    ChatAgent agent = null;

    @Inject     
    AtlasMongoDB mongodbFunction;

    @OnOpen
    public void onOpen(Session session) {
        logger.info("Server connected to session: " + session.getId());
        System.out.println("In order to use the knowledge base: visit http://localhost:9081/openapi/ui/ and" +
        "\ntry the POST request at `/api/embedding/init` to initialize the database.");
    }

    private List<Float> toFloat(float[] embedding){
        List<Float> vector = new ArrayList<>();
        for (float elem : embedding) {
            vector.add(elem);
        }
        return vector;
    }
    
    @OnMessage
    @Timed(name = "chatProcessingTime", absolute = true,
           description = "Time needed chatting to the agent.")
    public void onMessage(String message, Session session) {

        logger.info("Server received message \"" + message + "\" "
                    + "from session: " + session.getId());

        String answer;
        try {
            String sessionId = session.getId();
            float[] userQueryEmbedding = mongodbFunction.convertUserQueryToEmbedding(message);
            List<Float> result = toFloat(userQueryEmbedding);
            List<String> output = mongodbFunction.retrieveContent(result,message);
            message += "Here are some relevent information from the knowledge base:";
            message += output;
            answer = agent.chat(sessionId, message);
        } catch (Exception e) {
            answer = "My failure reason is:\n\n" + e.getMessage();
        }

        try {
            session.getBasicRemote().sendObject(answer);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info("Session " + session.getId() +
            " was closed with reason " + closeReason.getCloseCode());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.severe("WebSocket error for " + session.getId() + " " +
            throwable.getMessage());
    }
}
