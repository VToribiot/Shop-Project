package com.example;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.unset;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.example.models.RefreshToken;
import com.example.models.TokenPayload;
import com.example.models.User;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class AuthService {

    private static final String FIELD_REVOKED = "revoked";
    private static final String FIELD_TOKEN_HASH = "tokenHash";

    /**
     * Compared against when the email is unknown, so response time does not reveal
     * which emails exist.
     */
    private static final String DUMMY_HASH = BcryptUtil.bcryptHash("dummy-password");

    private final TokenService tokens;

    @Inject
    public AuthService(TokenService tokens) {
        this.tokens = tokens;
    }

    public User register(String email, String password, List<String> roles) {
        String e = email.toLowerCase().trim();
        if (User.findByEmail(e) != null) {
            throw new WebApplicationException("Email already registered", 409);
        }
        User u = new User();
        u.email = e;
        u.passwordHash = BcryptUtil.bcryptHash(password);
        u.roles = new ArrayList<>(roles);
        u.customerId = new ObjectId(); // the customer identity used by orders-service
        u.persist();
        return u;
    }

    public TokenPayload login(String email, String password) {
        User u = User.findByEmail(email.toLowerCase().trim());
        if (u == null) {
            BcryptUtil.matches(password, DUMMY_HASH);
            throw invalid();
        }
        if (!u.isActive)
            throw invalid();
        if (!BcryptUtil.matches(password, u.passwordHash)) {
            throw invalid();
        }
        User.mongoCollection().updateOne(eq("_id", u.id), combine(
                set("failedLoginCount", 0),
                unset("lockedUntil"),
                set("lastLoginAt", Instant.now())));
        return tokens.createTokenPayload(u);
    }

    public TokenPayload refresh(String raw) {
        String hash = TokenService.tokenHash(raw);
        RefreshToken rt = (RefreshToken) RefreshToken.mongoCollection().findOneAndUpdate(
                and(eq(FIELD_TOKEN_HASH, hash), eq(FIELD_REVOKED, false), gt("expiresAt", Instant.now())),
                set(FIELD_REVOKED, true));
        if (rt == null) {
            RefreshToken old = RefreshToken.find(FIELD_TOKEN_HASH, hash).firstResult();
            if (old != null)
                revokeFamily(old);
            throw invalid();
        }
        User u = User.findById(rt.userId);
        if (u == null || !u.isActive) {
            revokeFamily(rt);
            throw invalid();
        }
        return tokens.createTokenPayload(u);
    }

    public void logout(String raw) {
        RefreshToken rt = RefreshToken.find(FIELD_TOKEN_HASH, TokenService.tokenHash(raw)).firstResult();
        if (rt != null)
            revokeFamily(rt);
    }

    private void revokeFamily(RefreshToken token) {
        RefreshToken.mongoCollection().updateMany(eq(FIELD_TOKEN_HASH, token.tokenHash), set(FIELD_REVOKED, true));
    }

    private WebApplicationException invalid() {
        return new WebApplicationException("Invalid credentials", 401);
    }
}
