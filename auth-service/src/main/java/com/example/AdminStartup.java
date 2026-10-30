package com.example;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.example.models.User;

/**
 * Creates the first admin from configuration (env: APP_STARTUP_ADMIN_EMAIL /
 * APP_STARTUP_ADMIN_PASSWORD).
 */
@ApplicationScoped
public class AdminStartup {

    private final AuthService auth;

    @Inject
    public AdminStartup(AuthService auth) {
        this.auth = auth;
    }

    @ConfigProperty(name = "app.startup.admin-email")
    Optional<String> email;
    @ConfigProperty(name = "app.startup.admin-password")
    Optional<String> password;

    void onStart(@Observes StartupEvent ev) {
        if (email.isEmpty() || password.isEmpty())
            return;
        if (User.findByEmail(email.get().toLowerCase().trim()) != null)
            return;
        auth.register("John Doe", email.get(), password.get(), List.of("admin", "customer"));
        Log.infof("Bootstrap admin created: %s", email.get());
    }
}
