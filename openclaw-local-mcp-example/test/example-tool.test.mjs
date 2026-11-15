import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import ExampleTool from "../dist/tools/ExampleTool.js";

test("the CLI retains its Node shebang", async () => {
  assert((await readFile(new URL("../dist/index.js", import.meta.url), "utf8")).startsWith("#!/usr/bin/env node\n"));
});

test("the example retains its input validation and execution result", async () => {
  const tool = new ExampleTool();
  assert.equal(await tool.execute(tool.schema.parse({ message: "fixture" })), "Processed: fixture");
  assert.throws(() => tool.schema.parse({}));
  assert.throws(() => tool.schema.parse({ message: 42 }));
});
