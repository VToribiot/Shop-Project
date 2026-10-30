package com.example.models;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "products")
public class Product extends PanacheMongoEntity {

    public String sku;
    public String name;
    public double price;
    public int stock;
    public boolean active = true;

    public Product() {
    }

    public Product(String sku, String name, double price, int stock) {
        this.id = new ObjectId();
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }
}