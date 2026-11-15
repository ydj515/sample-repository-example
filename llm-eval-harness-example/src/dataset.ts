/**
 * 평가셋과 스냅샷.
 * - 평가셋(fixtures/eval-set.json): 질문과 채점 기준(키워드, 인용, 거부, 기준 답)
 * - 스냅샷(fixtures/snapshots/*.json): 특정 프롬프트 버전으로 평가셋을 돌린 결과
 * 스냅샷을 파일로 남겨 두면 regression.ts에서 Ollama 없이 버전 간 비교를 할 수 있다.
 */
import { z } from "zod";
import { readFileSync, mkdirSync, writeFileSync } from "node:fs"
import { dirname } from "node:path"
import { runSUT } from "./sut.js";
import { keywordPresent, citationPresent, refusalDetected } from "./scorers/deterministic.js";
import { semanticScore } from "./scorers/semantic.js";
import { pointwiseJudge } from "./judge.js";
import { SUT_MODEL } from "./ollama.js";

const EvalCaseSchema = z.strictObject({
    id: z.string().min(1), // 케이스를 구별하는 고유한 식별자
    question: z.string().min(1), // SUT에 넣을 질문
    reference: z.string().optional(), // 의미 유사도 채점에 쓸 기준 값
    keywords: z.array(z.string()).optional(), // 결과 값에 반드시 들어가있어야 하는 단어의 목록
    mustCite: z.array(z.string()).optional(), // 답이 참조해야 하는 문서 id 목록
    expectRefusal: z.boolean().optional(), // 우리가 보는 문서에 거부가 정답인 케이스 표시
    triage: z.string().optional(), // 실패 유형을 분류하는 태그
});

export type EvalCase = z.infer<typeof EvalCaseSchema>;

// 케이스 하나의 실행 결과. scores는 평가셋에 해당 기준이 있을 때만 채워진다.
const CaseResultSchema = z.strictObject({
    id: z.string().min(1),
    answer: z.string(),
    scores: z.strictObject({
        keyword: z.boolean().optional(),
        citation: z.boolean().optional(),
        refusal: z.boolean().optional(),
        semantic: z.number().optional(),
    }),
    verdict: z.enum(["correct", "incorrect", "partial"]),
    reason: z.string(),
});

// 스냅샷 파일 형식: 어떤 버전·모델로 언제 만들었는지와 케이스별 결과
const SnapshotSchema = z.strictObject({
    version: z.string().min(1),
    model: z.string().min(1),
    createdAt: z.string().min(1),
    cases: z.array(CaseResultSchema),
});

export type CaseResult = z.infer<typeof CaseResultSchema>;
export type Snapshot = z.infer<typeof SnapshotSchema>;

const DEFAULT_PATH = new URL("../fixtures/eval-set.json", import.meta.url);

const SNAPSHOT_RUBRIC =
    "당신은 사내 규정 FAQ 답변 채점자입니다. 질문에 제시된 규정을 기준으로, 답변이 사실적으로 옳으면 correct, 부분적으로만 옳으면 partial, 틀리면 incorrect로 판정하세요.";

/**
 * 평가셋 전체를 지정한 프롬프트 버전으로 실행해 스냅샷을 만든다.
 * 케이스마다 SUT 답변 생성 -> 결정론적 채점 -> 의미 유사도 -> Judge 판정 순서로 진행한다.
 * 케이스를 순차로 처리하므로 실행 시간은 케이스 수에 비례한다.
 */
export async function generateSnapshot(version: "v1" | "v2"): Promise<Snapshot> {
    const cases: CaseResult[] = [];
    for (const evalCase of loadEvalSet()) {
        const answer = await runSUT(evalCase.question, version); // 결과(답)을 생성

        const scores: CaseResult["scores"] = {};
        if (evalCase.keywords) {
            scores.keyword = keywordPresent(answer, evalCase.keywords).pass;
        }
        if (evalCase.mustCite) {
            scores.citation = citationPresent(answer, evalCase.mustCite).pass;
        }
        if (evalCase.expectRefusal) {
            scores.refusal = refusalDetected(answer).pass;
        }

        if (evalCase.reference) {
            scores.semantic = await semanticScore(answer, evalCase.reference);
        }

        // regex, json, exact -> 우리가 다루는 SUT는 JSON 같은 고정 형식 출력을 내지 않기 떄문

        const { verdict, reason } = await pointwiseJudge(
            evalCase.question,
            answer,
            SNAPSHOT_RUBRIC,
        );

        cases.push({ id: evalCase.id, answer, scores, verdict, reason });
    }

    return {
        version,
        model: SUT_MODEL,
        createdAt: new Date().toISOString(),
        cases,
    };
}


/** 스냅샷을 JSON 파일로 저장한다. 상위 디렉터리가 없으면 만든다. */
export function saveSnapshot(s: Snapshot, path: string): void {
    mkdirSync(dirname(path), { recursive: true });
    writeFileSync(path, JSON.stringify(s, null, 2) + "\n");
}

/** 스냅샷 파일을 읽고 스키마로 검증한다. 형식이 다르면 ZodError를 던진다. */
export function loadSnapshot(path: string): Snapshot {
    return SnapshotSchema.parse(JSON.parse(readFileSync(path, "utf-8")));
}

/** 평가셋 원본을 스키마로 검증하고 id 중복을 거부한다. */
export function parseEvalSet(raw: unknown): EvalCase[] {
    const cases = z.array(EvalCaseSchema).parse(raw);
    const seen = new Set<string>();
    for (const evalCase of cases) {
        if (seen.has(evalCase.id)) {
            throw new Error(`평가셋 id 중복: ${evalCase.id}`);
        }
        seen.add(evalCase.id);
    }
    return cases;
}

/** 평가셋 파일을 읽는다. 경로를 생략하면 fixtures/eval-set.json을 쓴다. */
export function loadEvalSet(filePath?: string): EvalCase[] {
    const target = filePath ?? DEFAULT_PATH;
    return parseEvalSet(JSON.parse(readFileSync(target, "utf-8")));
}