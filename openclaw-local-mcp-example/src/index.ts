// src/server.ts
import express, { Request, Response } from "express";
import cors from "cors";
import { randomUUID } from "node:crypto";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import {
  isInitializeRequest,
  CreateMessageRequestSchema,
  ElicitRequestSchema,
  SetLevelRequestSchema,
  SUPPORTED_PROTOCOL_VERSIONS,
  LATEST_PROTOCOL_VERSION,
  DEFAULT_NEGOTIATED_PROTOCOL_VERSION
} from "@modelcontextprotocol/sdk/types.js";
import { registerAllTools } from "./tools/index.js";
import { registerAllResources } from "./resources/index.js";
import { registerAllPrompts } from "./prompts/index.js";

/**
 * 1) MCP 서버 생성 + Tool 등록
 */
function buildMcpServer() {
  console.log("[MCP] SUPPORTED_PROTOCOL_VERSIONS:", SUPPORTED_PROTOCOL_VERSIONS);
  console.log("[MCP] LATEST_PROTOCOL_VERSION:", LATEST_PROTOCOL_VERSION);
  console.log(
    "[MCP] DEFAULT_NEGOTIATED_PROTOCOL_VERSION:",
    DEFAULT_NEGOTIATED_PROTOCOL_VERSION
  );

  const server = new McpServer(
    {
      name: "mcp-claude-test-server",
      version: "1.0.0"
    },
    {
      // Advertise capabilities so Inspector enables tabs/buttons
      capabilities: {
        // Standard capabilities will also be registered automatically
        // by McpServer when we register tools/resources/prompts.
        sampling: {},
        elicitation: {},
        logging: {}
      }
    }
  );

  registerAllTools(server);
  registerAllResources(server);
  registerAllPrompts(server);

  // Provide a minimal sampling provider so Inspector's Sampling tab can call the server
  server.server.setRequestHandler(CreateMessageRequestSchema, async (request) => {
    const userText = request.params.messages
      .map((m) => (m.content.type === "text" ? m.content.text : "[non-text]"))
      .join("\n\n");
    return {
      model: "stub-sampler",
      role: "assistant",
      stopReason: "endTurn",
      content: { type: "text", text: `Echoed by server sampler:\n\n${userText}` }
    };
  });

  // Provide a minimal elicitation handler (client→server). We decline, since typical flow is server→client.
  server.server.setRequestHandler(ElicitRequestSchema, async (_request) => {
    return { action: "decline" } as const;
  });

  // Implement logging/setLevel so advertising 'logging' capability is valid
  let currentLogLevel: string | undefined = undefined;
  server.server.setRequestHandler(SetLevelRequestSchema, async (request) => {
    currentLogLevel = request.params.level;
    // Optionally, send a confirmation log back to the client
    await server.server.sendLoggingMessage({
      level: request.params.level,
      logger: "mcp-claude-test-server",
      data: `Log level set to ${request.params.level}`
    });
    return {};
  });

  return server;
}

/**
 * 2) Express + Streamable HTTP 전송/세션 라우팅
 */
const app = express();
app.use(express.json());
app.use(
  cors({
    origin: "*", // 운영에서는 화이트리스트로 제한
    exposedHeaders: ["Mcp-Session-Id"],
    allowedHeaders: ["Content-Type", "mcp-session-id", "Mcp-Session-Id"]
  })
);

// 세션 저장소
const transports: Record<string, StreamableHTTPServerTransport> = {};

// MCP 요청(POST) - 세션 초기화 및 일반 요청 처리
app.post("/mcp", async (req: Request, res: Response) => {
  const sid = req.header("mcp-session-id"); // string | undefined
  console.log("[MCP] POST /mcp, sid=", sid);
  console.log("[MCP] request body:", JSON.stringify(req.body, null, 2));

  let transport: StreamableHTTPServerTransport | undefined = sid
    ? transports[sid]
    : undefined;

  const isInit = isInitializeRequest(req.body as any);
  console.log("[MCP] isInitializeRequest:", isInit);

  // 세션이 없고 initialize 요청이면 새로 생성
  if (!transport && isInit) {
    console.log("[MCP] creating new session/transport for initialize");
    transport = new StreamableHTTPServerTransport({
      sessionIdGenerator: () => randomUUID(),
      onsessioninitialized: (newSid: string) => {
        console.log("[MCP] session initialized:", newSid);
        transports[newSid] = transport!;
      }
      // enableDnsRebindingProtection: true,
      // allowedHosts: ["127.0.0.1"],
    });

    const server = buildMcpServer();
    await server.connect(transport);

    transport.onclose = async () => {
      console.log("[MCP] transport closed, sessionId=", transport?.sessionId);
      if (transport?.sessionId) delete transports[transport.sessionId];
      await server.close();
    };
  }

  if (!transport) {
    console.log("[MCP] No valid session, returning 400");
    return res.status(400).json({ error: "No valid session" });
  }

  await transport.handleRequest(req, res, req.body);
});

// MCP 알림/스트림(GET)
app.get("/mcp", async (req: Request, res: Response) => {
  const sid = req.header("mcp-session-id");
  console.log("[MCP] GET /mcp, sid=", sid);
  const t: StreamableHTTPServerTransport | undefined = sid
    ? transports[sid]
    : undefined;
  if (!t) return res.status(400).send("Invalid or missing session ID");
  await t.handleRequest(req, res);
});

// MCP 세션 종료(DELETE)
app.delete("/mcp", (req: Request, res: Response) => {
  const sid = req.header("mcp-session-id");
  console.log("[MCP] DELETE /mcp, sid=", sid);
  const t: StreamableHTTPServerTransport | undefined = sid
    ? transports[sid]
    : undefined;
  if (!t) return res.status(400).send("Invalid or missing session ID");
  t.close();
  res.status(204).end();
});

// 헬스체크
app.get("/health", (_req: Request, res: Response) => res.json({ ok: true }));

// 시작
const PORT = Number(process.env.PORT || 3000);
app.listen(PORT, () => {
  console.log(`MCP Streamable HTTP server listening on :${PORT}`);
});
