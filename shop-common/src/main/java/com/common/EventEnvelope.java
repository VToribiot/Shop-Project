package com.common;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record EventEnvelope(String eventId, String type, int version, Instant occurredAt, JsonNode data) {
}
