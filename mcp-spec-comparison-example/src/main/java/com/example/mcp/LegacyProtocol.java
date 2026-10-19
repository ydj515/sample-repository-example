package com.example.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
final class LegacyProtocol {
    private static final long SESSION_SECONDS = 300;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final DocumentSearch search;

    private static final class Session {
        final String node;
        final JsonNode capabilities;
        final Instant expires = Instant.now().plusSeconds(SESSION_SECONDS);
        boolean initialized;
        Pending pending;

        Session(String node, JsonNode capabilities) {
            this.node = node;
            this.capabilities = capabilities;
        }
    }

    private record Pending(String inputId, JsonNode request, SseEmitter stream) {}

    LegacyProtocol(DocumentSearch search) {
        this.search = search;
    }

    Object handle(String node, JsonNode request, HttpHeaders headers, HttpServletResponse response) {
        if ("initialize".equals(request.path("method").asText())) {
            return initialize(node, request, response);
        }
        String sessionId = headers.getFirst("MCP-Session-Id");
        if (sessionId == null) throw Rpc.fail(400, -32000, "Initialize first and send MCP-Session-Id");
        Session session = sessions.get(sessionId);
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
            if (!request.has("method")) return answer(session, request);
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
        sessions.put(sessionId, new Session(node, params.path("capabilities")));
        response.setHeader("MCP-Session-Id", sessionId);
        ObjectNode result = Rpc.object().put("protocolVersion", Rpc.LEGACY);
        result.putObject("capabilities").putObject("tools");
        result.set("serverInfo", Rpc.info());
        return Rpc.result(request.get("id"), result, false);
    }

    private Object call(Session session, JsonNode request) {
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
        if (session.pending != null) throw Rpc.fail(409, -32000, "One pending elicitation per demo session");
        SseEmitter stream = new SseEmitter(15_000L);
        String inputId = "scope-" + UUID.randomUUID();
        Pending pending = new Pending(inputId, request.deepCopy(), stream);
        session.pending = pending;
        Runnable clear = () -> {
            synchronized (session) {
                if (session.pending == pending) session.pending = null;
            }
        };
        stream.onCompletion(clear);
        stream.onTimeout(() -> { clear.run(); stream.complete(); });
        stream.onError(error -> clear.run());
        ObjectNode input = search.inputRequest().put("jsonrpc", "2.0").put("id", inputId);
        try {
            stream.send(SseEmitter.event().name("message").data(input));
        } catch (IOException ex) {
            clear.run();
            stream.completeWithError(ex);
        }
        return stream;
    }

    private Object answer(Session session, JsonNode answer) {
        Pending pending = session.pending;
        if (pending == null || !pending.inputId.equals(answer.path("id").asText())) {
            throw Rpc.fail(200, -32602, "No matching pending server request");
        }
        JsonNode result = answer.path("result");
        String action = result.path("action").asText();
        ObjectNode toolResult;
        if (answer.has("error") || action.equals("decline") || action.equals("cancel")) {
            toolResult = search.declined();
        } else if (action.equals("accept")) {
            toolResult = search.search(pending.request.path("params").path("arguments"),
                    search.validateScope(result.path("content").path("scope")));
        } else {
            throw Rpc.fail(200, -32602, "Invalid elicitation response");
        }
        session.pending = null;
        try {
            pending.stream.send(SseEmitter.event().name("message")
                    .data(Rpc.result(pending.request.get("id"), toolResult, false)));
            pending.stream.complete();
        } catch (IOException ex) {
            pending.stream.completeWithError(ex);
        }
        return ResponseEntity.accepted().build();
    }
}
