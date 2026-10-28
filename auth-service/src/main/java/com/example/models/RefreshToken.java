package com.example.models;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import java.time.Instant;
import org.bson.types.ObjectId;

@MongoEntity(collection = "refresh_tokens")
public class RefreshToken extends PanacheMongoEntity {
    public ObjectId userId;
    public String tokenHash;
    public boolean revoked;
    public Instant createdAt = Instant.now();
    public Instant expiresAt;
}
