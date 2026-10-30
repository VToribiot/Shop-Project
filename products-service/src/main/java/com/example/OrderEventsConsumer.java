package com.example;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Updates.inc;

import com.common.EventEnvelope;
import com.common.MongoContext;
import com.common.Events.OrderCancelled;
import com.common.Events.OrderPlaced;
import com.common.Events.StockRejected;
import com.common.Events.StockReserved;
import com.common.Outbox;
import com.common.Topics;
import com.example.models.Product;
import com.example.models.exceptions.DuplicateEventException;
import com.example.models.exceptions.OutOfStockException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/**
 * Inventory side of the order saga.
 * OrderPlaced -> reserve stock for every item (all or nothing) -> StockReserved
 * | StockRejected
 * OrderCancelled -> give the stock back
 * Each message is handled in ONE Mongo transaction: idempotency ledger + stock
 * + outbox event.
 */
@ApplicationScoped
public class OrderEventsConsumer {

    final MongoContext context;
    final Outbox outbox;
    final ProcessedEvents processed;
    final ObjectMapper mapper;

    @Inject
    public OrderEventsConsumer(MongoContext context, Outbox outbox, ProcessedEvents processed, ObjectMapper mapper) {
        this.context = context;
        this.outbox = outbox;
        this.processed = processed;
        this.mapper = mapper;
    }

    @Incoming("order-events-in")
    @Blocking
    public void onMessage(String json) throws Exception {
        EventEnvelope env = mapper.readValue(json, EventEnvelope.class);
        switch (env.type()) {
            case "OrderPlaced" -> onOrderPlaced(env, mapper.treeToValue(env.data(), OrderPlaced.class));
            case "OrderCancelled" -> onOrderCancelled(env, mapper.treeToValue(env.data(), OrderCancelled.class));
            default -> Log.debugf("Ignoring event type %s", env.type());
        }
    }

    private void onOrderPlaced(EventEnvelope env, OrderPlaced e) {
        try {
            context.run(s -> {
                processed.markProcessed(s, env.eventId());
                for (OrderPlaced.Item item : e.items()) {
                    var result = Product.mongoCollection().updateOne(s,
                            and(eq("_id", new ObjectId(item.productId())),
                                    eq("active", true),
                                    gte("stock", item.quantity())),
                            inc("stock", -item.quantity()));
                    if (result.getModifiedCount() == 0) {
                        throw new OutOfStockException(item.productId());
                    }
                }
                outbox.add(s, Topics.INVENTORY_EVENTS, e.orderId(), "StockReserved", new StockReserved(e.orderId()));
                return null;
            });
        } catch (DuplicateEventException dup) {
            Log.debugf("Duplicate %s skipped", env.eventId());
        } catch (OutOfStockException oos) {
            reject(env, e, oos.getMessage());
        }
    }

    private void reject(EventEnvelope env, OrderPlaced e, String reason) {
        try {
            context.run(s -> {
                processed.markProcessed(s, env.eventId());
                outbox.add(s, Topics.INVENTORY_EVENTS, e.orderId(), "StockRejected",
                        new StockRejected(e.orderId(), reason));
                return null;
            });
        } catch (DuplicateEventException dup) {
            Log.debugf("Duplicate %s skipped", env.eventId());
        }
    }

    private void onOrderCancelled(EventEnvelope env, OrderCancelled e) {
        try {
            context.run(s -> {
                processed.markProcessed(s, env.eventId());
                for (OrderPlaced.Item item : e.items()) {
                    Product.mongoCollection().updateOne(s,
                            eq("_id", new ObjectId(item.productId())),
                            inc("stock", item.quantity()));
                }
                return null;
            });
        } catch (RuntimeException dup) {
            Log.debugf("Duplicate %s skipped", env.eventId());
        }
    }
}
