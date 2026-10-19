#!/usr/bin/env python3
"""Real HTTP/SSE comparison client; Python standard library only."""
import argparse
import copy
import json
from urllib.error import HTTPError
from urllib.request import Request, urlopen

P = "io.modelcontextprotocol/"
OLD, NEW = "2025-11-25", "2026-07-28"


def rpc(method, modern=False, request_id=1):
    params = {}
    if modern:
        params["_meta"] = {
            P + "protocolVersion": NEW,
            P + "clientCapabilities": {"elicitation": {"form": {}}},
            P + "clientInfo": {"name": "comparison-client", "version": "1.0.0"},
        }
    if method == "tools/call":
        params.update(name="search_documents", arguments={"region": "kr", "query": "deployment"})
    return {"jsonrpc": "2.0", "id": request_id, "method": method, "params": params}


def modern_headers(method):
    headers = {"MCP-Protocol-Version": NEW, "Mcp-Method": method}
    if method == "tools/call":
        headers.update({"Mcp-Name": "search_documents", "Mcp-Param-Region": "kr"})
    return headers


def exchange(base, path, body, headers=None, stream=False):
    request_headers = {"Content-Type": "application/json", "Accept": "application/json, text/event-stream"}
    request_headers.update(headers or {})
    request = Request(base + path, json.dumps(body).encode(), request_headers, method="POST")
    try:
        response = urlopen(request, timeout=8)
    except HTTPError as error:
        response = error
    if stream:
        return response
    with response:
        raw = response.read()
        result = json.loads(raw) if raw else None
        print(f"  POST {path} -> {response.status}, node={response.headers.get('X-Demo-Node', '-')}, "
              f"routing={response.headers.get('X-Demo-Routing', 'direct')}")
        print(" ", json.dumps(result, ensure_ascii=False))
        return response.status, response.headers, result


def event(stream):
    for raw in stream:
        line = raw.decode().strip()
        if line.startswith("data:"):
            value = json.loads(line[5:])
            print("  SSE", json.dumps(value, ensure_ascii=False))
            return value
    raise RuntimeError("SSE closed before the expected message")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:8097")
    base = parser.parse_args().base_url.rstrip("/")
    print("1. Legacy: initialize -> initialized -> call; session is node-local")
    init = rpc("initialize")
    init["params"] = {"protocolVersion": OLD, "capabilities": {"elicitation": {"form": {}}},
                      "clientInfo": {"name": "comparison-client", "version": "1.0.0"}}
    status, headers, _ = exchange(base, "/legacy/kr/mcp", init)
    assert status == 200
    session = {"MCP-Session-Id": headers["MCP-Session-Id"], "MCP-Protocol-Version": OLD}
    ready = {"jsonrpc": "2.0", "method": "notifications/initialized"}
    assert exchange(base, "/legacy/kr/mcp", ready, session)[0] == 202
    assert exchange(base, "/legacy/us/mcp", rpc("tools/list"), session)[0] == 404

    print("2. Same search via body routing (legacy) and header routing (modern)")
    old = rpc("tools/call")
    old["params"]["arguments"]["scope"] = "public"
    old_result = exchange(base, "/legacy/mcp", old, session)[2]["result"]
    new = rpc("tools/call", True)
    new["params"]["arguments"]["scope"] = "public"
    new_result = exchange(base, "/modern/mcp", new, modern_headers("tools/call"))[2]["result"]
    assert old_result["structuredContent"] == new_result["structuredContent"]
    for node in ("kr", "us"):
        assert exchange(base, f"/modern/{node}/mcp", rpc("tools/list", True), modern_headers("tools/list"))[0] == 200

    print("3. Header/body mismatch must fail before executing a tool")
    mismatch = copy.deepcopy(new)
    mismatch["params"]["arguments"]["region"] = "us"
    status, _, error = exchange(base, "/modern/mcp", mismatch, modern_headers("tools/call"))
    assert status == 400 and error["error"]["code"] == -32020

    print("4. Legacy: answer a server-initiated request on an open SSE response")
    with exchange(base, "/legacy/kr/mcp", rpc("tools/call"), session, stream=True) as stream:
        assert stream.status == 200
        question = event(stream)
        assert question["method"] == "elicitation/create"
        answer = {"jsonrpc": "2.0", "id": question["id"],
                  "result": {"action": "accept", "content": {"scope": "public"}}}
        assert exchange(base, "/legacy/kr/mcp", answer, session)[0] == 202
        legacy_result = event(stream)
        assert legacy_result["id"] == 1 and "resultType" not in legacy_result["result"]

    print("5. Modern: input_required -> retry with a new id -> complete")
    first = rpc("tools/call", True, 10)
    result = exchange(base, "/modern/mcp", first, modern_headers("tools/call"))[2]["result"]
    assert result["resultType"] == "input_required"
    retry = copy.deepcopy(first)
    retry["id"] = 11
    retry["params"]["inputResponses"] = {"scope": {"action": "accept", "content": {"scope": "public"}}}
    # This demo returns no requestState. A generic client echoes it only when present.
    if "requestState" in result:
        retry["params"]["requestState"] = result["requestState"]
    completed = exchange(base, "/modern/mcp", retry, modern_headers("tools/call"))[2]
    assert completed["id"] == 11 and completed["result"]["resultType"] == "complete"
    assert completed["result"]["structuredContent"] == legacy_result["result"]["structuredContent"]
    print("PASS: all five real HTTP/SSE comparison scenarios")


if __name__ == "__main__":
    main()
