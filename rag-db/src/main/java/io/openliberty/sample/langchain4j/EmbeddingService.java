/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.sample.langchain4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import io.openliberty.sample.langchain4j.mongo.AtlasMongoDB;
import io.openliberty.sample.langchain4j.util.ModelBuilder;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/embedding")
@ApplicationScoped
public class EmbeddingService {

    private static final String[] MD_FILES = {
        "jakartaEE.md", "microProfileConfig.md"
    };

    @Inject
    private ModelBuilder modelBuilder;

    @Inject 
    private AtlasMongoDB mongoDB;

    private List<Float> toFloat(float[] embedding){
        List<Float> vector = new ArrayList<>();
        for (float elem : embedding) {
            vector.add(elem);
        }
        return vector;
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin" })
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Successfully added embedding."),
        @APIResponse(responseCode = "400", description = "Invalid embedding configuration.")})
    @Parameters(value = {
        @Parameter(
            name = "summary", in = ParameterIn.QUERY,
            description = "The summary of the embedding",
            required = true,
            schema = @Schema(type = SchemaType.STRING)),
        @Parameter(
            name = "content", in = ParameterIn.QUERY,
            description = "The content of the embedding",
            required = true,
            schema = @Schema(type = SchemaType.STRING))})
    @Operation(summary = "Add a new embedding to the database.")
    public Response add(
        @QueryParam("summary") String summary,
        @QueryParam("content") String content) {

        Document newEmbedding = new Document();
        newEmbedding.put("Summary", summary);
        newEmbedding.put("Content", content);
        newEmbedding.put("Vector",
            toFloat(modelBuilder.getEmbeddingModel().embed(content).content().vector()));
        mongoDB.insertOne(newEmbedding);

        return Response
            .status(Response.Status.OK)
            .entity(newEmbedding.toJson())
            .build();
    }

    @POST
    @Path("/init")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({ "admin" })
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Successfully added knowledge base."),
        @APIResponse(responseCode = "400", description = "Invalid embedding configuration.")})
    @Operation(summary = "Add the knowledge base embeddings to the database.")
    public Response initializeDatabase() {
        if (mongoDB.isEmpty()) {
            mongoDB.createIndex();
            try {
                ClassLoader classLoader = EmbeddingService.class.getClassLoader();
                for (String txtFile : MD_FILES) {
                    InputStream inStream = classLoader.getResourceAsStream("knowledge_base/" + txtFile);
                    if (inStream != null) {
                        InputStreamReader reader = new InputStreamReader(inStream, StandardCharsets.UTF_8);
                        BufferedReader br = new BufferedReader(reader);
                        String summary = br.readLine();
                        StringBuffer content = new StringBuffer();
                        String line;
                        while ((line = br.readLine()) != null) {
                            content.append(line).append("\n");
                        }
                        br.close();
                        reader.close();
                        inStream.close();
                        Document newEmbedding = new Document();
                        newEmbedding.put("Summary", summary);
                        newEmbedding.put("Content", content.toString());
                        newEmbedding.put("Vector",
                                toFloat(modelBuilder.getEmbeddingModel().embed(summary).content().vector()));
                        mongoDB.insertOne(newEmbedding);
                    }
                }
                return Response.status(Status.OK)
                               .entity("Successfully loaded knowledge base into MongoDB.")
                               .build();
            } catch (Exception exception) {
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                               .entity("Could not load knowledge base into MongoDB.")
                               .build();
            }

        }

        return Response
            .status(Response.Status.OK)
            .entity("Knowledge base already initialized.")
            .build();

    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin", "user" })
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Successfully listed the embeddings."),
        @APIResponse(responseCode = "500", description = "Failed to list the embeddings.") })
    @Operation(summary = "List the embeddings from the database.")
    public Response retrieve() {
        
        StringWriter sb = new StringWriter();
        try {
            sb.append("[");
            boolean first = true;
            FindIterable<Document> docs = mongoDB.getCollection().find();
            for (Document d : docs) {
                if (!first) {
                    sb.append(",");
                } else {
                    first = false;
                }
                sb.append(d.toJson());
            }
            sb.append("]");
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("[\"Unable to list embeddings: " + e.getMessage() + "\"]")
                           .build();
        }
        return Response.status(Response.Status.OK)
                       .entity(sb.toString())
                       .build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin" })
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Successfully updated embeddings."),
        @APIResponse(responseCode = "400", description = "Invalid object id or embeddings configuration."),
        @APIResponse(responseCode = "404", description = "Embeddings object id was not found.") })
    @Parameters(value = {
        @Parameter(
            name = "id", in = ParameterIn.PATH,
            description = "The object id of the embedding",
            required = true, example = "6880f9eef887c128f1ed0bf1",
            schema = @Schema(type = SchemaType.STRING)),
        @Parameter(
            name = "summary", in = ParameterIn.QUERY,
            description = "The summary of the embedding",
            required = true,
            schema = @Schema(type = SchemaType.STRING)),
        @Parameter(
            name = "content", in = ParameterIn.QUERY,
            description = "The content of the embedding",
            required = true,
            schema = @Schema(type = SchemaType.STRING))})
    @Operation(summary = "Update an embedding in the database.")
    public Response update(
        @PathParam("id") String id,
        @QueryParam("summary") String summary,
        @QueryParam("content") String content) {

        ObjectId oid;
        try {
            oid = new ObjectId(id);
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("[\"Invalid object id!\"]")
                           .build();
        }

        Document query = new Document("_id", oid);
        Document newEmbedding = new Document();
        newEmbedding.put("Summary", summary);
        newEmbedding.put("Content", content);
        newEmbedding.put("Vector", toFloat(
            modelBuilder.getEmbeddingModel().embed(summary).content().vector()));
        UpdateResult updateResult = mongoDB.replaceOne(query, newEmbedding);
        if (updateResult.getMatchedCount() == 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("[\"_id was not found!\"]")
                           .build();
        }

        newEmbedding.put("_id", oid);
        return Response.status(Response.Status.OK)
                       .entity(newEmbedding.toJson())
                       .build();
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin" })
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Successfully deleted the embedding."),
        @APIResponse(responseCode = "400", description = "Invalid object id."),
        @APIResponse(responseCode = "404", description = "Embedding object id was not found.") })
    @Parameter(description = "Object id of the embedding to delete.", required = true)
    @Operation(summary = "Delete an embedding from the database.")
    public Response remove(@PathParam("id") String id) {

        ObjectId oid;
        try {
            oid = new ObjectId(id);
        } catch (Exception e) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity("[\"Invalid object id!\"]")
                .build();
        }

        Document query = new Document("_id", oid);
        DeleteResult deleteResult = mongoDB.deleteOne(query);
        if (deleteResult.getDeletedCount() == 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("[\"_id was not found!\"]")
                           .build();
        }

        return Response.status(Response.Status.OK)
                       .entity(query.toJson())
                       .build();
    }

}