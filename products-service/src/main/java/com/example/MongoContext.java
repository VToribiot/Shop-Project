package com.example;

import java.util.function.Function;

import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MongoContext {

    final MongoClient mongoClient;

    @Inject
    public MongoContext(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public <T> T run(Function<ClientSession, T> body) {
        TransactionOptions options = TransactionOptions.builder()
                .writeConcern(WriteConcern.MAJORITY)
                .build();
        try (ClientSession session = mongoClient.startSession()) {
            return session.withTransaction(() -> body.apply(session), options);
        }

    }

}
