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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EmbeddingServiceIT {

    private static final String EMBEDDING_API_URL = "http://localhost:9081/api/embedding";
    private static final String AUTHORIZATION = "Basic " + Base64.getEncoder().encodeToString("bob:bobpwd".getBytes(StandardCharsets.UTF_8));

    private static Client client;
    private static HashMap<String, String> testData = new HashMap<>();
    private static ArrayList<String> testIDs = new ArrayList<>(2);

    @BeforeAll
    public static void setup() {
        client = ClientBuilder.newClient();
        testData.put("Open Liberty docs, blogs, and guides " +
                     "will help in developing cloud-native applications.", "See https://openliberty.io/");
        testData.put( "Sample content summary","Test content");
    }

    @AfterAll
    public static void teardown() {
        client.close();
    }

    @Test
    @Order(1)
    public void testAddEmbeddings() {

        System.out.println("=== Testing: Adding " + testData.size() +
                           " embeddings to the database");

        for (Map.Entry<String, String> testDataElem : testData.entrySet()) {
            Response response = client.target(EMBEDDING_API_URL)
                                      .queryParam("summary", testDataElem.getKey())
                                      .queryParam("content", testDataElem.getValue())
                                      .request()
                                      .header("Authorization", AUTHORIZATION)
                                      .post(null);
            String id = "";
            String responseRes = response.readEntity(String.class);
            try (JsonReader reader = Json.createReader(new StringReader(responseRes))) {
                JsonObject responseObj = reader.readObject();
                JsonObject idObject = responseObj.getJsonObject("_id");
                id = idObject.getString("$oid");
            } catch (Exception e) {
                System.out.println(responseRes);
                throw e;
            }
            assertResponse(EMBEDDING_API_URL, response);
            testIDs.add(id);
            response.close();
        }

        System.out.println("    Done");

    }

    @Test
    @Order(2)
    public void testUpdateEmbeddings() {

        System.out.println("=== Testing: Updating embedding with id " + testIDs.get(0));

        Response response = client.target(EMBEDDING_API_URL + "/")
                                  .path(testIDs.get(0))
                                  .queryParam("summary", "Summary Update Version 2")
                                  .queryParam("content", "Content Update Version 2")
                                  .request()
                                  .header("Authorization", AUTHORIZATION)
                                  .put(null);
                
        assertResponse(EMBEDDING_API_URL + "/" + testIDs.get(0), response);

        System.out.println("    Done");
    }

    @Test
    @Order(3)
    public void testGetEmbeddings() {

        System.out.println("=== Testing: Get embeddings from the database");

        Response response = client.target(EMBEDDING_API_URL)
                                  .request().header("Authorization", AUTHORIZATION)
                                  .get();

        this.assertResponse(EMBEDDING_API_URL, response);
        String responseText = response.readEntity(String.class);
        JsonReader reader = Json.createReader(new StringReader(responseText));
        JsonArray embeddings = reader.readArray();
        reader.close();

        int testDataCount = 0;
        for (JsonValue value : embeddings) {
            JsonObject embedding = (JsonObject) value;
            String id = embedding.getJsonObject("_id").getString("$oid");
            if (testIDs.contains(id)) {
                testDataCount++;
            }
        }

        assertEquals(testIDs.size(), testDataCount, "Incorrect number of embeddings.");

        System.out.println("    There are " + testDataCount + " embeddings.");
        System.out.println("    Done");

        response.close();

    }

    @Test
    @Order(4)
    public void testDeleteEmbeddings() {
        System.out.println("=== Testing: Removing " + testIDs.size() + 
                           " embeddings from the database");
        for (String id : testIDs) {
            String url = EMBEDDING_API_URL + "/" + id;
            Response response = client.target(url)
                                      .request()
                                      .header("Authorization", AUTHORIZATION)
                                      .delete();
            assertResponse(url, response);
            response.close();
        }
        System.out.println("    Done");
    }

    private void assertResponse(String url, Response response) {
        assertEquals(200, response.getStatus(), "Incorrect response code from " + url);
    }

}