package io.openliberty.sample.langchain4j;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class StreamingChatServiceIT {

    private static StringBuilder builder;
    private static CompletableFuture<String> future;

    @Test
    public void testChat() throws Exception {
        if (Util.usingHuggingFace()) {
            return;
        }
        URI uri = new URI("ws://localhost:9080/streamingchat");
        StreamingChatClient client = new StreamingChatClient(uri);
        future = new CompletableFuture<>();
        builder = new StringBuilder();
        client.sendMessage("When was the LangChain4j launched?");
        String message;
        try {
            message = future.get(20, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            message = builder.toString();
        }
        client.close();
        assertNotNull(message);
        assertTrue(message.contains("2020") || message.contains("2021") ||
            message.contains("2022") || message.contains("2023"),
            message);
    }

    public static void verify(String message) {
        if (message.equals("")) {
            future.complete(builder.toString());
        }
        builder.append(message);
    }

}
