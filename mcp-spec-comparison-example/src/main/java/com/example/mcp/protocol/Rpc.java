package com.example.mcp.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class Rpc {
    public static final String LEGACY = "2025-11-25";
    public static final String MODERN = "2026-07-28";
    public static final String PREFIX = "io.modelcontextprotocol/";

    private Rpc() {}

    public static ObjectNode object() {
        return JsonNodeFactory.instance.objectNode();
    }

    public static ObjectNode envelope(JsonNode id) {
        ObjectNode result = object().put("jsonrpc", "2.0");
        result.set("id", id == null ? JsonNodeFactory.instance.nullNode() : id);
        return result;
    }

    public static ObjectNode result(JsonNode id, ObjectNode result, boolean modern) {
        if (modern) {
            result.put("resultType", "complete");
            result.withObject("_meta").set(PREFIX + "serverInfo", info());
        }
        ObjectNode response = envelope(id);
        response.set("result", result);
        return response;
    }

    public static ObjectNode info() {
        return object().put("name", "mcp-spec-comparison").put("version", "1.0.0");
    }

    public static RpcFailure fail(int status, int code, String message) {
        return new RpcFailure(status, code, message);
    }
}
