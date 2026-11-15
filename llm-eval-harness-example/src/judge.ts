/**
 * LLM-as-a-Judge.
 * - pointwiseJudge: 답변 하나를 루브릭으로 correct / partial / incorrect 판정
 * - pairwiseJudge: 답변 두 개를 비교해 A / B / tie 판정
 * 두 함수 모두 chatFn을 주입받을 수 있어, 테스트에서는 Ollama 대신 가짜 함수를 넣을 수 있다.
 */

// 루브릭 : 어떤 답변이 좋냐, 어떤 답변이 부분점수냐, 어떤 답변이 틀렸냐 정의

/*

당신은 HR 정책 QA 시스템의 답변을 채점합니다.
다음 기준으로 판정하세요.

- correct: 자료의 내용을 정확하게 반영하고, 질문의 핵심에 답했으며, 자료 밖의 내용을 추가하지 않았다.
- partial: 질문에 부분적으로 답했거나, 자료 근거가 불완전하거나, 답이 모호하다.
- incorrect: 자료와 다른 내용을 답했거나, 질문에 전혀 응하지 않았거나, 자료에 없는 내용을 단정적으로 말했다

*/

// 1. 판정 단계의 수
// 2. 각 레이블의 경계가 명확
// 3. 거부 판정

import { z } from "zod";
import { chat, type ChatFn } from "./ollama.js";

// Judge 출력 계약: {"verdict": ..., "reason": ...}
const VerdictSchema = z.object({
    verdict: z.enum(["correct", "incorrect", "partial"]),
    reason: z.string(),
});

type VerdictResult = z.infer<typeof VerdictSchema>;

/**
 * Judge 응답 텍스트를 VerdictSchema로 검증한다.
 * JSON 파싱 실패나 스키마 불일치는 "incorrect"로 폴백한다.
 * (판정 불가를 통과로 처리하지 않기 위한 보수적 선택)
 */
function parseVerdict(text: string): VerdictResult {
    try {
        const r = VerdictSchema.safeParse(JSON.parse(text));
        if (r.success) return r.data;
    } catch { }
    return { verdict: "incorrect" as const, reason: "판정 파싱 실패(폴백)" };
}

/**
 * 답변 하나를 루브릭 기준으로 채점한다.
 * system 프롬프트에 루브릭과 JSON 출력 형식을, user 프롬프트에 질문과 답변을 넣고
 * Ollama JSON 모드(format: "json")로 호출한다.
 */
export async function pointwiseJudge(
    question: string,
    answer: string,
    rubric: string,
    chatFn: ChatFn = chat,
): Promise<{ verdict: "correct" | "incorrect" | "partial"; reason: string }> {
    const systemPrompt =
        `${rubric}\n\nJSON만 출력: {"verdict": "correct" | "incorrect" | "partial", "reason": "판정 이유"}`;
    const userPrompt = `질문: ${question}\n\n답변: ${answer}`;

    const result = await chatFn(
        [
            { role: "system", content: systemPrompt },
            { role: "user", content: userPrompt },
        ],
        { json: true },
    );

    return parseVerdict(result.text);
}

// const rubric = `당신은 HR 정책 QA 시스템의 답변을 채점합니다.
// - correct: 자료를 정확히 반영하고 질문 핵심에 답했으며 자료 밖 내용을 추가하지 않았다.
// - partial: 부분적으로만 답했거나 근거가 불완전하거나 모호하다.
// - incorrect: 자료와 다른 내용을 답했거나 자료에 없는 내용을 단정했다.`;

// const result = await pointwiseJudge(
//     "1년 근속하면 연차 휴가가 며칠 나오나요?",
//     "1년 근속 후 15일의 연차 휴가가 발생합니다.",
//     rubric,
// );

// console.log(result);

// const rubric = `당신은 HR 정책 QA 시스템의 답변을 채점합니다.
// - correct: 자료를 정확히 반영하고 질문 핵심에 답했으며 자료 밖 내용을 추가하지 않았다.
// - partial: 부분적으로만 답했거나 근거가 불완전하거나 모호하다.
// - incorrect: 자료와 다른 내용을 답했거나 자료에 없는 내용을 단정했다.`;

// for (let i = 0; i < 5; i++) {
//     const r = await pointwiseJudge(
//         "1년 근속하면 연차 휴가가 며칠 나오나요?",
//         "1년 근속 후 15일의 연차 휴가가 발생합니다.",
//         rubric,
//     );
//     console.log(`${i + 1}: ${r.verdict}`);
// }

// 쌍 비교 Judge 출력 계약: {"winner": ..., "reason": ...}
const WinnerSchema = z.object({
    winner: z.enum(["A", "B", "tie"]),
    reason: z.string(),
});

type WinnerResult = z.infer<typeof WinnerSchema>;

/**
 * 쌍 비교 응답을 검증한다. 파싱 실패 시 어느 쪽도 이기지 않은 "tie"로 폴백한다.
 * tie는 위치 편향률 계산(calibration.ts)에서 분모에서 제외된다.
 */
function parseWinner(text: string): WinnerResult {
    try {
        const r = WinnerSchema.safeParse(JSON.parse(text));
        if (r.success) return r.data;
    } catch { }
    return { winner: "tie" as const, reason: "판정 파싱 실패(폴백)" };
}

/**
 * 같은 질문에 대한 두 답변 중 더 나은 쪽을 고른다.
 * 프롬프트에 놓인 순서(A가 먼저)가 판정에 영향을 줄 수 있으므로,
 * 신뢰도 확인은 calibration.ts의 swapPairwise로 순서를 바꿔 함께 판정한다.
 */
export async function pairwiseJudge(
    question: string,
    answerA: string,
    answerB: string,
    chatFn: ChatFn = chat,
): Promise<{ winner: "A" | "B" | "tie"; reason: string }> {
    const systemPrompt =
        `두 답변을 비교하여 더 나은 것을 선택하세요.\n\nJSON만 출력: {"winner": "A" | "B" | "tie", "reason": "판정 이유"}`;
    const userPrompt =
        `질문: ${question}\n\n답변 A: ${answerA}\n\n답변 B: ${answerB}`;

    const result = await chatFn(
        [
            { role: "system", content: systemPrompt },
            { role: "user", content: userPrompt },
        ],
        { json: true },
    );

    return parseWinner(result.text);
}
