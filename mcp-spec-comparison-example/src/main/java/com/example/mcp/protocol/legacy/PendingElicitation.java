package com.example.mcp.protocol.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

record PendingElicitation(String inputId, JsonNode request, SseEmitter stream) {}
