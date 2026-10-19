package com.example.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ComparisonTest {
    @LocalServerPort int port;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    ObjectNode request(String method, boolean modern) {
        ObjectNode request = Rpc.object().put("jsonrpc", "2.0").put("id", 1).put("method", method);
        ObjectNode params = request.putObject("params");
        if (modern) {
            ObjectNode meta = params.putObject("_meta").put(Rpc.PREFIX + "protocolVersion", Rpc.MODERN);
            meta.putObject(Rpc.PREFIX + "clientCapabilities").putObject("elicitation").putObject("form");
        }
        if (method.equals("tools/call")) {
            params.put("name", "search_documents");
            params.putObject("arguments").put("region", "kr").put("query", "deployment");
        }
        return request;
    }

    HttpRequest wire(String path, String body, Map<String, String> headers) {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream");
        headers.forEach(builder::header);
        return builder.POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }

    HttpResponse<String> send(String path, JsonNode body, Map<String, String> headers) throws Exception {
        return http.send(wire(path, body.toString(), headers), HttpResponse.BodyHandlers.ofString());
    }

    Map<String, String> modernHeaders(String method) {
        return Map.of("MCP-Protocol-Version", Rpc.MODERN, "Mcp-Method", method,
                "Mcp-Name", "search_documents", "Mcp-Param-Region", "kr");
    }

    Map<String, String> session(boolean initialized) throws Exception {
        ObjectNode init = request("initialize", false);
        init.withObject("params").put("protocolVersion", Rpc.LEGACY)
                .putObject("capabilities").putObject("elicitation").putObject("form");
        init.withObject("params").set("clientInfo", Rpc.info());
        var response = send("/legacy/kr/mcp", init, Map.of());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json.readTree(response.body()).path("result").path("protocolVersion").asText()).isEqualTo(Rpc.LEGACY);
        var headers = Map.of("MCP-Session-Id", response.headers().firstValue("MCP-Session-Id").orElseThrow(),
                "MCP-Protocol-Version", Rpc.LEGACY);
        if (initialized) {
            ObjectNode ready = request("notifications/initialized", false);
            ready.remove("id");
            assertThat(send("/legacy/kr/mcp", ready, headers).statusCode()).isEqualTo(202);
        }
        return headers;
    }

    @Test void legacyRequiresInitializeAndReadyNotification() throws Exception {
        assertThat(send("/legacy/mcp", request("tools/list", false), Map.of()).statusCode()).isEqualTo(400);
        assertThat(send("/legacy/kr/mcp", request("tools/list", false), session(false)).statusCode()).isEqualTo(400);
    }

    @Test void legacySessionDoesNotMoveAcrossNodes() throws Exception {
        var headers = session(true);
        assertThat(send("/legacy/kr/mcp", request("tools/list", false), headers).statusCode()).isEqualTo(200);
        assertThat(send("/legacy/us/mcp", request("tools/list", false), headers).statusCode()).isEqualTo(404);
    }

    @Test void modernNeedsNoInitializationOrSessionOnEitherNode() throws Exception {
        for (String node : new String[]{"kr", "us"}) {
            var response = send("/modern/" + node + "/mcp", request("tools/list", true), modernHeaders("tools/list"));
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("MCP-Session-Id")).isEmpty();
            JsonNode result = json.readTree(response.body()).path("result");
            assertThat(result.path("resultType").asText()).isEqualTo("complete");
            assertThat(result.path("ttlMs").asInt()).isZero();
            assertThat(result.path("tools").get(0).path("inputSchema").path("properties").path("region")
                    .path("x-mcp-header").asText()).isEqualTo("Region");
        }
    }

    @Test void discoverAdvertisesTheImplementedVersion() throws Exception {
        var response = send("/modern/mcp", request("server/discover", true), modernHeaders("server/discover"));
        JsonNode result = json.readTree(response.body()).path("result");
        assertThat(result.path("supportedVersions").get(0).asText()).isEqualTo(Rpc.MODERN);
        assertThat(result.path("_meta").path(Rpc.PREFIX + "serverInfo").path("name").asText()).isNotEmpty();
    }

    @Test void gatewayUsesBodyForLegacyAndHeadersForModern() throws Exception {
        ObjectNode oldRequest = request("tools/call", false);
        oldRequest.withObject("params").withObject("arguments").put("scope", "public");
        var old = send("/legacy/mcp", oldRequest, session(true));
        ObjectNode newRequest = request("tools/call", true);
        newRequest.withObject("params").withObject("arguments").put("scope", "public");
        var current = send("/modern/mcp", newRequest, modernHeaders("tools/call"));
        assertThat(old.headers().firstValue("X-Demo-Routing")).contains("body");
        assertThat(current.headers().firstValue("X-Demo-Routing")).contains("headers");
        assertThat(json.readTree(old.body()).path("result").path("structuredContent"))
                .isEqualTo(json.readTree(current.body()).path("result").path("structuredContent"));
    }

    @Test void modernRejectsHeaderBodyMismatch() throws Exception {
        ObjectNode body = request("tools/call", true);
        body.withObject("params").withObject("arguments").put("region", "us");
        var response = send("/modern/mcp", body, modernHeaders("tools/call"));
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json.readTree(response.body()).path("error").path("code").asInt()).isEqualTo(-32020);
    }

    @Test void modernRejectsMissingHeadersAndCapabilities() throws Exception {
        var missing = send("/modern/mcp", request("tools/list", true), Map.of());
        assertThat(json.readTree(missing.body()).path("error").path("code").asInt()).isEqualTo(-32020);
        ObjectNode body = request("tools/call", true);
        body.withObject("params").withObject("_meta").putObject(Rpc.PREFIX + "clientCapabilities");
        var response = send("/modern/mcp", body, modernHeaders("tools/call"));
        assertThat(json.readTree(response.body()).path("error").path("code").asInt()).isEqualTo(-32021);
    }

    @Test void unsupportedVersionIsDistinctFromHeaderMismatch() throws Exception {
        ObjectNode body = request("tools/list", true);
        body.withObject("params").withObject("_meta").put(Rpc.PREFIX + "protocolVersion", "1900-01-01");
        var response = send("/modern/mcp", body,
                Map.of("MCP-Protocol-Version", "1900-01-01", "Mcp-Method", "tools/list"));
        assertThat(json.readTree(response.body()).path("error").path("code").asInt()).isEqualTo(-32022);
    }

    @Test void mrtrReturnsInputRequiredThenCompletesASeparateRequest() throws Exception {
        ObjectNode body = request("tools/call", true);
        var first = send("/modern/mcp", body, modernHeaders("tools/call"));
        JsonNode input = json.readTree(first.body()).path("result");
        assertThat(input.path("resultType").asText()).isEqualTo("input_required");
        assertThat(input.path("inputRequests").path("scope").path("method").asText()).isEqualTo("elicitation/create");
        body.put("id", 2).withObject("params").putObject("inputResponses").putObject("scope")
                .put("action", "accept").putObject("content").put("scope", "public");
        var second = send("/modern/mcp", body, modernHeaders("tools/call"));
        JsonNode completed = json.readTree(second.body());
        assertThat(completed.path("id").asInt()).isEqualTo(2);
        assertThat(completed.path("result").path("resultType").asText()).isEqualTo("complete");
        assertThat(completed.path("result").path("structuredContent").path("documents").get(0).asText())
                .isEqualTo("Deployment guide: Korea");
    }

    @Test void mrtrHandlesDeclineAndRejectsInvalidScope() throws Exception {
        ObjectNode body = request("tools/call", true);
        ObjectNode answer = body.withObject("params").putObject("inputResponses").putObject("scope");
        answer.put("action", "decline");
        assertThat(json.readTree(send("/modern/mcp", body, modernHeaders("tools/call")).body())
                .path("result").path("isError").asBoolean()).isTrue();
        answer.put("action", "accept").putObject("content").put("scope", "secret");
        assertThat(json.readTree(send("/modern/mcp", body, modernHeaders("tools/call")).body())
                .path("error").path("code").asInt()).isEqualTo(-32602);
    }

    @Test void legacySseCarriesServerRequestAndOriginalResult() throws Exception {
        var headers = session(true);
        var response = http.send(wire("/legacy/kr/mcp", request("tools/call", false).toString(), headers),
                HttpResponse.BodyHandlers.ofInputStream());
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("text/event-stream");
        try (var executor = Executors.newSingleThreadExecutor();
             var reader = new BufferedReader(new InputStreamReader(response.body()))) {
            JsonNode input = executor.submit(() -> readEvent(reader)).get(5, TimeUnit.SECONDS);
            assertThat(input.path("method").asText()).isEqualTo("elicitation/create");
            ObjectNode answer = Rpc.envelope(input.get("id"));
            answer.putObject("result").put("action", "accept").putObject("content").put("scope", "public");
            assertThat(send("/legacy/kr/mcp", answer, headers).statusCode()).isEqualTo(202);
            JsonNode result = executor.submit(() -> readEvent(reader)).get(5, TimeUnit.SECONDS);
            assertThat(result.path("id").asInt()).isEqualTo(1);
            assertThat(result.path("result").has("resultType")).isFalse();
            assertThat(result.path("result").path("structuredContent").path("documents").size()).isEqualTo(1);
        }
    }

    @Test void modernRejectsWrongMethodAndToolHeaders() throws Exception {
        ObjectNode body = request("tools/call", true);
        for (String header : new String[]{"Mcp-Method", "Mcp-Name", "MCP-Protocol-Version"}) {
            var headers = new java.util.HashMap<>(modernHeaders("tools/call"));
            headers.put(header, "wrong");
            var response = send("/modern/mcp", body, headers);
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(json.readTree(response.body()).path("error").path("code").asInt()).isEqualTo(-32020);
        }
    }

    @Test void modernRejectsUnknownMethodAndWrongDirectNode() throws Exception {
        var unknown = send("/modern/mcp", request("unknown", true), modernHeaders("unknown"));
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(json.readTree(unknown.body()).path("error").path("code").asInt()).isEqualTo(-32601);
        ObjectNode body = request("tools/call", true);
        body.withObject("params").withObject("arguments").put("scope", "public");
        assertThat(send("/modern/us/mcp", body, modernHeaders("tools/call")).statusCode()).isEqualTo(400);
    }

    @Test void usRegionRoutesToUsDocuments() throws Exception {
        ObjectNode body = request("tools/call", true);
        body.withObject("params").withObject("arguments").put("region", "us").put("scope", "public");
        var headers = new java.util.HashMap<>(modernHeaders("tools/call"));
        headers.put("Mcp-Param-Region", "us");
        var response = send("/modern/mcp", body, headers);
        assertThat(response.headers().firstValue("X-Demo-Node")).contains("us");
        assertThat(json.readTree(response.body()).path("result").path("structuredContent")
                .path("documents").get(0).asText()).isEqualTo("Deployment guide: US");
    }

    @Test void browserOriginsAreNotAccepted() throws Exception {
        var headers = new java.util.HashMap<>(modernHeaders("tools/list"));
        headers.put("Origin", "https://example.com");
        assertThat(send("/modern/mcp", request("tools/list", true), headers).statusCode()).isEqualTo(403);
    }

    @Test void localDemoOriginIsAllowedButOtherPortsAreRejected() throws Exception {
        for (String host : new String[]{"localhost", "127.0.0.1"}) {
            var headers = new java.util.HashMap<>(modernHeaders("tools/list"));
            headers.put("Origin", "http://" + host + ":" + port);
            assertThat(send("/modern/mcp", request("tools/list", true), headers).statusCode()).isEqualTo(200);
            headers.put("Origin", "http://" + host + ":" + (port + 1));
            assertThat(send("/modern/mcp", request("tools/list", true), headers).statusCode()).isEqualTo(403);
        }
    }

    @Test void comparisonPageIsServedWithLocalAssets() throws Exception {
        for (String path : new String[]{"/", "/comparison.js", "/comparison.css"}) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isNotBlank();
        }
    }

    private JsonNode readEvent(BufferedReader reader) throws Exception {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("data:")) return json.readTree(line.substring(5).strip());
        }
        throw new IllegalStateException("Stream ended before an event");
    }

    @Test void malformedEnvelopeAndJsonReturnProtocolErrors() throws Exception {
        var malformed = http.send(wire("/modern/mcp", "{", modernHeaders("tools/list")), HttpResponse.BodyHandlers.ofString());
        assertThat(json.readTree(malformed.body()).path("error").path("code").asInt()).isEqualTo(-32700);
        var invalid = send("/modern/mcp", Rpc.object(), modernHeaders("tools/list"));
        assertThat(json.readTree(invalid.body()).path("error").path("code").asInt()).isEqualTo(-32600);
    }
}
