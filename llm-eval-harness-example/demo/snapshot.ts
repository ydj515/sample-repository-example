/**
 * 스냅샷 생성 데모 (Ollama 필요).
 * 평가셋 전체를 지정한 프롬프트 버전으로 실행해 fixtures/snapshots/{version}.json에 저장한다.
 * 기존 스냅샷 파일을 덮어쓴다.
 *
 * 실행: mise run snapshot:v1 또는 mise run snapshot:v2
 */
import { fileURLToPath } from "node:url";
import { generateSnapshot, saveSnapshot } from "../src/dataset.js";
import type { CaseResult } from "../src/dataset.js";

function parseVersion(raw: string | undefined): "v1" | "v2" {
    const version = raw ?? "v1";
    if (version !== "v1" && version !== "v2") {
        throw new Error(`알 수 없는 버전: ${version} (v1 또는 v2)`);
    }
    return version;
}

const VERSION = parseVersion(process.argv[2]);

const OUT_PATH = fileURLToPath(
    new URL(`../fixtures/snapshots/${VERSION}.json`, import.meta.url),
);

function summarizeScores(scores: CaseResult["scores"]): string {
    const parts: string[] = [];
    if (scores.keyword !== undefined) parts.push(`keyword ${scores.keyword ? "pass" : "fail"}`);
    if (scores.citation !== undefined) parts.push(`citation ${scores.citation ? "pass" : "fail"}`);
    if (scores.refusal !== undefined) parts.push(`refusal ${scores.refusal ? "pass" : "fail"}`);
    if (scores.semantic !== undefined) parts.push(`semantic ${scores.semantic.toFixed(3)}`);
    return parts.join(" / ");
}

async function main() {
    console.log(`=== ${VERSION} 스냅샷 생성 ===`);
    const snapshot = await generateSnapshot(VERSION);
    saveSnapshot(snapshot, OUT_PATH);
    console.log(`저장: fixtures/snapshots/${VERSION}.json (${snapshot.cases.length}케이스)\n`);

    for (const c of snapshot.cases) {
        console.log(`${c.id}  ${c.verdict.padEnd(9)}  ${summarizeScores(c.scores)}`);
    }

    const byVerdict = { correct: 0, incorrect: 0, partial: 0 };
    for (const c of snapshot.cases) byVerdict[c.verdict]++;
    const pass = (key: "keyword" | "citation" | "refusal") =>
        snapshot.cases.filter((c) => c.scores[key] === true).length;
    const total = (key: "keyword" | "citation" | "refusal") =>
        snapshot.cases.filter((c) => c.scores[key] !== undefined).length;

    console.log(
        `\nverdict: correct ${byVerdict.correct} / partial ${byVerdict.partial} / incorrect ${byVerdict.incorrect}`,
    );
    console.log(
        `keyword ${pass("keyword")}/${total("keyword")} · citation ${pass("citation")}/${total("citation")} · refusal ${pass("refusal")}/${total("refusal")}`,
    );
}

main().catch((error: unknown) => {
    console.error(error instanceof Error ? error.message : String(error));
    process.exit(1);
});
