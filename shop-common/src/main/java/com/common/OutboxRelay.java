package com.common;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;

/**
 * Polls the outbox and publishes pending events in creation order. Delivery is
 * at-least-once
 * (a crash between send and markPublished re-sends), so consumers must be
 * idempotent.
 */
@ApplicationScoped
public class OutboxRelay {

    @Inject
    Outbox outbox;
    @Inject
    KafkaPublisher publisher;

    @Scheduled(every = "1s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void relay() {
        for (Document d : outbox.pending(100)) {
            String id = d.getString("_id");
            try {
                publisher.send(d.getString("topic"), d.getString("key"), d.getString("payload"));
                outbox.markPublished(id);
            } catch (Exception e) {
                Log.warnf("Outbox publish failed for %s, will retry: %s", id, e.getMessage());
                return; // keep ordering: stop at the first failure
            }
        }
    }
}
