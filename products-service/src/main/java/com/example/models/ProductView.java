package com.example.models;

public record ProductView(String id, String sku, String name, double price, int stock, boolean active) {
    public static ProductView of(Product p) {
        return new ProductView(p.id.toHexString(), p.sku, p.name, p.price, p.stock, p.active);
    }
}
