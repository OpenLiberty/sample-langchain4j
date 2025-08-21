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
import com.mongodb.client.MongoIterable;

public class DatabaseReady {
    public static boolean existing(MongoClient mongodbClient, String name) {
        MongoIterable<String> allDb = mongodbClient.listDatabaseNames();
        for (String dbName : allDb) {
            if (name.equals(dbName)) {
                return true;
            }
        }
        return false;
    }

}
