package com.example.models;

import org.bson.types.ObjectId;

public class OrderItem {
    public ObjectId productId;
    public String sku;
    public String name;
    public double unitPrice;
    public int quantity;

    public OrderItem(ObjectId productId, String sku, String name, double unitPrice, int quantity) {
        this.productId = productId;
        this.sku = sku;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }
}
