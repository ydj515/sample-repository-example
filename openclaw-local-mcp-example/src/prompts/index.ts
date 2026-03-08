import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";

export function registerAllPrompts(server: McpServer) {
  // Simple echo prompt with one required arg
  server.registerPrompt(
    "echo",
    {
      title: "Echo Prompt",
      description: "Creates a prompt to process a message",
      argsSchema: {
        message: z.string()
      }
    },
    ({ message }) => ({
      messages: [
        {
          role: "user",
          content: {
            type: "text",
            text: `Please process this message: ${message}`
          }
        }
      ]
    })
  );

  // Example with optional argument
  server.registerPrompt(
    "summarize",
    {
      title: "Summarize Text",
      description: "Prompt for summarizing provided text",
      argsSchema: {
        text: z.string(),
        maxWords: z.string().optional() // must be string per PromptArgsRawShape
      }
    },
    ({ text, maxWords }) => ({
      messages: [
        {
          role: "user",
          content: {
            type: "text",
            text:
              typeof maxWords === "string" && maxWords.trim().length > 0
                ? `Summarize the following in up to ${maxWords} words:\n\n${text}`
                : `Summarize the following:\n\n${text}`
          }
        }
      ]
    })
  );
}

