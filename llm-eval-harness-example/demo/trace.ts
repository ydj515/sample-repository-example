/**
 * 트레이싱 데모 (Ollama 필요).
 * 케이스 하나를 retrieve -> sut-chat -> semantic-embed -> judge 순서로 실행하며 단계별 span을 기록한다.
 * 토큰 회계를 콘솔에 출력하고, artifacts/trace.html에 워터폴 차트를 저장한다.
 *
 * 실행: mise run trace
 */
import { writeFileSync, mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { Tracer, tokenAccount, renderWaterfall } from "../src/tracer.js";
import { retrieve } from "../src/sut.js";
import { chat, embed, SUT_MODEL, JUDGE_MODEL, SEED } from "../src/ollama.js";



const QUESTION = "1년 근속하면 연차 휴가가 며칠 나오나요?";
const RUBRIC =
    "당신은 사내 규정 FAQ 답변 채점자입니다. 질문에 제시된 규정을 기준으로, " +
    "답변이 사실적으로 옳으면 correct, 부분적으로만 옳으면 partial, 틀리면 incorrect로 판정하세요.";

async function main() {
    const tracer = new Tracer("demo-trace-" + Date.now());

    let sutAnswer = "";

    await tracer.span("evaluate-case", async () => {
        // 1. retrieve (동기 함수를 span으로 감쌈)
        const docs = await tracer.span("retrieve", async () => {
            return retrieve(QUESTION);
        });

        // 2. SUT chat
        const ctx = docs.map((d) => `[${d.id}] ${d.text}`).join("\n");
        sutAnswer = await tracer.span("sut-chat", async () => {
            const { text, usage } = await chat(
                [
                    {
                        role: "system",
                        content: `다음 자료를 참고해서 질문에 답하세요.\n\n${ctx}`,
                    },
                    { role: "user", content: QUESTION },
                ],
                { model: SUT_MODEL },
            );
            tracer.setAttribute("promptEvalCount", usage.promptEvalCount);
            tracer.setAttribute("evalCount", usage.evalCount);
            return text;
        });

        // 3. semantic embed (질문 + 답변)
        await tracer.span("semantic-embed", async () => {
            const { vectors, promptEvalCount } = await embed([QUESTION, sutAnswer]);
            tracer.setAttribute("promptEvalCount", promptEvalCount);
            return vectors;
        });

        // 4. judge (루브릭 채점, 직접 chat 호출로 usage 기록)
        await tracer.span("judge", async () => {
            const systemPrompt =
                `${RUBRIC}\n\nJSON만 출력: ` +
                `{"verdict": "correct" | "incorrect" | "partial", "reason": "판정 이유"}`;
            const userPrompt = `질문: ${QUESTION}\n\n답변: ${sutAnswer}`;
            const { text, usage } = await chat(
                [
                    { role: "system", content: systemPrompt },
                    { role: "user", content: userPrompt },
                ],
                { json: true, model: JUDGE_MODEL },
            );
            tracer.setAttribute("promptEvalCount", usage.promptEvalCount);
            tracer.setAttribute("evalCount", usage.evalCount);
            return text;
        });
    });

    // ── 토큰 회계 출력 ────────────────────────────────────────────────────────
    const account = tokenAccount(tracer.spans());
    console.log("=== 토큰 회계 ===");
    console.log(`총 프롬프트 토큰: ${account.totalPromptTokens}`);
    console.log(`총 생성 토큰:     ${account.totalEvalTokens}`);
    console.log("단계별:");
    for (const [name, stats] of Object.entries(account.byName)) {
        if (stats.promptTokens > 0 || stats.evalTokens > 0) {
            console.log(
                `  ${name.padEnd(18)}: prompt=${stats.promptTokens} eval=${stats.evalTokens} calls=${stats.calls}`,
            );
        }
    }

    // ── HTML 워터폴 저장 ──────────────────────────────────────────────────────
    const artifactsDir = fileURLToPath(
        new URL("../artifacts", import.meta.url),
    );
    mkdirSync(artifactsDir, { recursive: true });
    const htmlPath = fileURLToPath(
        new URL("../artifacts/trace.html", import.meta.url),
    );
    const html = renderWaterfall(tracer.spans());
    writeFileSync(htmlPath, html, "utf-8");
    console.log("\n저장: artifacts/trace.html  (file:// 더블클릭으로 열림)");

    // ── JSONL 미리보기 ────────────────────────────────────────────────────────
    const jsonl = tracer.toJSONL();
    const lines = jsonl.split("\n");
    const previewCount = Math.min(lines.length, 3);
    console.log(`\n=== JSONL (처음 ${previewCount}줄 / 전체 ${lines.length}줄) ===`);
    for (const line of lines.slice(0, previewCount)) {
        console.log(line);
    }
}

main().catch((err: unknown) => {
    console.error(err instanceof Error ? err.message : String(err));
    process.exit(1);
});
