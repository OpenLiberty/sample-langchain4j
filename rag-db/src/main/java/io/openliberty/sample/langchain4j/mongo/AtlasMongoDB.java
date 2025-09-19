/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.sample.langchain4j.mongo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONObject;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.SearchIndexModel;
import com.mongodb.client.model.SearchIndexType;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import io.openliberty.sample.langchain4j.util.ModelBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AtlasMongoDB {

    private final String COLLECTION_NAME = "EmbeddingsStored";
    private final String SEARCH_INDEX_NAME = "vector_index";
    private final String PATH = "Vector";
    private final int MAX_RESULTS_TO_AI = 1;
    private final int NUM_CANDIDATES = 2;
    private final int TIMEOUT = 5000;
    @Inject
    private ModelBuilder modelBuilder;

    @Inject
    MongoDatabase db;

    private void sleep(int t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
        }
    }

    public MongoCollection<Document> getCollection() {
        return db.getCollection(COLLECTION_NAME);
    }

    public boolean isEmpty() {
        return getCollection().countDocuments() < 1;
    }

    public void createIndex() {

        db.createCollection(COLLECTION_NAME);
        MongoCollection<Document> embeddingStore = db.getCollection(COLLECTION_NAME);

        Bson documentDefinition = new Document("fields",
            Collections.singletonList(
                new Document("type", "vector")
                    .append("path", PATH)
                    .append("numDimensions", modelBuilder.getEmbeddingModel().dimension())
                    .append("similarity", "cosine")));
        SearchIndexType vectorSearch = SearchIndexType.vectorSearch();
        List<SearchIndexModel> searchIndexModels = Collections.singletonList(
                new SearchIndexModel(SEARCH_INDEX_NAME, documentDefinition, vectorSearch));

        sleep(TIMEOUT);
		embeddingStore.createSearchIndexes(searchIndexModels);
        boolean indexReady = false;
        do{
            Document index = embeddingStore.listSearchIndexes().first();
            JSONObject json = new JSONObject(index.toJson());
            boolean queryable = json.getBoolean("queryable");
            if (queryable && json.getString("name").equals(SEARCH_INDEX_NAME)) {
                indexReady = true;
                break;
            }
        }while (!indexReady);
    }

    public float[] convertUserQueryToEmbedding(String userQuery) {
        return modelBuilder.getEmbeddingModel().embed(userQuery).content().vector();
    }

    public DeleteResult deleteOne(Document query) {
        return getCollection().deleteOne(query);
    }

    public void insertOne(Document newEmbedding) {
        getCollection().insertOne(newEmbedding);
    }

    public UpdateResult replaceOne(Document query, Document newEmbedding) {
        return getCollection().replaceOne(query, newEmbedding);
    }

    public List<String> retrieveContent(List<Float> userQuery, String query) {
        List<String> similarContent = new ArrayList<>();
        Document vecSearchDocument = new Document("queryVector", userQuery)
            .append("path", PATH)
            .append("numCandidates", NUM_CANDIDATES)
            .append("limit", MAX_RESULTS_TO_AI)
            .append("index", SEARCH_INDEX_NAME);
        Bson vectorSearch = new Document("$vectorSearch", vecSearchDocument);
        AggregateIterable<Document> results = getCollection().aggregate(Arrays.asList(vectorSearch));
        for (Document result : results) {
            similarContent.add(result.getString("Content"));
        }
        return similarContent;
    }

}
