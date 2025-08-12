package it.io.openliberty.sample.langchan4j;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
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
    
    private final String ADMIN_USERNAME = "bob";

    private final String ADMIN_PASSWD = "bobpwd";

    @BeforeEach
    public void setup() throws Exception {
        countDown = new CountDownLatch(1);
        URI uri = new URI("ws://localhost:9080/chat");
        client = new RAGChatClient(uri);
        initializeDatabase();
    }
    
    public void initializeDatabase() throws Exception{
        
        String startURL = "http://localhost:9080/api/embedding/init";

        try{

            URL url = new URI(startURL).toURL();
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            String login = ADMIN_USERNAME + ":" + ADMIN_PASSWD;
            String encodedCredentials = Base64.getEncoder().encodeToString((login).getBytes());
            String value = "Basic " + encodedCredentials;
            httpConnection.setRequestProperty("Authorization", value);
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("Accept", "*/*");
            httpConnection.setDoOutput(false);

        }catch(Exception exception){

            throw new Exception("Error in initializing the knowledge base.");
        
        }
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
