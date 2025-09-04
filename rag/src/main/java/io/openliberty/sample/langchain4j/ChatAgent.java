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

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.mongodb.client.MongoDatabase;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.openliberty.sample.langchain4j.mongo.AtlasMongoDB;
import io.openliberty.sample.langchain4j.util.ModelBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ChatAgent {

    AtlasMongoDB mongoDB = new AtlasMongoDB();

	@Inject
    private ModelBuilder modelBuilder;

    @Inject 
    private MongoDatabase db;

    @Inject
    @ConfigProperty(name = "chat.memory.max.messages")
    private Integer MAX_MESSAGES;

    interface Assistant {
        @SystemMessage("You are a Java, Jakarta EE and MicroProfile coding helper, " +
            "people will go to you for questions around coding. " +
            "You have ONLY four tools. " +
            "ONLY use the tools if NECESSARY. " +
            "ALWAYS follow the tool call parameters exactly and make sure to provide ALL necessary parameters. " +
            "Do NOT add more parameters than needed" + 
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
        
        float[] userQueryEmbedding = mongoDB.convertUserQueryToEmbedding(message);
        List<Float> result = new ArrayList<>();
        for (float element : userQueryEmbedding) {
            result.add(element); 
        }
        List<String> res = mongoDB.retrieveContent(result,db);
        System.out.println("content retrieved: ");
        for (String e: res){
            System.out.println("SEG : \n" + e);
        }
        message += "Here are some relevent information from the knowledge base:";
        message += res;
        String reply = getAssistant().chat(sessionId, message).trim();
        return reply;
    }

}
