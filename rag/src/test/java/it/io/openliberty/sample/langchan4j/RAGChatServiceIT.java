package it.io.openliberty.sample.langchan4j;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class RAGChatServiceIT {

    private static CountDownLatch countDown;

    private static RAGChatClient client;
    
    @BeforeEach
    public void setup() throws Exception {
        countDown = new CountDownLatch(1);
        URI uri = new URI("ws://localhost:9080/chat");
        client = new RAGChatClient(uri);
    }

    @AfterEach
    public void teardown() throws Exception {
      client.close();
    }

    @Test
    public void testLangChain4j() throws Exception {
        if (Util.usingHuggingFace()) {
            return;
        }

        client.sendMessage("How to chat with the assistant using langchain4j?");
        countDown.await(120, TimeUnit.SECONDS);
    }

    @Test
    public void testJakartaEE() throws Exception {
        if (Util.usingHuggingFace()) {
            return;
        }

        client.sendMessage("How to enable the Jakarta EE Web Profile in Open Liberty?");
        countDown.await(120, TimeUnit.SECONDS);
    }

    @Test
    public void testMicroProfile() throws Exception {
        if (Util.usingHuggingFace()) {
            return;
        }
        
        client.sendMessage("Create a Java class that uses the MicroProfile Health API to check/montior if the CPU usage is below 95%.\n");
        countDown.await(120, TimeUnit.SECONDS);
    }

    public static void verify(String message) {
        assertNotNull(message);

        assertTrue(message.toLowerCase().contains("microprofile") || message.contains("OperatingSystemMXBean") ||
            message.toLowerCase().contains("jakarta") || message.contains("webProfile-10.0") || 
            message.toLowerCase().contains("langchain") || message.toLowerCase().contains("assistant.chat") || message.toLowerCase().contains("interface Assistant"),
            message);
        countDown.countDown();
    }

}
