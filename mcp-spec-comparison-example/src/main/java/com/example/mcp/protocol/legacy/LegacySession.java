package com.example.mcp.protocol.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

final class LegacySession {
    private static final long SESSION_SECONDS = 300;
    final String node;
    final JsonNode capabilities;
    final Instant expires = Instant.now().plusSeconds(SESSION_SECONDS);
    boolean initialized;
    PendingElicitation pending;

    LegacySession(String node, JsonNode capabilities) {
        this.node = node;
        this.capabilities = capabilities;
    }
}
