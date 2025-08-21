package it.io.openliberty.sample.langchan4j;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
        URI uri = new URI("ws://localhost:9081/chat");
        client = new RAGChatClient(uri);
        initializeDatabase();
    }
    
    public void initializeDatabase() throws Exception{
        
        String startURL = "http://localhost:9081/api/embedding/init";

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
            BufferedReader bufferReader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
        
            String line = "";
            StringBuilder res = new StringBuilder();

            while ((line = bufferReader.readLine()) != null) {
                res.append(line);
            }
            
            httpConnection.disconnect(); 

        }catch(Exception exception){
            
            throw new Exception("Error in initializing the knowledge base.");
        
        }
    }

    @AfterEach
    public void teardown() throws Exception {
      client.close();
    }

    @Test
    public void testLogs() throws Exception {
        if (Util.usingHuggingFace()) {
            return;
        }

        client.sendMessage("what A, E, I, O, R, W character stands for in the log entries finding in the messages.log file?\n");
        countDown.await(120, TimeUnit.SECONDS);
    }

    @Test
    public void testSecurity() throws Exception {
        if (Util.usingHuggingFace()) {
            return;
        }

        client.sendMessage("I got an java.lang.InternalError exception when tried to log in via SAML.\n");
        countDown.await(120, TimeUnit.SECONDS);
    }


    public static void verify(String message) {
        assertNotNull(message);

        assertTrue((message.toLowerCase().contains("audit") || message.toLowerCase().contains("error") || 
        message.toLowerCase().contains("information") || message.toLowerCase().contains("system.out") || 
        message.toLowerCase().contains("system.err") || message.toLowerCase().contains("warning")) || 
        (message.toLowerCase().contains("securerandom.source") || message.toLowerCase().contains("file:/dev/urandom") || message.contains("java.security")), message);
        countDown.countDown();
    }

}
