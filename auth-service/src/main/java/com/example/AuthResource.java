package com.example;

import java.util.List;
import java.util.Map;

import com.example.models.Credentials;
import com.example.models.RefreshRequest;
import com.example.models.TokenPayload;
import com.example.models.User;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService auth;

    @Inject
    public AuthResource(AuthService auth) {
        this.auth = auth;
    }

    @POST
    @Path("/register")
    public Response register(@Valid Credentials c) {
        User user = auth.register(c.email(), c.password(), List.of("customer"));

        return Response.status(201).entity(Map.of("id", user.id.toHexString(), "email", user.email,
                "customerId", user.customerId.toHexString()))
                .build();
    }

    @POST
    @Path("/login")
    public TokenPayload login(@Valid Credentials c) {
        return auth.login(c.email(), c.password());
    }

    @POST
    @Path("/refresh")
    public TokenPayload refresh(@Valid RefreshRequest r) {
        return auth.refresh(r.refreshToken());
    }

    @POST
    @Path("/logout")
    public Response logout(@Valid RefreshRequest r) {
        auth.logout(r.refreshToken());
        return Response.noContent().build();
    }
}
