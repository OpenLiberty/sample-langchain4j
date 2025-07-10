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

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MongodbClient {
    @Inject
    @ConfigProperty(name = "mongo.uri.connection", defaultValue = "")
    String connectionStr;

    @Produces
    public MongoClient createMongodbClient() {
        if (connectionStr == "" || connectionStr == null) {
            throw new IllegalStateException("Mongodb Connection string can not be empty");
        }
        return MongoClients.create(connectionStr);

    }
}
