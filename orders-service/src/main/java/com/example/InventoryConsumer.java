package com.example;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import com.common.EventEnvelope;
import com.common.Events.StockRejected;
import com.common.Events.StockReserved;
import com.example.models.Order;
import com.example.models.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/**
 * Completes the saga: PENDING -> CONFIRMED | REJECTED. The update only matches
 * orders still in
 * PENDING, so redelivered events are harmless (idempotent without a ledger).
 */
@ApplicationScoped
public class InventoryConsumer {

    final ObjectMapper mapper;

    @Inject
    public InventoryConsumer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Incoming("inventory-events-in")
    @Blocking
    public void onMessage(String json) throws Exception {
        EventEnvelope env = mapper.readValue(json, EventEnvelope.class);
        switch (env.type()) {
            case "StockReserved" -> {
                StockReserved e = mapper.treeToValue(env.data(), StockReserved.class);
                transition(e.orderId(), OrderStatus.CONFIRMED, null);
            }
            case "StockRejected" -> {
                StockRejected e = mapper.treeToValue(env.data(), StockRejected.class);
                transition(e.orderId(), OrderStatus.REJECTED, e.reason());
            }
            default -> {
            }
        }
    }

    private void transition(String orderId, String newStatus, String reason) {
        Order.mongoCollection().updateOne(
                and(eq("_id", new ObjectId(orderId)), eq("status", OrderStatus.PENDING)),
                combine(set("status", newStatus),
                        set("rejectionReason", reason),
                        set("updatedAt", Instant.now())));
    }
}
