package io.openliberty.sample.langchain4j.mongo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.SearchIndexModel;
import com.mongodb.client.model.SearchIndexType;

import io.openliberty.sample.langchain4j.util.ModelBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.bson.Document;
import org.json.JSONObject;
import org.bson.conversions.Bson;

@ApplicationScoped
public class AtlasMongoDB {

    private final String COLLECTION_NAME = "EmbeddingsStored";
    private final String SEARCH_INDEX_NAME = "vector_index";
    private final String PATH = "Vector";
    private final int MAX_RESULTS_TO_AI = 1;
    private final int NUM_CANDIDATES = 2;

    @Inject
    private ModelBuilder modelBuilder;

    @Inject
    MongoDatabase db;
    
    public void getContentStored() {
        MongoCollection<Document> embeddingStore = db.getCollection(COLLECTION_NAME);
        
        FindIterable<Document> docs = embeddingStore.find();
        
        int num = 0;
        for (Document d : docs) {
            
            num += 1;
            System.out.println("\n\nDocument:" + num + "\n");
            System.out.println(d.toJson());
        
        }

    }

    public void createIndex() {

        db.createCollection(COLLECTION_NAME);

        MongoCollection<Document> embeddingStore = db.getCollection(COLLECTION_NAME);

        Bson documentDefinition = new Document(
                "fields",
                Collections.singletonList(
                        new Document("type", "vector")
                                .append("path", PATH)
                                .append("numDimensions", modelBuilder.getEmbeddingModel().dimension())
                                .append("similarity", "cosine")));

        SearchIndexType vectorSearch = SearchIndexType.vectorSearch();

        List<SearchIndexModel> searchIndexModels = Collections.singletonList(
                new SearchIndexModel(SEARCH_INDEX_NAME, documentDefinition, vectorSearch));

        try {
            Thread.sleep(5000);
            embeddingStore.createSearchIndexes(searchIndexModels);
            boolean indexReady = false;
            while (!indexReady) {

                for (Document index : embeddingStore.listSearchIndexes()) {
                    JSONObject json = new JSONObject(index.toJson());
                    Boolean queryable = json.getBoolean("queryable");

                    if (queryable.equals(true) && json.getString("name").equals(SEARCH_INDEX_NAME)) {
                        indexReady = true;
                        break;
                    } else {
                        Thread.sleep(200);
                    }

                }

            }
            
        } catch (Exception exception) {
            System.out.println("[ERROR] Could not create search index required for vector search. Message: " + exception.getMessage());
        }
    }

    public float[] convertUserQueryToEmbedding(String userQuery) {
        return modelBuilder.getEmbeddingModel().embed(userQuery).content().vector();
    }

    public List<String> retrieveContent(List<Float> userQuery, String query) {
        
        MongoCollection<Document> embeddingStore = db.getCollection(COLLECTION_NAME);

        List<String> similarContent = new ArrayList<>();
        
        Document vecSearchDocument = new Document("queryVector", userQuery)
                .append("path", PATH)
                .append("numCandidates", NUM_CANDIDATES)
                .append("limit", MAX_RESULTS_TO_AI)
                .append("index", SEARCH_INDEX_NAME);

        Bson vectorSearch = new Document("$vectorSearch", vecSearchDocument);

        AggregateIterable<Document> results = embeddingStore.aggregate(Arrays.asList(vectorSearch));
        
        for (Document result : results) {
            similarContent.add(result.getString("Content"));
        }

        return similarContent;
    }

    public boolean contentAlreadyStored() {
        
        MongoCollection<org.bson.Document> embeddingStore = db.getCollection(COLLECTION_NAME);
        return embeddingStore.countDocuments() > 0;

    }

}
