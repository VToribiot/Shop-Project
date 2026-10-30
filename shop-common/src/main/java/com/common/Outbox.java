package com.common;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Transactional outbox. Services call add(session, ...) inside the SAME
 * transaction that changes
 * their business data, so "state changed" and "event to publish" are committed
 * atomically.
 * OutboxRelay later ships pending rows to Kafka.
 */
@ApplicationScoped
public class Outbox {

    @Inject
    MongoClient client;
    @Inject
    ObjectMapper mapper;
    @ConfigProperty(name = "quarkus.mongodb.database")
    String database;

    private MongoCollection<Document> col() {
        return client.getDatabase(database).getCollection("outbox");
    }

    void onStart(@Observes StartupEvent ev) {
        col().createIndex(Indexes.compoundIndex(Indexes.ascending("published"), Indexes.ascending("createdAt")));
    }

    public void add(ClientSession session, String topic, String key, String type, Object data) {
        String eventId = UUID.randomUUID().toString();
        try {
            String payload = mapper.writeValueAsString(
                    new EventEnvelope(eventId, type, 1, Instant.now(), mapper.valueToTree(data)));
            col().insertOne(session, new Document("_id", eventId)
                    .append("topic", topic)
                    .append("key", key)
                    .append("payload", payload)
                    .append("published", false)
                    .append("createdAt", new Date()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize event " + type, e);
        }
    }

    List<Document> pending(int limit) {
        return col().find(eq("published", false)).sort(Sorts.ascending("createdAt")).limit(limit)
                .into(new ArrayList<>());
    }

    void markPublished(String eventId) {
        col().updateOne(eq("_id", eventId), combine(set("published", true), set("publishedAt", new Date())));
    }
}
