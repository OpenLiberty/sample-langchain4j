package it.io.openliberty.sample.langchain4j;

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
public class ToolServiceIT {

    private static CountDownLatch countDown;

    private static ToolClient client;
    
    @BeforeEach
    public void setup() throws Exception {
        countDown = new CountDownLatch(1);
        URI uri = new URI("ws://localhost:9080/toolchat");
        client = new ToolClient(uri);
    }

    @AfterEach
    public void teardown() throws Exception {
      client.close();
    }

    @Test
    public void testJakartaEE() throws Exception {
        if (Util.usingHuggingFace()) {
            return;
        }

        client.sendMessage("What are some current problems users have with JakartaEE?");
        countDown.await(120, TimeUnit.SECONDS);
    }

    @Test
    public void testMicroProfile() throws Exception {
        if (Util.usingHuggingFace()) {
            return;
        }

        client.sendMessage("What are some current problems users have with MicroProfile?");
        countDown.await(120, TimeUnit.SECONDS);
    }

    @Test
    public void testLangChain4j() throws Exception {
        if (Util.usingHuggingFace()) {
            return;
        }

        client.sendMessage("What are some current problems users have with LangChain4J?");
        countDown.await(120, TimeUnit.SECONDS);
    }

    public static void verify(String message) {
        assertNotNull(message);

        String text = message.toLowerCase();
        assertTrue(text.contains("microprofile") ||
            text.contains("jakarta") || text.contains("langchain"),
            message);
        countDown.countDown();
    }

}
