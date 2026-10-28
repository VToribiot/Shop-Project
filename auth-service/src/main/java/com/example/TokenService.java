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
import jakarta.ws.rs.NotFoundException;

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

    /**
     * Generates an access token for the given user.
     * 
     * @param user The user for whom the access token is generated.
     * @return The generated access token as a String.
     */
    public String accessToken(User user) {
        JwtClaimsBuilder token = Jwt.issuer(issuer).subject(user.id.toHexString()).upn(user.email).audience(audience)
                .expiresIn(accessTtl).groups(new HashSet<>(user.roles));

        if (user.customerId != null) {
            token.claim("customerId", user.customerId.toHexString());
        }

        return token.jws().keyId(keyId).sign();
    }

    /**
     * Creates a token payload for the given user, including an access token and a
     * refresh token.
     * The refresh token is stored in the database with a hashed value and an
     * expiration time.
     * 
     * @param user The user for whom the token payload is created.
     * @return The created token payload containing all the details for the other
     *         services.
     */

    public TokenPayload createTokenPayload(User user) {
        String raw = createToken();
        RefreshToken rt = new RefreshToken();
        rt.userId = user.id;
        rt.tokenHash = tokenHash(raw);
        rt.expiresAt = Instant.now().plus(refreshTtl);
        rt.persist();
        return new TokenPayload(accessToken(user), raw, accessTtl.toSeconds(), "Bearer");
    }

    /**
     * Generates a random token string.
     * 
     * @return A random token string.
     */
    static String createToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Encodes a token string using SHA-256.
     *
     * @param token The token string to encode.
     * @return The hashed token.
     */
    static String tokenHash(String token) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}
