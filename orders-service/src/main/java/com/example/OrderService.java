package com.example;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import com.common.Events.OrderCancelled;
import com.common.Events.OrderPlaced;
import com.common.MongoContext;
import com.common.Outbox;
import com.common.Topics;
import com.example.models.CatalogProduct;
import com.example.models.Order;
import com.example.models.OrderItem;
import com.example.models.OrderStatus;
import com.example.models.OrderView;
import com.example.models.requests.ItemRequest;
import com.example.models.requests.OrderPostRequest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class OrderService {

    private final MongoContext context;
    private final Outbox outbox;

    @Inject
    public OrderService(MongoContext context, Outbox outbox) {
        this.context = context;
        this.outbox = outbox;
    }

    public OrderView create(OrderPostRequest request, ObjectId customerId) {
        Double total = 0.0;

        Order order = new Order(customerId, new ArrayList<>(), total);
        order.id = new ObjectId();
        order.customerId = customerId;
        List<OrderPlaced.Item> eventItems = new ArrayList<>();

        for (ItemRequest r : request.items()) {
            CatalogProduct p = CatalogProduct.findById(parseId(r.productId()));
            if (p == null || !p.active) {
                throw new BadRequestException("Unknown or inactive product: " + r.productId());
            }
            OrderItem item = new OrderItem(p.id, p.sku, p.name, p.price, r.quantity());
            order.items.add(item);
            eventItems.add(new OrderPlaced.Item(p.id.toHexString(), r.quantity()));
            total += p.price * r.quantity();
        }
        order.total = total;

        OrderPlaced event = new OrderPlaced(order.id.toHexString(), order.customerId.toHexString(), eventItems);
        context.run(s -> {
            Order.mongoCollection().insertOne(s, order);
            outbox.add(s, Topics.ORDER_EVENTS, event.orderId(), "OrderPlaced", event);
            return null;
        });

        return OrderView.of(order);
    }

    public OrderView cancel(String id) {
        Order o = find(id);
        List<OrderPlaced.Item> items = o.items.stream()
                .map(i -> new OrderPlaced.Item(i.productId.toHexString(), i.quantity)).toList();

        boolean cancelled = context.run(s -> {
            var result = Order.mongoCollection().updateOne(s,
                    and(eq("_id", o.id), eq("status", OrderStatus.CONFIRMED)),
                    combine(set("status", OrderStatus.CANCELLED), set("updatedAt", Instant.now())));
            if (result.getModifiedCount() == 0)
                return false;
            outbox.add(s, Topics.ORDER_EVENTS, id, "OrderCancelled",
                    new OrderCancelled(id, o.customerId.toHexString(), items));
            return true;
        });
        if (!cancelled) {
            throw new WebApplicationException("Only CONFIRMED orders can be cancelled", 409);
        }
        Order fresh = Order.findById(o.id);
        return OrderView.of(fresh);
    }

    public List<OrderView> listForCustomer(ObjectId customerId) {
        List<Order> orders = Order.find("customerId", customerId).list();
        return orders.stream().map(OrderView::of).toList();
    }

    public List<OrderView> listAll() {
        List<Order> orders = Order.listAll();
        return orders.stream().map(OrderView::of).toList();
    }

    public Order find(String id) {
        Order o = Order.findById(parseId(id));
        if (o == null) {
            throw new NotFoundException();
        }
        return o;
    }

    private static ObjectId parseId(String id) {
        if (!ObjectId.isValid(id))
            throw new BadRequestException("Invalid id");
        return new ObjectId(id);
    }

}
