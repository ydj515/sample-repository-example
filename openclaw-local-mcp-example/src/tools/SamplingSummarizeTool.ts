import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";

const InputSchema = z.object({
  text: z.string().describe("Text to summarize"),
  maxTokens: z
    .number()
    .int()
    .min(1)
    .max(4000)
    .optional()
    .describe("Max tokens for the summary (default 300)")
});

type SamplingSummarizeInput = z.infer<typeof InputSchema>;

class SamplingSummarizeTool {
  name = "sampling_summarize";

  description =
    "Summarize text using the client's LLM (MCP sampling).";

  schema = InputSchema;

  private server: McpServer;

  constructor(server: McpServer) {
    this.server = server;
  }

  async execute({ text, maxTokens }: SamplingSummarizeInput) {
    const response = await this.server.server.createMessage({
      messages: [
        {
          role: "user",
          content: {
            type: "text",
            text: `Summarize concisely:\n\n${text}`
          }
        }
      ],
      maxTokens: maxTokens ?? 300
    });

    const out = response.content;
    console.log(out);
    const textContent =
      out.type === "text" ? out.text : JSON.stringify(response, null, 2);

    return {
      content: [{ type: "text", text: textContent }]
    };
  }
}

export default SamplingSummarizeTool;
