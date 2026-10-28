package com.example.models;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;

@MongoEntity(collection = "users")
public class User extends PanacheMongoEntity {
    public String email;
    public String name;
    public String passwordHash;
    public List<String> roles = new ArrayList<>();
    public Boolean isActive = true;
    public ObjectId customerId;
    public Instant createdAt = Instant.now();
}
