package com.example.mcp.tool;

import com.example.mcp.protocol.Rpc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public final class DocumentSearch {
    record Document(String region, String scope, String title) {}

    private final List<Document> documents = List.of(
            new Document("kr", "public", "Deployment guide: Korea"),
            new Document("us", "public", "Deployment guide: US"),
            new Document("kr", "team", "Deployment checklist: Korea team"),
            new Document("us", "team", "Deployment checklist: US team"));

    public ObjectNode tools(boolean modern) {
        ObjectNode tool = Rpc.object().put("name", "search_documents")
                .put("description", "Search synthetic documents by region, query and scope");
        ObjectNode schema = tool.putObject("inputSchema").put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ObjectNode region = properties.putObject("region").put("type", "string");
        region.putArray("enum").add("kr").add("us");
        if (modern) region.put("x-mcp-header", "Region");
        properties.putObject("query").put("type", "string");
        properties.putObject("scope").put("type", "string").putArray("enum").add("public").add("team");
        schema.putArray("required").add("region").add("query");
        ObjectNode result = Rpc.object();
        result.putArray("tools").add(tool);
        if (modern) result.put("ttlMs", 0).put("cacheScope", "private");
        return result;
    }

    public void validate(JsonNode params) {
        if (!"search_documents".equals(params.path("name").asText())) {
            throw Rpc.fail(200, -32602, "Unknown tool");
        }
        JsonNode args = params.path("arguments");
        if (!args.isObject() || !args.path("query").isTextual() || args.path("query").asText().isBlank()
                || args.path("query").asText().length() > 200
                || !List.of("kr", "us").contains(args.path("region").asText())) {
            throw Rpc.fail(200, -32602, "Expected region kr/us and a non-empty query (max 200 characters)");
        }
        if (args.has("scope")) validateScope(args.get("scope"));
    }

    public String validateScope(JsonNode scope) {
        if (!scope.isTextual() || !List.of("public", "team").contains(scope.asText())) {
            throw Rpc.fail(200, -32602, "Expected scope public/team");
        }
        return scope.asText();
    }

    public ObjectNode inputRequest() {
        ObjectNode input = Rpc.object().put("method", "elicitation/create");
        ObjectNode params = input.putObject("params").put("mode", "form")
                .put("message", "Choose the document scope");
        ObjectNode schema = params.putObject("requestedSchema").put("type", "object");
        schema.putObject("properties").putObject("scope").put("type", "string")
                .putArray("enum").add("public").add("team");
        schema.putArray("required").add("scope");
        return input;
    }

    public ObjectNode search(JsonNode args, String scope) {
        String query = args.path("query").asText().toLowerCase(Locale.ROOT);
        List<String> hits = documents.stream()
                .filter(d -> d.region().equals(args.path("region").asText()) && d.scope().equals(scope))
                .filter(d -> d.title().toLowerCase(Locale.ROOT).contains(query))
                .map(Document::title).toList();
        ObjectNode result = Rpc.object();
        result.putArray("content").addObject().put("type", "text")
                .put("text", String.join("\n", hits));
        result.putObject("structuredContent").putPOJO("documents", hits);
        return result;
    }

    public ObjectNode declined() {
        ObjectNode result = Rpc.object().put("isError", true);
        result.putArray("content").addObject().put("type", "text").put("text", "Scope selection declined");
        return result;
    }
}
