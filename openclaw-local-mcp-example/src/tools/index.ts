import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z, ZodType } from "zod";
import { DATA_PORTAL_API_KEY } from "../config.js";

import DustTool from "./DustTool.js";
import WeatherForcastShortTermTool from "./WeatherForcastShortTerm.js";
import SamplingSummarizeTool from "./SamplingSummarizeTool.js";

const tools = (server: McpServer) => [
  new DustTool(DATA_PORTAL_API_KEY),
  new WeatherForcastShortTermTool(DATA_PORTAL_API_KEY),
  new SamplingSummarizeTool(server),
];

export function registerAllTools(server: McpServer) {
  const activeTools = tools(server);

  for (const tool of activeTools) {
    let inputSchema: Record<string, ZodType> = {};

    if (tool.schema instanceof z.ZodObject) {
      inputSchema = tool.schema.shape;
    } else {
      // Handle the specific structure of ExampleTool's schema
      for (const key in tool.schema as any) {
        const prop = (tool.schema as any)[key];
        if (
          prop.type &&
          typeof prop.type.describe === "function" &&
          prop.description
        ) {
          inputSchema[key] = prop.type.describe(prop.description);
        }
      }
    }

    const implementation = async (
      args: any,
      _extra: any // Using 'any' as the type for the extra parameter is not available for import.
    ): Promise<any> => {
      try {
        const result = await tool.execute(args);
        const text =
          typeof result === "string" ? result : JSON.stringify(result, null, 2);
        return {
          content: [{ type: "text", text }]
        };
      } catch (error: any) {
        return {
          isError: true,
          content: [
            { type: "text", text: `Tool execution failed: ${error.message}` }
          ]
        };
      }
    };

    server.registerTool(
      tool.name,
      {
        description: tool.description,
        inputSchema
      },
      implementation
    );
  }
}
