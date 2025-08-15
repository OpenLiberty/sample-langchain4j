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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bson.conversions.Bson;
import org.eclipse.microprofile.metrics.annotation.Timed;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.util.AbstractMap;
import java.util.ArrayList;

import java.util.Comparator;
import java.util.PriorityQueue;

import org.apache.lucene.util.VectorUtil;


@ApplicationScoped
@ServerEndpoint(value = "/chat", encoders = { ChatMessageEncoder.class })
public class ChatService {
   
    private static Logger logger = Logger.getLogger(ChatService.class.getName());

    private int MAX_RESULTS = 3;

    private EmbeddingModel embModel = new AllMiniLmL6V2EmbeddingModel();

    private PriorityQueue<Map.Entry<String, Float>> maxHeap = new PriorityQueue<>(Map.Entry.comparingByValue(Comparator.reverseOrder()));

    @Inject
    ChatAgent agent = null;
   
    @Inject
    private MongoDatabase db;

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
            List<String> topThree = getSimilarContent(message);
            message += "\nHere are the top similar content from the knowledge base (use relevent info only): ";;
            for (String part : topThree){
                message += "\n" + part;
            }
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

    public List<String> getSimilarContent(String userMessage){
        byte[] messageVec = toBytes(embModel.embed(userMessage).content().vector());

        List<String> result = new ArrayList<>();

        MongoCollection<org.bson.Document> embeddingStore = db.getCollection("EmbeddingsStored");

        Bson projection = Projections.fields(Projections.include( "Vector", "Content"));

        FindIterable<org.bson.Document> docs = embeddingStore.find().projection(projection);

        for (org.bson.Document d : docs) {

            org.bson.types.Binary binaryVectorFormat = d.get("Vector", org.bson.types.Binary.class);
            byte[] vec = binaryVectorFormat.getData();
            Float similarity = VectorUtil.cosine(messageVec, vec);
            maxHeap.offer(new AbstractMap.SimpleEntry<>((String) d.getString("Content"), similarity));
            
        }
        
        int currRes = 0;
        while(!maxHeap.isEmpty()){
            Map.Entry<String,Float> topThree = maxHeap.poll();

            if (currRes <= MAX_RESULTS){

                result.add(topThree.getKey());
                currRes +=1;
                
            }else{
                break;
            }

        }

        return result;
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info("Session " + session.getId()
                    + " was closed with reason " + closeReason.getCloseCode());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.info("WebSocket error for " + session.getId() + " "
                    + throwable.getMessage());
    }

}
