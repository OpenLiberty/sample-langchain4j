/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package it.io.openliberty.sample.langchain4j.client;

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
public class McpClientServiceIT {

    private static CountDownLatch countDown;

    private static McpWebsocketClient client;
    
    @BeforeEach
    public void setup() throws Exception {
        countDown = new CountDownLatch(1);
        URI uri = new URI("ws://localhost:9080/mcpchat");
        client = new McpWebsocketClient(uri);
    }

    @AfterEach
    public void teardown() throws Exception {
      client.close();
    }

    @Test
    public void testJakartaEE() throws Exception {
        client.sendMessage("What are some current problems users have with JakartaEE?");
        countDown.await(120, TimeUnit.SECONDS);
    }

    @Test
    public void testMicroProfile() throws Exception {
        client.sendMessage("What are some current problems users have with MicroProfile?");
        countDown.await(120, TimeUnit.SECONDS);
    }

    @Test
    public void testLangChain4j() throws Exception {
        client.sendMessage("What are some current problems users have with LangChain4J?");
        countDown.await(120, TimeUnit.SECONDS);
    }

    public static void verify(String message) {
        assertNotNull(message);
        String text = message.toLowerCase();
        assertTrue(
            text.contains("stackoverflow.com/questions/") ||
            !text.contains("my failure reason is:"),
            message
        );
        countDown.countDown();
    }
}
