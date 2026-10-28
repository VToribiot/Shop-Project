package com.example;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.example.models.RefreshToken;
import com.example.models.TokenPayload;
import com.example.models.User;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @ConfigProperty(name = "app.jwt.issuer")
    String issuer;

    @ConfigProperty(name = "app.jwt.audience")
    String audience;

    @ConfigProperty(name = "app.jwt.key-id")
    String keyId;

    @ConfigProperty(name = "app.jwt.access-ttl")
    Duration accessTtl;

    @ConfigProperty(name = "app.jwt.refresh-ttl")
    Duration refreshTtl;

    public String accessToken(User user) {
        JwtClaimsBuilder token = Jwt.issuer(issuer).subject(user.id.toHexString()).upn(user.email).audience(audience)
                .expiresIn(accessTtl).groups(new HashSet<>(user.roles));

        if (user.customerId != null) {
            token.claim("customerId", user.customerId.toHexString());
        }

        return token.jws().keyId(keyId).sign();
    }

    public TokenPayload createTokenPayload(User user) {
        String raw = createToken();
        RefreshToken rt = new RefreshToken();
        rt.userId = user.id;
        rt.tokenHash = passwordHash(raw);
        rt.expiresAt = Instant.now().plus(refreshTtl);
        rt.persist();
        return new TokenPayload(accessToken(user), raw, accessTtl.toSeconds(), "Bearer");
    }

    static String createToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String passwordHash(String password) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}
