import { z } from "zod/v4";

const InputSchema = z.object({
  message: z.string().describe("Message to process"),
});

type ExampleInput = z.infer<typeof InputSchema>;

class ExampleTool {
  name = "example_tool";
  description = "An example tool that processes messages";
  schema = InputSchema;

  async execute(input: ExampleInput) {
    return `Processed: ${input.message}`;
  }
}

export default ExampleTool;
