package com.example;

import java.util.List;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.jwt.JsonWebToken;

import com.example.models.OrderView;
import com.example.models.requests.OrderPostRequest;

import io.quarkus.security.ForbiddenException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/orders")
@RolesAllowed({ "customer", "admin" })
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    final OrderService orderService;
    final JsonWebToken jwt;

    @Inject
    public OrderResource(OrderService orderService, JsonWebToken jwt) {
        this.orderService = orderService;
        this.jwt = jwt;
    }

    @GET
    public List<OrderView> list() {
        return isAdmin()
                ? orderService.listAll()
                : orderService.listForCustomer(customerId());
    }

    @GET
    @Path("/{id}")
    public OrderView get(@PathParam("id") String id) {
        return OrderView.of(orderService.find(id));
    }

    @POST
    @RolesAllowed("customer")
    public Response create(@Valid OrderPostRequest req) {
        return Response.accepted(orderService.create(req, customerId()))
                .build();
    }

    @POST
    @Path("/{id}/cancel")
    public Response cancel(@PathParam("id") String id) {
        return Response.ok(orderService.cancel(id)).build();
    }

    private boolean isAdmin() {
        return jwt.getGroups().contains("admin");
    }

    private ObjectId customerId() {
        String c = jwt.getClaim("customerId");
        if (c == null)
            throw new ForbiddenException("Token has no customerId claim");
        return new ObjectId(c);
    }
}
