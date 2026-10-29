package com.example;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import java.util.List;

import org.bson.types.ObjectId;

import com.example.models.Product;
import com.example.models.ProductView;
import com.example.models.requests.ProductPatchRequest;
import com.example.models.requests.ProductPostRequest;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.result.UpdateResult;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ProductService {

    final MongoContext context;

    @Inject
    public ProductService(MongoContext tx) {
        this.context = tx;
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
            return null;
        });

        return ProductView.of(p);
    }

    public void disable(String id) {
        ObjectId oid = parseId(id);

        UpdateResult result = context.run(s -> Product.mongoCollection().updateOne(
                s,
                eq("_id", oid),
                set("active", false)));

        if (result.getMatchedCount() == 0) {
            throw new NotFoundException("Product " + id + " not found");
        }
    }

    public Product find(String id) {
        Product p = Product.findById(parseId(id));
        if (p == null)
            throw new NotFoundException();
        return p;
    }

    public List<ProductView> findAll() {
        List<Product> all = Product.listAll();
        return all.stream().map(ProductView::of).toList();
    }

    private ObjectId parseId(String id) {
        if (!ObjectId.isValid(id)) {
            throw new BadRequestException(id + " is not a valid ObjectId");
        }
        return new ObjectId(id);
    }

}
