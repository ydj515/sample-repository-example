const MCP_URL = process.env.MCP_URL || "http://localhost:3000/mcp";

type Json = any;

function parseSseEvents(bodyText: string): { event?: string; data?: any }[] {
  const events: { event?: string; data?: any }[] = [];

  const chunks = bodyText.split(/\n\n+/);
  for (const chunk of chunks) {
    const lines = chunk.split(/\n/);
    let eventName: string | undefined;
    const dataLines: string[] = [];

    for (const line of lines) {
      if (line.startsWith("event:")) {
        eventName = line.slice("event:".length).trim();
      } else if (line.startsWith("data:")) {
        dataLines.push(line.slice("data:".length).trim());
      }
    }

    if (dataLines.length > 0) {
      const dataStr = dataLines.join("\n");
      try {
        const parsed = JSON.parse(dataStr);
        events.push({ event: eventName, data: parsed });
      } catch {
        events.push({ event: eventName, data: dataStr });
      }
    }
  }

  return events;
}

async function parseMcpResponse(res: Response): Promise<Json> {
  const contentType = res.headers.get("content-type") || "";

  const isJson =
    contentType.includes("application/json") ||
    contentType.includes("application/json+jsonrpc") ||
    contentType.includes("application/json+mcp");

  const isSse = contentType.includes("text/event-stream");

  if (isJson) {
    return (await res.json()) as Json;
  }

  const text = await res.text();

  if (isSse) {
    const events = parseSseEvents(text);
    if (events.length > 0) {
      return events[0].data;
    }
    throw new Error("SSE 응답을 받았지만 data 이벤트를 찾지 못했습니다.");
  }

  return text;
}

async function initializeSession() {
  const body = {
    jsonrpc: "2.0",
    id: 1,
    method: "initialize",
    params: {
      protocolVersion: "2025-06-18",
      clientInfo: {
        name: "openclaw-mcp-client",
        version: "0.1.0"
      },
      capabilities: {}
    }
  };

  const res = await fetch(MCP_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept:
        "application/json, application/json+jsonrpc, application/json+mcp, text/event-stream"
    },
    body: JSON.stringify(body)
  });

  if (!res.ok) {
    throw new Error(`initialize 실패: ${res.status} ${res.statusText}`);
  }

  const sessionId =
    res.headers.get("Mcp-Session-Id") || res.headers.get("mcp-session-id");

  if (!sessionId) {
    throw new Error("initialize 응답에 MCP 세션 ID가 없습니다.");
  }

  const json = await parseMcpResponse(res);
  return { sessionId, json };
}

async function sendMcpRequest(sessionId: string, body: Json) {
  const headers = {
    "Content-Type": "application/json",
    Accept:
      "application/json, application/json+jsonrpc, application/json+mcp, text/event-stream",
    "Mcp-Session-Id": sessionId
  } as Record<string, string>;

  const res = await fetch(MCP_URL, {
    method: "POST",
    headers,
    body: JSON.stringify(body)
  });

  if (!res.ok) {
    throw new Error(`MCP 요청 실패: ${res.status} ${res.statusText}`);
  }

  const json = await parseMcpResponse(res);
  return { json };
}

async function listTools() {
  const { sessionId } = await initializeSession();

  const body = {
    jsonrpc: "2.0",
    id: 2,
    method: "tools/list",
    params: {}
  };

  const { json } = await sendMcpRequest(sessionId, body);
  console.log(JSON.stringify(json, null, 2));
}

async function callTool(name: string, args: Json) {
  const { sessionId } = await initializeSession();

  const body = {
    jsonrpc: "2.0",
    id: 3,
    method: "tools/call",
    params: {
      name,
      arguments: args
    }
  };

  const { json } = await sendMcpRequest(sessionId, body);
  console.log(JSON.stringify(json, null, 2));
}

async function main() {
  const [, , cmd, arg1, arg2] = process.argv;

  if (cmd === "list-tools") {
    await listTools();
  } else if (cmd === "call-tool") {
    if (!arg1) {
      throw new Error("call-tool 사용법: call-tool <toolName> '<jsonArgs>'");
    }
    const toolName = arg1;
    const jsonArgs = arg2 ? JSON.parse(arg2) : {};
    await callTool(toolName, jsonArgs);
  } else {
    console.log(
      "사용법:\n" +
        "  node dist/mcpClient.js list-tools\n" +
        "  node dist/mcpClient.js call-tool <toolName> '<jsonArgs>'"
    );
  }
}

main().catch((err) => {
  console.error("mcpClient error:", err);
  process.exit(1);
});
