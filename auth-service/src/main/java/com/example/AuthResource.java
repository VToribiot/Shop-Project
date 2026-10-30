package com.example;

import java.util.List;
import java.util.Map;

import com.example.models.Credentials;
import com.example.models.RefreshRequest;
import com.example.models.RegistryRequest;
import com.example.models.TokenPayload;
import com.example.models.User;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Authenticated
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService auth;

    @Inject
    public AuthResource(AuthService auth) {
        this.auth = auth;
    }

    @POST
    @PermitAll
    @Path("/register")
    public Response register(@Valid RegistryRequest r) {
        User user = auth.register(r.name(), r.email(), r.password(), List.of("customer"));

        return Response.status(201).entity(Map.of("id", user.id.toHexString(), "name", user.name, "email", user.email,
                "customerId", user.customerId.toHexString()))
                .build();
    }

    @POST
    @PermitAll
    @Path("/login")
    public TokenPayload login(@Valid Credentials c) {
        return auth.login(c.email(), c.password());
    }

    @POST
    @RolesAllowed({ "customer", "admin" })
    @Path("/refresh")
    public TokenPayload refresh(@Valid RefreshRequest r) {
        return auth.refresh(r.refreshToken());
    }

    @PATCH
    @RolesAllowed({ "admin" })
    @Path("{id}/disable")
    public Response disable(@PathParam("id") String id) {
        auth.disable(id);
        return Response.noContent().build();
    }

    @POST
    @PermitAll
    @Path("/logout")
    public Response logout(@Valid RefreshRequest r) {
        auth.logout(r.refreshToken());
        return Response.noContent().build();
    }
}
