package com.example.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class ComparisonController {
    private final ObjectMapper mapper;
    private final LegacyProtocol legacy;
    private final ModernProtocol modern;

    ComparisonController(ObjectMapper mapper, LegacyProtocol legacy, ModernProtocol modern) {
        this.mapper = mapper;
        this.legacy = legacy;
        this.modern = modern;
    }

    @GetMapping("/health")
    Object health() {
        return Rpc.object().put("status", "ok");
    }

    @PostMapping(value = {"/{era}/mcp", "/{era}/{node}/mcp"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    Object post(@PathVariable String era, @PathVariable(required = false) String node,
                @RequestHeader HttpHeaders headers, @RequestBody String body,
                HttpServletResponse response, HttpServletRequest servletRequest) {
        JsonNode request = null;
        try {
            if (!List.of("legacy", "modern").contains(era)) throw Rpc.fail(404, -32601, "Unknown era");
            String origin = headers.getOrigin();
            if (origin != null && !List.of("http://127.0.0.1:" + servletRequest.getLocalPort(),
                    "http://localhost:" + servletRequest.getLocalPort()).contains(origin)) {
                throw Rpc.fail(403, -32000, "Only this local demo origin is allowed");
            }
            String accept = String.join(",", headers.getOrEmpty("Accept"));
            if (!accept.contains("application/json") || !accept.contains("text/event-stream")) {
                throw Rpc.fail(406, -32600, "Accept must include application/json and text/event-stream");
            }
            if (body.length() > 65536) throw Rpc.fail(413, -32600, "Demo request too large");
            // Modern routing happens before JSON parsing. Nodes share a JVM: this is not a network proxy.
            String selected = node;
            if (selected == null && era.equals("modern")) {
                selected = headers.getFirst("Mcp-Param-Region");
                if (selected == null) selected = "kr";
                response.setHeader("X-Demo-Routing", "headers");
            }
            request = mapper.readTree(body);
            validateEnvelope(request, era);
            if (selected == null) {
                selected = request.path("params").path("arguments").path("region").asText("kr");
                response.setHeader("X-Demo-Routing", "body");
            }
            if (!List.of("kr", "us").contains(selected)) throw Rpc.fail(400, -32602, "Unknown demo node");
            response.setHeader("X-Demo-Node", selected);
            if (era.equals("legacy")) return legacy.handle(selected, request, headers, response);
            return modern.handle(selected, request, headers);
        } catch (JsonProcessingException ex) {
            return ResponseEntity.badRequest().body(Rpc.fail(400, -32700, "Parse error").response(null));
        } catch (Rpc.Failure failure) {
            return ResponseEntity.status(failure.status).body(failure.response(request == null ? null : request.get("id")));
        }
    }

    private void validateEnvelope(JsonNode request, String era) {
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
