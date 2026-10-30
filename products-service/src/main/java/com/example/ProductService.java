package com.example;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import java.util.List;

import org.bson.types.ObjectId;

import com.common.Events.ProductUpserted;
import com.common.MongoContext;
import com.common.Outbox;
import com.common.Topics;
import com.example.models.Product;
import com.example.models.ProductView;
import com.example.models.requests.ProductPatchRequest;
import com.example.models.requests.ProductPostRequest;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.result.UpdateResult;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ProductService {

    private final MongoContext context;
    private final Outbox outbox;

    @Inject
    public ProductService(MongoContext context, Outbox outbox) {
        this.context = context;
        this.outbox = outbox;
    }

    public ProductView create(ProductPostRequest product) {

        Product p = new Product(
                product.sku(),
                product.name(),
                product.price(),
                product.stock());

        try {
            context.run(s -> {
                Product.mongoCollection().insertOne(s, p);
                publish(s, p);
                return null;
            });

            return ProductView.of(p);
        } catch (MongoWriteException e) {

            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY)
                throw new Error("Product already exists");

            throw e;
        }
    }

    public ProductView update(String id, ProductPatchRequest product) {
        Product p = find(id);

        if (product.name() != null)
            p.name = product.name();
        if (product.price() != null)
            p.price = product.price();

        context.run(s -> {
            Product.mongoCollection().updateOne(s, eq("_id", p.id),
                    combine(set("name", p.name), set("price", p.price)));
            publish(s, p);
            return null;
        });

        return ProductView.of(p);
    }

    public void disable(String id) {
        ObjectId oid = parseId(id);

        UpdateResult result = context.run(s -> {
            UpdateResult updateResult = Product.mongoCollection().updateOne(
                    s,
                    eq("_id", oid),
                    set("active", false));
            if (updateResult.getMatchedCount() > 0)
                publish(s, Product.findById(oid));
            return updateResult;
        });

        if (result.getMatchedCount() == 0) {
            throw new NotFoundException("Product " + id + " not found");
        }
    }

    public ProductView adjustStock(String id, int delta) {
        ObjectId oid = parseId(id);

        UpdateResult result = context.run(s -> {
            UpdateResult updateResult = Product.mongoCollection().updateOne(
                    s,
                    eq("_id", oid),
                    combine(set("stock", Math.max(0, delta))));
            if (updateResult.getMatchedCount() > 0)
                publish(s, Product.findById(oid));
            return updateResult;

        });

        if (result.getMatchedCount() == 0) {
            throw new NotFoundException("Product " + id + " not found");
        }

        return ProductView.of(find(id));
    }

    public Product find(String id) {
        Product p = Product.findById(parseId(id));
        if (p == null || !p.active)
            throw new NotFoundException();
        return p;
    }

    public List<ProductView> findAll() {
        List<Product> all = Product.list(eq("active", true));
        return all.stream().map(ProductView::of).toList();
    }

    private ObjectId parseId(String id) {
        if (!ObjectId.isValid(id)) {
            throw new BadRequestException(id + " is not a valid ObjectId");
        }
        return new ObjectId(id);
    }

    private void publish(ClientSession s, Product p) {
        outbox.add(s, Topics.PRODUCT_EVENTS, p.id.toHexString(), "ProductUpserted",
                new ProductUpserted(p.id.toHexString(), p.sku, p.name, Double.toString(p.price), p.active));
    }

}
