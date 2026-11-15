import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { once } from "node:events";
import { createServer } from "node:net";
import { setTimeout as delay } from "node:timers/promises";
import test from "node:test";
import { gzipSync, deflateSync } from "node:zlib";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StreamableHTTPClientTransport } from "@modelcontextprotocol/sdk/client/streamableHttp.js";
import { CreateMessageResultSchema } from "@modelcontextprotocol/sdk/types.js";

test("SDK upgrade preserves discovery, prompt validation, and sampling content", { timeout: 30000 }, async (t) => {
  const probe = createServer();
  probe.listen(0, "127.0.0.1");
  await once(probe, "listening");
  const { port } = probe.address();
  await new Promise((resolve) => probe.close(resolve));

  const process = spawn(globalThis.process.execPath, ["dist/index.js"], {
    cwd: new URL("../", import.meta.url),
    env: { ...globalThis.process.env, PORT: String(port), DATA_PORTAL_API_KEY: "" },
    stdio: ["ignore", "ignore", "pipe"],
  });
  let stderr = "";
  process.stderr.on("data", (chunk) => { stderr += chunk; });
  const client = new Client({ name: "sdk-compatibility-test", version: "1.0.0" });
  t.after(async () => {
    await client.close();
    if (process.exitCode === null && process.signalCode === null) {
      const exited = once(process, "exit");
      process.kill("SIGTERM");
      await exited;
    }
  });

  const base = `http://127.0.0.1:${port}`;
  let ready = false;
  for (let attempt = 0; attempt < 100; attempt++) {
    assert.equal(process.exitCode, null, stderr);
    try {
      const response = await fetch(`${base}/health`);
      if (response.ok) { ready = true; break; }
    } catch {}
    await delay(50);
  }
  assert(ready, stderr || "The local example server did not become ready");
  const oversized = Buffer.from(JSON.stringify({ value: "x".repeat(120000) }));
  for (const [encoding, compress] of [["gzip", gzipSync], ["deflate", deflateSync]]) {
    const response = await fetch(`${base}/mcp`, {
      method: "POST",
      headers: { "content-type": "application/json", "content-encoding": encoding },
      body: compress(oversized),
    });
    assert.equal(response.status, 413);
  }
  await client.connect(new StreamableHTTPClientTransport(new URL(`${base}/mcp`)));

  const { tools } = await client.listTools();
  assert.equal(tools.length, 3);
  assert(tools.some(({ name }) => name === "sampling_summarize"));
  const { prompts } = await client.listPrompts();
  assert.deepEqual(prompts.map(({ name }) => name).sort(), ["echo", "summarize"]);
  const prompt = await client.getPrompt({ name: "echo", arguments: { message: "fixture" } });
  assert.equal(prompt.messages[0].content.text, "Please process this message: fixture");
  await assert.rejects(client.getPrompt({ name: "echo", arguments: {} }));

  for (const content of [
    { type: "text", text: "single" },
    [{ type: "text", text: "first" }, { type: "text", text: "second" }],
  ]) {
    const result = await client.request({
      method: "sampling/createMessage",
      params: { messages: [{ role: "user", content }], maxTokens: 32 },
    }, CreateMessageResultSchema);
    assert.equal(result.content.type, "text");
    assert.equal(result.content.text, `Echoed by server sampler:\n\n${Array.isArray(content) ? "first\nsecond" : "single"}`);
  }
});
