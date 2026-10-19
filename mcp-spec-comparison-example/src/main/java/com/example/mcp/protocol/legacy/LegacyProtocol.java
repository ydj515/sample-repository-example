package com.example.mcp.protocol.legacy;

import com.example.mcp.protocol.Rpc;
import com.example.mcp.tool.DocumentSearch;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public final class LegacyProtocol {
    private final Map<String, LegacySession> sessions = new ConcurrentHashMap<>();
    private final DocumentSearch search;
    private final LegacyElicitation elicitation;

    LegacyProtocol(DocumentSearch search, LegacyElicitation elicitation) {
        this.search = search;
        this.elicitation = elicitation;
    }

    public Object handle(String node, JsonNode request, HttpHeaders headers, HttpServletResponse response) {
        if ("initialize".equals(request.path("method").asText())) {
            return initialize(node, request, response);
        }
        String sessionId = headers.getFirst("MCP-Session-Id");
        if (sessionId == null) throw Rpc.fail(400, -32000, "Initialize first and send MCP-Session-Id");
        LegacySession session = sessions.get(sessionId);
        if (session == null || !session.node.equals(node) || session.expires.isBefore(Instant.now())) {
            throw Rpc.fail(404, -32000, "Session not found on node " + node);
        }
        if (!Rpc.LEGACY.equals(headers.getFirst("MCP-Protocol-Version"))) {
            throw Rpc.fail(400, -32602, "Expected protocol version " + Rpc.LEGACY);
        }
        synchronized (session) {
            String method = request.path("method").asText();
            if ("notifications/initialized".equals(method)) {
                session.initialized = true;
                return ResponseEntity.accepted().build();
            }
            if (!session.initialized) throw Rpc.fail(400, -32000, "Send notifications/initialized first");
            if (!request.has("method")) return elicitation.answer(session, request);
            return switch (method) {
                case "tools/list" -> Rpc.result(request.get("id"), search.tools(false), false);
                case "tools/call" -> call(session, request);
                default -> throw Rpc.fail(200, -32601, "Unknown method");
            };
        }
    }

    private synchronized Object initialize(String node, JsonNode request, HttpServletResponse response) {
        sessions.entrySet().removeIf(e -> e.getValue().expires.isBefore(Instant.now()));
        if (sessions.size() >= 1000) throw Rpc.fail(503, -32000, "Demo session limit reached");
        JsonNode params = request.path("params");
        if (!params.path("capabilities").isObject() || !params.path("protocolVersion").isTextual()) {
            throw Rpc.fail(200, -32602, "Invalid initialize parameters");
        }
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new LegacySession(node, params.path("capabilities")));
        response.setHeader("MCP-Session-Id", sessionId);
        ObjectNode result = Rpc.object().put("protocolVersion", Rpc.LEGACY);
        result.putObject("capabilities").putObject("tools");
        result.set("serverInfo", Rpc.info());
        return Rpc.result(request.get("id"), result, false);
    }

    private Object call(LegacySession session, JsonNode request) {
        JsonNode params = request.path("params");
        search.validate(params);
        JsonNode args = params.path("arguments");
        if (!args.path("region").asText().equals(session.node)) {
            throw Rpc.fail(200, -32602, "Region does not match selected node");
        }
        if (args.has("scope")) {
            return Rpc.result(request.get("id"), search.search(args, search.validateScope(args.get("scope"))), false);
        }
        if (!session.capabilities.path("elicitation").path("form").isObject()) {
            throw Rpc.fail(200, -32602, "Form elicitation capability required");
        }
        return elicitation.open(session, request);
    }
}
