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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Paths;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
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

@Path("/embedding")
@ApplicationScoped
public class EmbeddingService {

    private EmbeddingModel embModel = new AllMiniLmL6V2EmbeddingModel();

    @Inject
    private MongoDatabase db;

    private byte[] toBytes(float[] vector) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(bos)) {
            for (float f : vector) {
                dos.writeFloat(f);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bos.toByteArray();
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

        MongoCollection<Document> embeddingStore = db.getCollection("EmbeddingsStored");

        Document newEmbedding = new Document();
        newEmbedding.put("Summary", summary);
        newEmbedding.put("Content", content);
        newEmbedding.put("Vector", toBytes(embModel.embed(summary).content().vector()));

        embeddingStore.insertOne(newEmbedding);

        return Response
            .status(Response.Status.OK)
            .entity(newEmbedding.toJson())
            .build();
    }

    @POST
    @Path("/init")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin" })
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Successfully added knowledge base."),
        @APIResponse(responseCode = "400", description = "Invalid embedding configuration.")})
    @Operation(summary = "Add the knowledge base embeddings to the database.")
    public Response initializeDatabase() {

        if (!contentAlreadyStored()){

            try{

                URL urlResourcePath = getClass().getClassLoader().getResource("knowledge_base/");

                String resourcePath = Paths.get(urlResourcePath.toURI()).toString();
                
                List<dev.langchain4j.data.document.Document> documents = FileSystemDocumentLoader.loadDocuments(resourcePath, new ApacheTikaDocumentParser());

                var docSplitter = DocumentSplitters.recursive(2500, 50);

                List<TextSegment> textSeg = docSplitter.splitAll(documents);

                List<Response> embeddingResponses = new ArrayList<>();

                for (TextSegment content : textSeg){
                    embeddingResponses.add(add(content.toString(),content.toString()));
                }

                return Response
                    .status(Response.Status.OK)
                    .entity(new ObjectMapper().writeValueAsString(embeddingResponses))
                    .build();

            }catch(Exception exception){
            
                return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("[\"Could not load knowledge base into MongoDB.\"]")
                .build();

            }
        }

        return Response
            .status(Response.Status.OK)
            .entity("[\"Knowledge base already initialized.\"]")
            .build();

    }

    
    public boolean contentAlreadyStored(){
        MongoCollection<org.bson.Document> embeddingStore = db.getCollection("EmbeddingsStored");
        return embeddingStore.countDocuments() > 0 ? true : false;
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
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity("[\"Invalid object id!\"]")
                .build();
        }

        MongoCollection<Document> embeddingStore = db.getCollection("EmbeddingsStored");
        Document query = new Document("_id", oid);
        Document newEmbedding = new Document();
        newEmbedding.put("Summary", summary);
        newEmbedding.put("Content", content);
        newEmbedding.put("Vector", toBytes(embModel.embed(summary).content().vector()));

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