package io.openliberty.sample.langchain4j.mongo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListSearchIndexesIterable;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.SearchIndexModel;
import com.mongodb.client.model.SearchIndexType;

import dev.langchain4j.model.embedding.EmbeddingModel;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.onnx.bgesmallenq.BgeSmallEnQuantizedEmbeddingModel;
import jakarta.inject.Inject;

import org.bson.Document;

import org.json.JSONObject;
public class AtlasMongoDB {
    private final String COLLECTION_NAME = "Embeddings";
    private final String SEARCH_INDEX_NAME = "vector_search_index";
    private final String PATH = "Vector";
    private final int MAX_RESULTS_TO_AI = 10;
    private final int NUM_CANDIDATES = 150;

    private static final String[] MD_FILES = {
            "logs-1.md", "security-1.md", "mp-health-1.md"
    };


    private final EmbeddingModel MODEL = new BgeSmallEnQuantizedEmbeddingModel();

    @Inject
    private MongoDatabase db;

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

    public void deleteCollection() {
        MongoCollection<Document> collection = db.getCollection(COLLECTION_NAME);
        collection.drop();

    }

    public void createSearchIndex() throws InterruptedException {
        // this implementation is similar to one from the Mongodb's tutorial online
        db.createCollection(COLLECTION_NAME);
        MongoCollection<Document> collection = db.getCollection(COLLECTION_NAME);
        // define the index details for the index model

        String indexName = SEARCH_INDEX_NAME;
        Bson definition = new Document(
                "fields",
                Collections.singletonList(
                        new Document("type", "vector")
                                .append("path", PATH)
                                .append("numDimensions", MODEL.dimension())
                                .append("similarity", "cosine")));
        SearchIndexModel indexModel = new SearchIndexModel(
                indexName,
                definition,
                SearchIndexType.vectorSearch());
        // create the index using the defined model
        try {
            List<String> result = collection.createSearchIndexes(Collections.singletonList(indexModel));
            System.out.println("Successfully created a vector index named: " + result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // wait for Atlas to build the index and make it queryable
        ListSearchIndexesIterable<Document> searchIndexes = collection.listSearchIndexes();
        while (true) {
            try (MongoCursor<Document> cursor = searchIndexes.iterator()) {
                if (!cursor.hasNext()) {
                    break;
                }
                Document current = cursor.next();
                String name = current.getString("name");
                boolean queryable = current.getBoolean("queryable");
                if (name.equals(indexName) && queryable) {
                    System.out.println(indexName + " index is ready to query");
                    return;
                } else {
                    Thread.sleep(500);
                }
            }
        }
        System.out.println("Polling to confirm the index has completed building.");
        System.out.println("It may take up to a minute for the index to build before you can query using it.");

    }

    public void createIndex() {

        db.createCollection(COLLECTION_NAME);

        MongoCollection<Document> embeddingStore = db.getCollection(COLLECTION_NAME);

        Bson documentDefinition = new Document(
                "fields",
                Collections.singletonList(
                        new Document("type", "vector")
                                .append("path", PATH)
                                .append("numDimensions", MODEL.dimension())
                                .append("similarity", "cosine")));

        SearchIndexType vectorSearch = SearchIndexType.vectorSearch();

        List<SearchIndexModel> searchIndexModels = Collections.singletonList(
                new SearchIndexModel(SEARCH_INDEX_NAME, documentDefinition, vectorSearch));

        try {

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

    public void intializeDatabase() {
        if (!contentAlreadyStored()) {
            try {
                createIndex();
                try {
                    MongoCollection<Document> embeddingStore = db.getCollection(COLLECTION_NAME);
                    ClassLoader classLoader = Embedding.class.getClassLoader();
                    for (String txtFile : MD_FILES) {
                        InputStream inStream = classLoader.getResourceAsStream("documents/" + txtFile);
                        if (inStream != null) {
                            InputStreamReader reader = new InputStreamReader(inStream, StandardCharsets.UTF_8);
                            BufferedReader br = new BufferedReader(reader);
                            StringBuffer content = new StringBuffer();
                            String line;
                            while ((line = br.readLine()) != null) {
                                content.append(line).append("\n");
                            }
                            br.close();
                            reader.close();
                            inStream.close();

                            Document newEmbedding = new Document();

                            float[] contentEmbedding = MODEL.embed(content.toString()).content().vector();
                            List<Float> vector = new ArrayList<>();
                            for (float elem : contentEmbedding) {
                                vector.add(elem);
                            }

                            newEmbedding.put("Content", content.toString());
                            newEmbedding.put("Vector", vector);
                            embeddingStore.insertOne(newEmbedding);
                        }
                    }
                } catch (Exception error) {
                    System.out.println("Error in loading into Atlas." + error.getMessage());
                }
            } catch (Exception e) {
                System.out.println("ERROR in search index: Please retry again." + e.getMessage());
            }
        }
    }

    public float[] convertUserQueryToEmbedding(String userQuery) {
        return MODEL.embed(userQuery).content().vector();
    }

    public List<String> retrieveContent(List<Float> userQuery) {

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
