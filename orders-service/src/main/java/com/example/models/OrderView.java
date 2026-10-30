package com.example.models;

import java.time.Instant;
import java.util.List;

public record OrderView(String id, String status, List<ItemView> items, Double total, Instant createdAt) {
        public static OrderView of(Order o) {
                return new OrderView(o.id.toHexString(), o.status,
                                o.items.stream().map(i -> new ItemView(i.productId.toHexString(), i.sku, i.name,
                                                i.unitPrice, i.quantity)).toList(),
                                o.total, o.createdAt);
        }
}