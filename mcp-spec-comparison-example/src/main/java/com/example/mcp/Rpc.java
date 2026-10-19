package com.example.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class Rpc {
    static final String LEGACY = "2025-11-25";
    static final String MODERN = "2026-07-28";
    static final String PREFIX = "io.modelcontextprotocol/";

    private Rpc() {}

    static ObjectNode object() {
        return JsonNodeFactory.instance.objectNode();
    }

    static ObjectNode envelope(JsonNode id) {
        ObjectNode result = object().put("jsonrpc", "2.0");
        result.set("id", id == null ? JsonNodeFactory.instance.nullNode() : id);
        return result;
    }

    static ObjectNode result(JsonNode id, ObjectNode result, boolean modern) {
        if (modern) {
            result.put("resultType", "complete");
            result.withObject("_meta").set(PREFIX + "serverInfo", info());
        }
        ObjectNode response = envelope(id);
        response.set("result", result);
        return response;
    }

    static ObjectNode info() {
        return object().put("name", "mcp-spec-comparison").put("version", "1.0.0");
    }

    static Failure fail(int status, int code, String message) {
        return new Failure(status, code, message);
    }

    static final class Failure extends RuntimeException {
        final int status;
        final int code;
        ObjectNode data;

        Failure(int status, int code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }

        ObjectNode response(JsonNode id) {
            ObjectNode response = envelope(id);
            ObjectNode error = response.putObject("error").put("code", code).put("message", getMessage());
            if (data != null) error.set("data", data);
            return response;
        }
    }
}
