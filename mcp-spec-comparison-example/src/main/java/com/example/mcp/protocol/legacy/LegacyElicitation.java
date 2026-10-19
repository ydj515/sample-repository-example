package com.example.mcp.protocol.legacy;

import com.example.mcp.protocol.Rpc;
import com.example.mcp.tool.DocumentSearch;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
final class LegacyElicitation {
    private final DocumentSearch search;

    LegacyElicitation(DocumentSearch search) {
        this.search = search;
    }

    Object open(LegacySession session, JsonNode request) {
        if (session.pending != null) throw Rpc.fail(409, -32000, "One pending elicitation per demo session");
        SseEmitter stream = new SseEmitter(15_000L);
        String inputId = "scope-" + UUID.randomUUID();
        PendingElicitation pending = new PendingElicitation(inputId, request.deepCopy(), stream);
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

    Object answer(LegacySession session, JsonNode answer) {
        PendingElicitation pending = session.pending;
        if (pending == null || !pending.inputId().equals(answer.path("id").asText())) {
            throw Rpc.fail(200, -32602, "No matching pending server request");
        }
        JsonNode result = answer.path("result");
        String action = result.path("action").asText();
        ObjectNode toolResult;
        if (answer.has("error") || action.equals("decline") || action.equals("cancel")) {
            toolResult = search.declined();
        } else if (action.equals("accept")) {
            toolResult = search.search(pending.request().path("params").path("arguments"),
                    search.validateScope(result.path("content").path("scope")));
        } else {
            throw Rpc.fail(200, -32602, "Invalid elicitation response");
        }
        session.pending = null;
        try {
            pending.stream().send(SseEmitter.event().name("message")
                    .data(Rpc.result(pending.request().get("id"), toolResult, false)));
            pending.stream().complete();
        } catch (IOException ex) {
            pending.stream().completeWithError(ex);
        }
        return ResponseEntity.accepted().build();
    }
}
