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

import java.util.Set;

import java.io.StringWriter;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.Json;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;

import com.mongodb.client.FindIterable;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;


import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;
import java.util.ArrayList;

@Path("/embedding")
@ApplicationScoped
public class EmbeddingService {

    @Inject
    MongoDatabase db;

    @Inject
    Validator validator;

    private JsonArray getViolations(Embedding embedding) {
        Set<ConstraintViolation<Embedding>> violations = validator.validate(
                embedding);

        JsonArrayBuilder messages = Json.createArrayBuilder();

        for (ConstraintViolation<Embedding> v : violations) {
            messages.add(v.getMessage());
        }

        return messages.build();
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin" })
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Successfully added embedding."),
            @APIResponse(responseCode = "400", description = "Invalid embedding configuration.") })
    @Operation(summary = "Add a new embedding to the database.")
    
    public Response add(Embedding embedding) {
        JsonArray violations = getViolations(embedding);

        if (!violations.isEmpty()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(violations.toString())
                    .build();
        }

        MongoCollection<Document> embeddingStore = db.getCollection("EmbeddingsStored");

        Document newEmbedding = new Document();
        newEmbedding.put("EmbeddingID", embedding.getEmbeddingID());
        newEmbedding.put("Tags", embedding.getTags());
        newEmbedding.put("Content", embedding.getContent());
        newEmbedding.put("Summary", embedding.getSummary());

        // whenever new content is stored, embedding is created and set
        // the template for the embedding is the embedding model
        EmbeddingModel embModel = new AllMiniLmL6V2EmbeddingModel();
        // text segments are made from the content
        TextSegment textSeg = TextSegment.from(embedding.getContent());
        // langchain4j is used to create its langchain4j Embedding object
        dev.langchain4j.data.embedding.Embedding contentEmb = embModel.embed(textSeg).content();
        // the vector is stored as a List<Doubles> in the database since float[] was not
        // supported. but the .vector returns a float[].
        float[] embVectorFloat = contentEmb.vector();
        // convert to List<Doubles>
        List<Double> embVector = new ArrayList<>(embVectorFloat.length);
        for (float num : embVectorFloat) {
            embVector.add((double) num);
        }
        // In the embedding object from Embedding.java class, set the variable
        embedding.setEmbedding(embVector);

        newEmbedding.put("Embedding", embedding.getEmbedding());

        embeddingStore.insertOne(newEmbedding);

        return Response
                .status(Response.Status.OK)
                .entity(newEmbedding.toJson())
                .build();
    }
    
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin", "user" })
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Successfully listed all embeddings."),
            @APIResponse(responseCode = "500", description = "Failed to list embeddings.") })
    @Operation(summary = "List all the embeddings from the database.")
    
    public Response retrieveAllEmbeddings() {
        StringWriter sb = new StringWriter();

        try {
            MongoCollection<Document> embeddingStore = db.getCollection("EmbeddingsStored");
            sb.append("[");
            boolean first = true;
            Bson projection = Projections.fields(Projections.include("EmbeddingID", "Embedding"));
            FindIterable<Document> docs = embeddingStore.find().projection(projection);
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
            e.printStackTrace(System.out);
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("[\"Unable to list embeddings!\"]")
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(sb.toString())
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
            MongoCollection<Document> embeddingStore = db.getCollection("EmbeddingsStored");
            sb.append("[");
            boolean first = true;
            FindIterable<Document> docs = embeddingStore.find();
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
            e.printStackTrace(System.out);
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("[\"Unable to list embeddings!\"]")
                    .build();
        }

        return Response
                .status(Response.Status.OK)
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
    @Operation(summary = "Update an embedding in the database.")
    
    public Response update(Embedding embedding,
            @Parameter(description = "Object id of the embedding to update.", required = true) @PathParam("id") String id) {

        JsonArray violations = getViolations(embedding);

        if (!violations.isEmpty()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(violations.toString())
                    .build();
        }

        ObjectId oid;

        try {
            oid = new ObjectId(id);
        } catch (Exception e) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("[\"Invalid object id!\"]")
                    .build();
        }

        
        MongoCollection<Document> embeddingStore = db.getCollection("EmbeddingsStored");
        

        
        Document query = new Document("_id", oid);
        

        Document newEmbedding = new Document();
        newEmbedding.put("EmbeddingID", embedding.getEmbeddingID());
        newEmbedding.put("Tags", embedding.getTags());
        newEmbedding.put("Content", embedding.getContent());
        newEmbedding.put("Summary", embedding.getSummary());
        // embedding vector itself should not be updated by user, instead if content
        // changes,
        // a new embedding should be made.

        EmbeddingModel embModel = new AllMiniLmL6V2EmbeddingModel();
        TextSegment textSeg = TextSegment.from(embedding.getContent());
        dev.langchain4j.data.embedding.Embedding contentEmb = embModel.embed(textSeg).content();
        float[] embVectorFloat = contentEmb.vector();
        List<Double> embVector = new ArrayList<>(embVectorFloat.length);
        for (float num : embVectorFloat) {
            embVector.add((double) num);
        }
        // In the embedding object from Embedding.java class, set the variable
        embedding.setEmbedding(embVector);

        newEmbedding.put("Embedding", embedding.getEmbedding());

        UpdateResult updateResult = embeddingStore.replaceOne(query, newEmbedding);

        if (updateResult.getMatchedCount() == 0) {

            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity("[\"_id was not found!\"]")
                    .build();
        }

        newEmbedding.put("_id", oid);

        return Response
                .status(Response.Status.OK)
                .entity(newEmbedding.toJson())
                .build();
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Successfully deleted the embedding."),
            @APIResponse(responseCode = "400", description = "Invalid object id."),
            @APIResponse(responseCode = "404", description = "Embedding object id was not found.") })
    @RolesAllowed({ "admin" })
    @Operation(summary = "Delete an embedding from the database.")
    
    public Response remove(
            @Parameter(description = "Object id of the embedding to delete.", required = true) @PathParam("id") String id) {

        ObjectId oid;

        try {
            oid = new ObjectId(id);
        } catch (Exception e) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("[\"Invalid object id!\"]")
                    .build();
        }

        MongoCollection<Document> embeddingStore = db.getCollection("EmbeddingsStored");

        Document query = new Document("_id", oid);

        DeleteResult deleteResult = embeddingStore.deleteOne(query);

        if (deleteResult.getDeletedCount() == 0) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity("[\"_id was not found!\"]")
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(query.toJson())
                .build();
    }
}