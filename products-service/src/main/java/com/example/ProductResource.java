package com.example;

import java.util.List;

import org.bson.types.ObjectId;

import com.example.models.Product;
import com.example.models.ProductView;
import com.example.models.requests.ProductPatchRequest;
import com.example.models.requests.ProductPostRequest;
import com.example.models.requests.StockChangeRequest;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({ "admin" })
public class ProductResource {

    final ProductService productService;

    @Inject
    public ProductResource(ProductService productService) {
        this.productService = productService;
    }

    @GET
    @PermitAll
    public List<ProductView> list() {
        return productService.findAll();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response get(@PathParam("id") String id) {
        return Response.ok(ProductView.of(productService.find(id))).build();
    }

    @POST
    public Response create(ProductPostRequest p) {
        return Response.status(201).entity(productService.create(p)).build();
    }

    @POST
    @Path("/{id}/stock")
    @RolesAllowed("admin")
    public Response adjustStock(@PathParam("id") String id, StockChangeRequest adj) {
        return Response.ok(productService.adjustStock(id, adj.delta())).build();
    }

    @PATCH
    @Path("/{id}")
    public Response update(@PathParam("id") String id, ProductPatchRequest p) {
        return Response.status(200).entity(productService.update(id, p)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        productService.disable(id);
        return Response.noContent().build();
    }
}