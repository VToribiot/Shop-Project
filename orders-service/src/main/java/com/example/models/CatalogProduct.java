package com.example.models;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "product_catalog")
public class CatalogProduct extends PanacheMongoEntityBase {
    @BsonId
    public ObjectId id;
    public String sku;
    public String name;
    public Double price;
    public boolean active;
}
