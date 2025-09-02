/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package it.io.openliberty.sample.langchain4j;

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
        URI uri = new URI("ws://localhost:9080/streamingchat");
        StreamingChatClient client = new StreamingChatClient(uri);
        future = new CompletableFuture<>();
        builder = new StringBuilder();
        client.sendMessage("What are large language models?");
        String message;
        try {
            message = future.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            message = builder.toString();
        }
        client.close();
        assertNotNull(message);
        assertTrue(message.contains("artificial intelligence"), message);
    }

    public static void verify(String message) {
        if (message.equals("")) {
            future.complete(builder.toString());
        }
        builder.append(message);
    }

}
