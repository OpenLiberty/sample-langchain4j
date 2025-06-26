package io.openliberty.sample.langchain4j;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import io.openliberty.sample.langchain4j.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ChatAgent {
    @Inject
    private ModelBuilder modelBuilder;

    @Inject
    private StackOverflowTools stackOverflowTools;

    @Inject
    @ConfigProperty(name = "chat.memory.max.messages")
    private Integer MAX_MESSAGES;

    interface Assistant {
        @SystemMessage("You are a coding helper, people will go to you for questions around coding. " +
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
            ChatModel model = modelBuilder.getChatModelForWeb();
            assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(stackOverflowTools)
                .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                    toolExecutionRequest, "Error: there is no tool with the following parameters called" + toolExecutionRequest.name()))
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

}
