/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package dev.langchain4j.example.chat;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.logging.Logger;

@ApplicationScoped
public class MongodbClient {
    @Inject
    @ConfigProperty(name = "mongo.uri.connection", defaultValue = "")
    private String MOGODB_CONNECTION_STRING;

    private static Logger logger = Logger.getLogger(MongodbClient.class.getName());

    @Produces
    public MongoClient createMongodbClient() {

        if (MOGODB_CONNECTION_STRING == "" || MOGODB_CONNECTION_STRING == null
                || MOGODB_CONNECTION_STRING.equals("set it by env variable")) {
            throw new IllegalStateException("Mongodb Connection string can not be empty");
        }

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(MOGODB_CONNECTION_STRING))
                .applyToSslSettings(builder -> builder.enabled(true))
                .build();

        MongoClient mongoClient = MongoClients.create(settings);
        logger.info("Enabled TLS/SSL when connecting to MongoDB Atlas");

        return mongoClient;

    }
}
