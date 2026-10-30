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
        EventEnvelope env = mapper.readValue(json, EventEnvelope.class);
        if (!"ProductUpserted".equals(env.type()))
            return;
        ProductUpserted e = mapper.treeToValue(env.data(), ProductUpserted.class);

        CatalogProduct c = new CatalogProduct();
        c.id = new ObjectId(e.productId());
        c.sku = e.sku();
        c.name = e.name();
        c.price = Double.parseDouble(e.price());
        c.active = e.active();
        c.persistOrUpdate();
    }
}
