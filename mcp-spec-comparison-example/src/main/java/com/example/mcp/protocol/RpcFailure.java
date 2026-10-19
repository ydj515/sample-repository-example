package com.example.mcp.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class RpcFailure extends RuntimeException {
    private final int status;
    private final int code;
    private ObjectNode data;

    RpcFailure(int status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() {
        return status;
    }

    public RpcFailure withData(ObjectNode data) {
        this.data = data;
        return this;
    }

    public ObjectNode response(JsonNode id) {
        ObjectNode response = Rpc.envelope(id);
        ObjectNode error = response.putObject("error").put("code", code).put("message", getMessage());
        if (data != null) error.set("data", data);
        return response;
    }
}
