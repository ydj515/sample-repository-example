package com.example.mcp.protocol.modern;

import com.example.mcp.protocol.Rpc;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.http.HttpHeaders;

final class ModernHeaderValidator {
    private ModernHeaderValidator() {}

    static void match(HttpHeaders headers, String name, String expected) {
        String actual = headers.getFirst(name);
        if (actual != null && actual.startsWith("=?base64?") && actual.endsWith("?=")) {
            try {
                if (actual.length() < 11) throw new IllegalArgumentException("Incomplete Base64 header");
                actual = new String(Base64.getDecoder().decode(actual.substring(9, actual.length() - 2)),
                        StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ex) {
                throw Rpc.fail(400, -32020, "Malformed header: " + name);
            }
        }
        if (expected.isEmpty() || !expected.equals(actual)) {
            throw Rpc.fail(400, -32020, "Header mismatch: " + name);
        }
    }
}
