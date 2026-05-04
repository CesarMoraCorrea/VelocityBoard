package com.example.VelocityBoard.config;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.example.VelocityBoard.repository")
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        Dotenv dotenv = null;
        try {
            dotenv = Dotenv.configure().ignoreIfMissing().load();
        } catch (Exception e) {
            // Ignore if .env is missing (e.g., in production)
        }
        
        // Attempt to load from dotenv, fallback to System environment variables
        String uri = null;
        if (dotenv != null) {
            uri = dotenv.get("MONGODB_URI");
        }
        if (uri == null || uri.isEmpty()) {
            uri = System.getenv("MONGODB_URI");
        }
        if (uri == null || uri.isEmpty()) {
            uri = System.getProperty("MONGODB_URI");
        }
        if (uri == null || uri.isEmpty()) {
            uri = "mongodb://localhost:27017/velocityboard";
        }
        
        System.out.println("====== CUSTOM MONGO CONFIG URI: " + uri + " ======");
        return MongoClients.create(uri);
    }

    @Override
    protected String getDatabaseName() {
        return "velocityboard";
    }
}
