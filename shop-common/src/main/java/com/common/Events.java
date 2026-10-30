package com.common;

import java.util.List;

/** Event payloads. Money travels as String to avoid precision loss. */
public final class Events {

    // orders -> products (topic order.events)
    public record OrderPlaced(String orderId, String customerId, List<Item> items) {
        public record Item(String productId, int quantity) {
        }
    }

    public record OrderCancelled(String orderId, String customerId, List<OrderPlaced.Item> items) {
    }

    // products -> orders (topic inventory.events)
    public record StockReserved(String orderId) {
    }

    public record StockRejected(String orderId, String reason) {
    }

    // products -> orders (topic product.events). State-carried: always the full
    // current product.
    public record ProductUpserted(String productId, String sku, String name, String price, boolean active) {
    }

    private Events() {
    }
}
