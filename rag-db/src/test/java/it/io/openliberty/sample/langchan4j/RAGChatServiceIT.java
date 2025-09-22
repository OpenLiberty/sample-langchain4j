/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class RAGChatServiceIT {

    private static final String ADMIN_USERNAME = "bob";
    private static final String ADMIN_PASSWD = "bobpwd";
    private static final String INIT_DB_URL = "http://localhost:9081/api/embedding/init";

    private static CountDownLatch countDown;
    private static RAGChatClient client;

    @BeforeAll
    public static void initializeDatabase() throws Exception {

        URL url = new URI(INIT_DB_URL).toURL();
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        String login = ADMIN_USERNAME + ":" + ADMIN_PASSWD;
        String encodedCredentials = Base64.getEncoder().encodeToString((login).getBytes());
        String value = "Basic " + encodedCredentials;
        httpConnection.setRequestProperty("Authorization", value);
        httpConnection.setRequestMethod("POST");
        httpConnection.setRequestProperty("Accept", "*/*");
        httpConnection.setDoOutput(false);
        BufferedReader bufferReader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));

        String line;
        StringBuilder res = new StringBuilder();
        while ((line = bufferReader.readLine()) != null) {
            res.append(line);
        }

        System.out.println(res.toString());
        bufferReader.close();
        httpConnection.disconnect();

    }

    @BeforeEach
    public void setup() throws Exception {
        countDown = new CountDownLatch(1);
        URI uri = new URI("ws://localhost:9081/chat");
        client = new RAGChatClient(uri);
    }

    @AfterEach
    public void teardown() throws Exception {
      client.close();
    }

    @Test
    public void testLogs() throws Exception {
        client.sendMessage("How do I isolate applications on the same server?\n");
        countDown.await(120, TimeUnit.SECONDS);
    }

    @Test
    public void testSecurity() throws Exception {
        client.sendMessage("What OpenTelemetry properties enabled by MicroProfile Telemetry do I need to set the exporter that is used to collect traces?");
        countDown.await(120, TimeUnit.SECONDS);
    }

    public static void verify(String message) throws Exception {
        assertNotNull(message);
        assertTrue((message.toLowerCase().contains("virtualHost") ||
                    message.toLowerCase().contains("hostAlias") ||
                    message.contains("<hostAlias>localhost:9081</hostAlias>") ||
                    message.toLowerCase().contains("myApp.ear") ||
                    message.toLowerCase().contains("myApp2.war") ||
                    message.toLowerCase().contains("webApplication") ||
                    message.toLowerCase().contains("server.xml") ||
                    message.toLowerCase().contains("jaeger") ||
                    message.toLowerCase().contains("logging")) ||
                   (message.toLowerCase().contains("otel.traces.exporter") ||
                    message.toLowerCase().contains("zipkin")), message);
        countDown.countDown();
    }

}
