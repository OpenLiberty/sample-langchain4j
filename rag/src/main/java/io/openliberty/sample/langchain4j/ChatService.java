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

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.microprofile.metrics.annotation.Timed;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.util.Base64;

@ApplicationScoped
@ServerEndpoint(value = "/chat", encoders = { ChatMessageEncoder.class })
public class ChatService {
   
    private static Logger logger = Logger.getLogger(ChatService.class.getName());
    private final String ADMIN_USERNAME = "bob";
    private final String ADMIN_PASSWD = "bobpwd";
    @Inject
    ChatAgent agent = null;
    

    @OnOpen
    public void onOpen(Session session) {
        logger.info("Server connected to session: " + session.getId());
        File file = new File("resources/knowledge_base");
        String fullPath = file.getAbsolutePath();
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(fullPath, new ApacheTikaDocumentParser());

        var docSplitter = DocumentSplitters.recursive(400, 50);

        List<TextSegment> textSeg = docSplitter.splitAll(documents);
        insertToDatabase(textSeg);
    }
    
    public void insertToDatabase(List<TextSegment> contentSeg){
        
        String startURL = "http://localhost:9080/api/embedding?";
        int i = 0;
        for (TextSegment content : contentSeg){
            i +=1;
            String contentEncoded = "";
            try{
                contentEncoded = URLEncoder.encode(content.text(), "UTF-8");
                String parameters = String.format("summary=%s&content=%s",i,contentEncoded);
                try{
                    URL url = new URI(startURL+parameters).toURL();
                    HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
                    String login = ADMIN_USERNAME + ":" + ADMIN_PASSWD;
                    String encodedCredentials = Base64.getEncoder().encodeToString((login).getBytes());
                    String value = "Basic " + encodedCredentials;
                    httpConnection.setRequestProperty("Authorization", value);
                    httpConnection.setRequestMethod("POST");
                    httpConnection.setRequestProperty("Accept", "*/*");
                    httpConnection.setDoOutput(false);
                    if (httpConnection.getResponseCode() != 200){
                        logger.warning("Could not connect to api");
                    }
                    
                }catch(Exception e){
                    logger.warning("Could not load knowledge base into MongoDB.");
                }
            }catch(Exception e){
                    logger.warning("Could not load knowledge base into MongoDB.");            
            }
        }

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
            answer = agent.chat(sessionId, message);
        } catch (Exception e) {
            //Work in progress area
            // but running the full app works.
            
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
        logger.info("Session " + session.getId()
                    + " was closed with reason " + closeReason.getCloseCode());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.info("WebSocket error for " + session.getId() + " "
                    + throwable.getMessage());
    }

}
