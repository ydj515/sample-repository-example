package com.example.mcp.presentation;

import com.example.mcp.protocol.Rpc;
import com.fasterxml.jackson.databind.JsonNode;

final class RpcEnvelopeValidator {
    private RpcEnvelopeValidator() {}

    static void validate(JsonNode request, String era) {
        if (request == null || !request.isObject() || !"2.0".equals(request.path("jsonrpc").asText())) {
            throw Rpc.fail(400, -32600, "Expected a JSON-RPC 2.0 object");
        }
        boolean initialized = "notifications/initialized".equals(request.path("method").asText());
        if (!(initialized && era.equals("legacy"))
                && (!request.has("id") || !(request.get("id").isTextual() || request.get("id").isIntegralNumber()))) {
            throw Rpc.fail(400, -32600, "A string or integer request id is required");
        }
        if (request.has("method")) {
            if (!request.get("method").isTextual()
                    || (request.has("params") && !request.get("params").isObject())) {
                throw Rpc.fail(400, -32600, "Invalid method or params");
            }
        } else if (!era.equals("legacy") || request.has("result") == request.has("error")) {
            throw Rpc.fail(400, -32600, "Expected method or legacy response");
        }
    }
}
