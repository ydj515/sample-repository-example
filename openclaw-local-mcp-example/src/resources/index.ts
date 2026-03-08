import { McpServer, ResourceTemplate } from "@modelcontextprotocol/sdk/server/mcp.js";

export function registerAllResources(server: McpServer) {
  // 1) Static resource example: server health/info
  server.registerResource(
    "server-health",
    "health://server",
    {
      title: "Server Health",
      description: "Basic health check for the MCP server",
      mimeType: "text/plain"
    },
    async (uri) => ({
      contents: [
        {
          uri: uri.href,
          text: `OK\nname=mcp-claude-test-server\ntime=${new Date().toISOString()}`
        }
      ]
    })
  );

  // 2) Dynamic resource example: echo with parameter
  server.registerResource(
    "echo",
    new ResourceTemplate("echo://{message}", { list: undefined }),
    {
      title: "Echo Resource",
      description: "Echo back the provided message",
      mimeType: "text/plain"
    },
    async (uri, variables) => {
      const v = (variables as Record<string, string | string[]>)["message"];
      const message = Array.isArray(v) ? v.join(",") : v ?? "";
      return {
        contents: [
          {
            uri: uri.href,
            text: `Resource echo: ${message}`
          }
        ]
      };
    }
  );
}
