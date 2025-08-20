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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Logger;

import org.apache.lucene.util.VectorUtil;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.metrics.annotation.Timed;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;

import io.openliberty.sample.langchain4j.util.ModelBuilder;
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

    private static int MAX_RESULTS = 3;

    @Inject
    private ModelBuilder modelBuilder;

    @Inject
    ChatAgent agent = null;
   
    @Inject
    private MongoDatabase db;

    private PriorityQueue<Map.Entry<String, Float>> maxHeap = new PriorityQueue<>(Map.Entry.comparingByValue(Comparator.reverseOrder()));

    @OnOpen
    public void onOpen(Session session) {
        logger.info("Server connected to session: " + session.getId());
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
            answer = agent.chat(sessionId, getSimilarContent(message));
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

    private byte[] toBytes(float[] vector) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(bos)) {
            for (float f : vector) {
                dos.writeFloat(f);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bos.toByteArray();
    }

    private String getSimilarContent(String userMessage) {

        byte[] messageVec = toBytes(modelBuilder.getEmbeddingModel().embed(userMessage).content().vector());
        MongoCollection<org.bson.Document> embeddingStore = db.getCollection("EmbeddingsStored");
        Bson projection = Projections.fields(Projections.include( "Vector", "Content"));
        FindIterable<org.bson.Document> docs = embeddingStore.find().projection(projection);
        for (org.bson.Document d : docs) {
            org.bson.types.Binary binaryVectorFormat = d.get("Vector", org.bson.types.Binary.class);
            byte[] vec = binaryVectorFormat.getData();
            Float similarity = VectorUtil.cosine(messageVec, vec);
            maxHeap.offer(new AbstractMap.SimpleEntry<>((String) d.getString("Content"), similarity));
        }

        StringBuffer sb = new StringBuffer();
        sb.append(userMessage);
        sb.append("\nHere are the similar content from the knowledge base (use relevent info only):\n");
        for (int i = 0; !maxHeap.isEmpty() && i < MAX_RESULTS; i++) {
            Map.Entry<String,Float> emtry = maxHeap.poll();
            sb.append(emtry.getKey());
            sb.append("\n");
        }

        return sb.toString();

    }
}
