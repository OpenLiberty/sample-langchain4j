package io.openliberty.sample.langchain4j;

import java.util.Set;

import java.io.StringWriter;

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
// tag::bsonDocument[]
import org.bson.Document;
// end::bsonDocument[]
import org.bson.types.ObjectId;

// tag::mongoImports1[]
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
// end::mongoImports1[]
// tag::mongoImports2[]
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
// end::mongoImports2[]

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

@Path("/embedding")
@ApplicationScoped
public class EmbeddingService {

    // tag::dbInjection[]
    @Inject
    MongoDatabase db;
    // end::dbInjection[]

    // tag::beanValidator[]
    @Inject
    Validator validator;
    // end::beanValidator[]

    // tag::getViolations[]
    private JsonArray getViolations(Embedding embedding) {
        Set<ConstraintViolation<Embedding>> violations = validator.validate(
                embedding);

        JsonArrayBuilder messages = Json.createArrayBuilder();

        for (ConstraintViolation<Embedding> v : violations) {
            messages.add(v.getMessage());
        }

        return messages.build();
    }
    // end::getViolations[]

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Successfully added embedding."),
            @APIResponse(responseCode = "400", description = "Invalid embedding configuration.") })
    @Operation(summary = "Add a new embedding to the database.")
    // tag::add[]
    public Response add(Embedding embedding) {
        JsonArray violations = getViolations(embedding);

        if (!violations.isEmpty()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(violations.toString())
                    .build();
        }

        // tag::getCollection[]
        MongoCollection<Document> embeddingStore = db.getCollection("EmbeddingsStored");
        // end::getCollection[]

        // tag::crewMemberCreation[]
        Document newEmbedding = new Document();
        newEmbedding.put("EmbeddingID", embedding.getEmbeddingID());
        newEmbedding.put("Tags", embedding.getTags());
        newEmbedding.put("Content", embedding.getContent());
        newEmbedding.put("Embedding", embedding.getEmbedding());
        // end::crewMemberCreation[]

        // tag::insertOne[]
        embeddingStore.insertOne(newEmbedding);
        // end::insertOne[]

        return Response
                .status(Response.Status.OK)
                .entity(newEmbedding.toJson())
                .build();
    }
    // end::add[]

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Successfully listed the embeddings."),
            @APIResponse(responseCode = "500", description = "Failed to list the embeddings.") })
    @Operation(summary = "List the embeddings from the database.")
    // tag::retrieve[]
    public Response retrieve() {
        StringWriter sb = new StringWriter();

        try {
            // tag::getCollectionRead[]
            MongoCollection<Document> embeddingStore = db.getCollection("EmbeddingsStored");
            // end::getCollectionRead[]
            sb.append("[");
            boolean first = true;
            // tag::find[]
            FindIterable<Document> docs = embeddingStore.find();
            // end::find[]
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
    // end::retrieve[]

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Successfully updated embeddings."),
            @APIResponse(responseCode = "400", description = "Invalid object id or embeddings configuration."),
            @APIResponse(responseCode = "404", description = "Embeddings object id was not found.") })
    @Operation(summary = "Update an embedding in the database.")
    // tag::update[]
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

        // tag::getCollectionUpdate[]
        MongoCollection<Document> embeddingStore = db.getCollection("EmbeddingsStored");
        // end::getCollectionUpdate[]

        // tag::queryUpdate[]
        Document query = new Document("_id", oid);
        // end::queryUpdate[]

        // tag::crewMemberUpdate[]

        Document newEmbedding = new Document();
        newEmbedding.put("EmbeddingID", embedding.getEmbeddingID());
        newEmbedding.put("Tags", embedding.getTags());
        newEmbedding.put("Content", embedding.getContent());
        newEmbedding.put("Embedding", embedding.getEmbedding());
        // end::crewMemberUpdate[]

        // tag::replaceOne[]
        UpdateResult updateResult = embeddingStore.replaceOne(query, newEmbedding);
        // end::replaceOne[]

        // tag::getMatchedCount[]
        if (updateResult.getMatchedCount() == 0) {
            // end::getMatchedCount[]
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
    // end::update[]

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Successfully deleted the embedding."),
            @APIResponse(responseCode = "400", description = "Invalid object id."),
            @APIResponse(responseCode = "404", description = "Embedding object id was not found.") })
    @Operation(summary = "Delete an embedding from the database.")
    // tag::remove[]
    public Response remove(
            @Parameter(description = "EmbeddingId of the embedding to delete.", required = true) @PathParam("id") String id) {

        ObjectId oid;

        try {
            oid = new ObjectId(id);
        } catch (Exception e) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("[\"Invalid object id!\"]")
                    .build();
        }

        // tag::getCollectionDelete[]
        MongoCollection<Document> embeddingStore = db.getCollection("EmbeddingsStored");
        // end::getCollectionDelete[]

        // tag::queryDelete[]
        Document query = new Document("_id", oid);
        // end::queryDelete[]

        // tag::deleteOne[]
        DeleteResult deleteResult = embeddingStore.deleteOne(query);
        // end::deleteOne[]

        // tag::getDeletedCount[]
        if (deleteResult.getDeletedCount() == 0) {
            // end::getDeletedCount[]
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
    // end::remove[]
}