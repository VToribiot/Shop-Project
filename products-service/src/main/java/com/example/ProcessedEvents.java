package com.example;

import com.example.models.exceptions.DuplicateEventException;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Date;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Idempotency ledger. markProcessed() runs inside the same transaction as the
 * business change:
 * if the event was already handled, the unique _id rejects the insert and the
 * whole transaction aborts.
 */
@ApplicationScoped
public class ProcessedEvents {

    final MongoClient client;

    @Inject
    public ProcessedEvents(MongoClient client, @ConfigProperty(name = "quarkus.mongodb.database") String db) {
        this.client = client;
        this.db = db;
    }

    @ConfigProperty(name = "quarkus.mongodb.database")
    String db;

    public void markProcessed(ClientSession session, String eventId) {
        try {
            client.getDatabase(db).getCollection("processed_events")
                    .insertOne(session, new Document("_id", eventId).append("at", new Date()));
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                throw new DuplicateEventException(eventId);
            }
            throw e;
        }
    }
}
