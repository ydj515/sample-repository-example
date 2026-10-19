package com.example.mcp.presentation;

import com.example.mcp.protocol.Rpc;
import com.example.mcp.protocol.RpcFailure;
import com.example.mcp.protocol.legacy.LegacyProtocol;
import com.example.mcp.protocol.modern.ModernProtocol;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
            RpcEnvelopeValidator.validate(request, era);
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
        } catch (RpcFailure failure) {
            return ResponseEntity.status(failure.status()).body(failure.response(request == null ? null : request.get("id")));
        }
    }
}
