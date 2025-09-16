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

    private static Client client;

    private static HashMap<String, String> testData = new HashMap<>();

    private static String rootURL;
    private static ArrayList<String> testIDs = new ArrayList<>(2);

    @BeforeAll
    public static void setup() {

        client = ClientBuilder.newClient();
        rootURL = "http://localhost:9081/api/embedding";
        testData.put("= Differences between MicroProfile 7.1 and 7.0\n" +
                        "MicroProfile 7.1 is a minor release. It includes minor changes for the MicroProfile OpenAPI 4.1 and Telemetry 2.1 features.\n" + 
                        "If you are updating your application from using MicroProfile 7.0 features to using link:https: github.com/eclipse/microprofile/releases/tag/7.1[MicroProfile 7.1] features, changes in API behavior might require you to update your application code. The following sections provide details about migrating your applications from MicroProfile 7.0 to 7.1:\n" + 
                        "- <<#openapi, Differences between MicroProfile OpenAPI 4.1 and 4.0>>\n" +  
                        "- <<#telemetry, Differences between MicroProfile Telemetry 2.1 and 1.1>>\n", "  Copyright (c) 2024 IBM Corporation and others.\n" +
                        " // Licensed under Creative Commons Attribution-NoDerivatives\n" +
                        " // 4.0 International (CC BY-ND 4.0)\n" + 
                        " // https://creativecommons.org/licenses/by-nd/4.0/\n" + 
                        " // Contributors:\n" +  
                        " // IBM Corporation\n" +  
                        ":page-description: MicroProfile 7.1 is a minor release. If you are updating your application from using MicroProfile 7.0 features to using MicroProfile 7.1 features, changes in API behavior might require you to update your application code.\n" +  
                        ":projectName: Open Liberty\n" +  
                        ":page-layout: general-reference\n" +  
                        ":page-type: general\n" +  
                        "= Differences between MicroProfile 7.1 and 7.0\n" +  
                        "MicroProfile 7.1 is a minor release. It includes minor changes for the MicroProfile OpenAPI 4.1 and Telemetry 2.1 features.\n" +  
                        "If you are updating your application from using MicroProfile 7.0 features to using link:https: github.com/eclipse/microprofile/releases/tag/7.1[MicroProfile 7.1] features, changes in API behavior might require you to update your application code. The following sections provide details about migrating your applications from MicroProfile 7.0 to 7.1:\n" +  
                        "- <<#openapi, Differences between MicroProfile OpenAPI 4.1 and 4.0>>\n" +  
                        "- <<#telemetry, Differences between MicroProfile Telemetry 2.1 and 1.1>>\n" +  
                        "[#openapi]\n" +  
                        "== Differences between MicroProfile OpenAPI 4.1 and 4.0\n" +  
                        "feature:mpOpenAPI-4.1[display=MicroProfile OpenAPI 4.1] adds the `getJsonSchemaDialect()` method and the `setJsonSchemaDialect()` method to set the default `jsonSchemaDialect` field by using the model API. \n" +  
                        "MicroProfile OpenAPI 4.1 also adds the `getExtension()` method and the `hasExtension()` method to the `Extensible` model class. \n" +  
                        "[#telemetry]\n" +  
                        "== Differences between MicroProfile Telemetry 2.1 and 1.1\n" +  
                        "feature:mpTelemetry-2.1[display=MicroProfile Telemetry 2.1] equips you with the most recent Open Telemetry technology. This feature now uses OpenTelemetry-1.48.0, upgraded from 1.39.0. \n" +  
                        "You no longer have to add the following configuration to the `server.xml` to use the OpenTelemetry API:\n" +  
                        "----\n" +  
                        "<webApplication id=\"app-name\" location=\"app-name.war\">\n" +  
                        "    <classloader apiTypeVisibility=\"+third-party\"/>\n" +  
                        "</webApplication>\n" +  
                        "----");
        testData.put( "Sample content summary","Test content");
    }

    @AfterAll
    public static void teardown() {
        client.close();
    }

    @Test
    @Order(1)
    public void testAddEmbeddings() {

        System.out.println("   === Testing: Adding " + testData.size()
                + " embeddings to the database. ===");

        for (Map.Entry<String, String> testDataElem : testData.entrySet()) {

            String url = rootURL;

            Response response = client.target(url).queryParam("summary",testDataElem.getKey()).queryParam("content",testDataElem.getValue()).request().header("Authorization","Basic " 
            + Base64.getEncoder().encodeToString("bob:bobpwd".getBytes(StandardCharsets.UTF_8))).post(null);
            String id = "";
            String responseRes = response.readEntity(String.class);

            try (JsonReader reader = Json.createReader(new StringReader(responseRes))) {
                JsonObject responseObj = reader.readObject();
                JsonObject idObject = responseObj.getJsonObject("_id");
                id = idObject.getString("$oid");
            }

            this.assertResponse(url, response);

            testIDs.add(id);

            response.close();
        }
        System.out.println("      === Done. ===");
    }

    @Test
    @Order(2)
    public void testUpdateEmbeddings() {

        System.out.println("   === Testing: Updating embedding with id " + testIDs.get(0)
                + ". ===");

        String url = rootURL + "/";

        Response response = client.target(url).path(testIDs.get(0)).queryParam("summary","Summary Update Version 2").queryParam("content","Content Update Version 2").request()
                .header("Authorization","Basic " + Base64.getEncoder().encodeToString("bob:bobpwd".getBytes(StandardCharsets.UTF_8))).put(null);
                
        this.assertResponse(rootURL + "/" + testIDs.get(0), response);

        System.out.println("      === Done. ===");
    }

    @Test
    @Order(3)
    public void testGetEmbeddings() {

        System.out.println("   === Testing: Get embeddings from the database. ===");

        String url = rootURL;

        Response response = client.target(url).request().header("Authorization","Basic " + Base64.getEncoder().encodeToString("bob:bobpwd".getBytes(StandardCharsets.UTF_8))).get();

        this.assertResponse(url, response);
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

        assertEquals(testIDs.size(), testDataCount,
                "Incorrect number of embeddings.");

        System.out.println("      === Done. There are " + testDataCount
                + " embeddings. ===");

        response.close();
    }

    @Test
    @Order(4)
    public void testDeleteEmbeddings() {
        System.out.println("   === Testing: Removing " + testIDs.size()
                + " embeddings from the database. ===");

        for (String id : testIDs) {
            String url = rootURL + "/" + id;
            Response response = client.target(url).request().header("Authorization","Basic " + Base64.getEncoder().encodeToString("bob:bobpwd".getBytes(StandardCharsets.UTF_8))).delete();
            this.assertResponse(url, response);
            response.close();
        }

        System.out.println("      === Done. ===");
    }

    private void assertResponse(String url, Response response) {
        assertEquals(200, response.getStatus(), "Incorrect response code from " + url);
    }
}