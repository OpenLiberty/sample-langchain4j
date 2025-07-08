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
