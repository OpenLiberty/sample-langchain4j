package dev.langchain4j.example.chat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MongodbClient {
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
