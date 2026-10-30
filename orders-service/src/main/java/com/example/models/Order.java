package com.example.models;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;

@MongoEntity(collection = "orders")
public class Order extends PanacheMongoEntity {
    public ObjectId customerId;
    public String status = OrderStatus.PENDING;
    public List<OrderItem> items = new ArrayList<>();
    public double total;
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();

    public Order(ObjectId customerId, List<OrderItem> items, double total) {
        this.id = new ObjectId();
        this.customerId = customerId;
        this.items = items;
        this.total = total;
    }
}
