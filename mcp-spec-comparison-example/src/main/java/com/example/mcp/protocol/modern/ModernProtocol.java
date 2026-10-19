package com.example.mcp.protocol.modern;

import com.example.mcp.protocol.Rpc;
import com.example.mcp.tool.DocumentSearch;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public final class ModernProtocol {
    private final DocumentSearch search;

    ModernProtocol(DocumentSearch search) {
        this.search = search;
    }

    public ObjectNode handle(String node, JsonNode request, HttpHeaders headers) {
        JsonNode params = request.path("params");
        JsonNode meta = params.path("_meta");
        ModernHeaderValidator.match(headers, "MCP-Protocol-Version", meta.path(Rpc.PREFIX + "protocolVersion").asText());
        ModernHeaderValidator.match(headers, "Mcp-Method", request.path("method").asText());
        String version = meta.path(Rpc.PREFIX + "protocolVersion").asText();
        if (!Rpc.MODERN.equals(version)) {
            ObjectNode data = Rpc.object().put("requested", version);
            data.putArray("supported").add(Rpc.MODERN);
            throw Rpc.fail(400, -32022, "Unsupported protocol version").withData(data);
        }
        if (!meta.path(Rpc.PREFIX + "clientCapabilities").isObject()) {
            throw Rpc.fail(400, -32602, "clientCapabilities must be an object");
        }
        String method = request.path("method").asText();
        if (method.equals("tools/call")) {
            ModernHeaderValidator.match(headers, "Mcp-Name", params.path("name").asText());
            search.validate(params);
            ModernHeaderValidator.match(headers, "Mcp-Param-Region", params.path("arguments").path("region").asText());
            if (!node.equals(params.path("arguments").path("region").asText())) {
                throw Rpc.fail(400, -32602, "Region does not match selected node");
            }
        }
        ObjectNode result = switch (method) {
            case "server/discover" -> discover();
            case "tools/list" -> search.tools(true);
            case "tools/call" -> call(params);
            default -> throw Rpc.fail(404, -32601, "Unknown method; supported version: " + Rpc.MODERN);
        };
        boolean inputRequired = "input_required".equals(result.path("resultType").asText());
        ObjectNode response = Rpc.result(request.get("id"), result, true);
        if (inputRequired) result.put("resultType", "input_required");
        return response;
    }

    private ObjectNode discover() {
        ObjectNode result = Rpc.object();
        result.putArray("supportedVersions").add(Rpc.MODERN);
        result.putObject("capabilities").putObject("tools");
        result.put("ttlMs", 0).put("cacheScope", "private");
        return result;
    }

    private ObjectNode call(JsonNode params) {
        JsonNode args = params.path("arguments");
        if (args.has("scope")) return search.search(args, search.validateScope(args.get("scope")));
        JsonNode capabilities = params.path("_meta").path(Rpc.PREFIX + "clientCapabilities");
        if (!capabilities.path("elicitation").path("form").isObject()) {
            throw Rpc.fail(400, -32021, "Form elicitation capability required when scope is absent");
        }
        JsonNode answer = params.path("inputResponses").path("scope");
        if (!answer.isMissingNode()) {
            String action = answer.path("action").asText();
            if (action.equals("decline") || action.equals("cancel")) return search.declined();
            if (!action.equals("accept")) throw Rpc.fail(200, -32602, "Invalid elicitation action");
            return search.search(args, search.validateScope(answer.path("content").path("scope")));
        }
        ObjectNode result = Rpc.object().put("resultType", "input_required");
        result.putObject("inputRequests").set("scope", search.inputRequest());
        // No continuation context is needed: the retry contains the complete read-only query.
        return result;
    }
}
