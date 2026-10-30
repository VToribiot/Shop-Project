package com.example;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import com.common.EventEnvelope;
import com.common.Events.ProductUpserted;
import com.example.models.CatalogProduct;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CatalogConsumer {

    final ObjectMapper mapper;

    @Inject
    public CatalogConsumer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Incoming("product-events-in")
    @Blocking
    public void onMessage(String json) throws Exception {
        System.out.println("Received product event");

        EventEnvelope env = mapper.readValue(json, EventEnvelope.class);

        if (!"ProductUpserted".equals(env.type())) {
            System.out.println("Ignored event: " + env.type());
            return;
        }

        ProductUpserted e = mapper.treeToValue(env.data(), ProductUpserted.class);
        ObjectId productId = new ObjectId(e.productId());

        if (!e.active()) {
            boolean deleted = CatalogProduct.deleteById(productId);
            System.out.println("Product " + productId + (deleted ? " deleted" : " not found, nothing to delete"));
            return;
        }

        CatalogProduct c = new CatalogProduct();
        c.id = productId;
        c.sku = e.sku();
        c.name = e.name();
        c.price = Double.parseDouble(e.price());
        c.active = true;

        c.persistOrUpdate();

        System.out.println("Product saved successfully!");
    }
}
