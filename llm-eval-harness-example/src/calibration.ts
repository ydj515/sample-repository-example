/**
 * Judge 캘리브레이션.
 * LLM Judge를 채점에 쓰기 전에 Judge 자체를 믿을 수 있는지 측정한다.
 * - agreementRate: 사람이 라벨링한 골드셋과 Judge 판정의 일치율
 * - positionBiasRate: 답변 순서만 바꿨는데 같은 "위치"를 고르는 비율(위치 편향)
 * 실행 예제는 demo/calibration.ts 참고.
 */

import { chat, type ChatFn } from "./ollama.js";
import { pointwiseJudge, pairwiseJudge } from "./judge.js";

/** 골드셋 한 케이스: 질문, 답변, 사람이 매긴 정답 라벨 */
export type GoldCase = {
    question: string;
    answer: string;
    label: "correct" | "incorrect" | "partial";
};

/**
 * 골드셋 전체를 Judge로 채점해 사람 라벨과 일치한 건수를 센다.
 * 케이스를 순차로 호출하므로 로컬 Ollama에 요청이 한 번에 몰리지 않는다.
 */
export async function agreementRate(
    goldSet: GoldCase[],
    rubric: string,
    chatFn: ChatFn = chat,
): Promise<{ agree: number; total: number }> {
    let agree = 0;
    for (const goldCase of goldSet) {
        const { verdict } = await pointwiseJudge(
            goldCase.question,
            goldCase.answer,
            rubric,
            chatFn,
        );
        if (verdict === goldCase.label) agree++;
    }
    return { agree, total: goldSet.length };
}

/**
 * 캘리브레이션 기본 루브릭.
 * 골드셋 질문에는 근거 규정이 함께 들어 있으므로 "질문에 제시된 규정"을 기준으로 판정하게 한다.
 */
export const CALIBRATION_RUBRIC =
    "당신은 사내 규정 FAQ 답변 채점자입니다. 질문에 제시된 규정을 기준으로, 답변이 사실적으로 옳으면 correct, 부분적으로만 옳으면 partial, 틀리면 incorrect로 판정하세요.";

/*

위치 편향 : judge에게 같은 두 답변을 보여줄 댸, 어느 것이 먼저 오느냐에 따라 판정이 달라지는 경향
-> 같은 쌍을 두 배치로 물어보는거
--> 1. A/B, 2. B/A

*/

/**
 * 같은 쌍을 A/B, B/A 두 순서로 판정한다.
 * order1에서 "A" = answerA, order2에서 "A" = answerB 이므로,
 * 두 번 모두 같은 글자(A/A 또는 B/B)를 골랐다면 내용이 아니라 위치를 고른 것으로 본다.
 */
export async function swapPairwise(
    question: string,
    answerA: string,
    answerB: string,
    chatFn: ChatFn = chat,
): Promise<{
    order1: { winner: "A" | "B" | "tie"; reason: string };
    order2: { winner: "A" | "B" | "tie"; reason: string };
    positionBiased: boolean;
}> {
    const order1 = await pairwiseJudge(question, answerA, answerB, chatFn);
    const order2 = await pairwiseJudge(question, answerB, answerA, chatFn);

    const positionBiased =
        (order1.winner === "A" && order2.winner === "A") ||
        (order1.winner === "B" && order2.winner === "B");

    return { order1, order2, positionBiased };
}

/** 위치 편향 측정용 고정 쌍. fixtures/pairs.json은 내용상 우열이 거의 없는 쌍으로 구성되어 있다. */
export type Pair = {
    question: string;
    answerA: string;
    answerB: string;
};

/**
 * 고정 쌍 전체의 위치 편향률을 계산한다.
 * 어느 한 순서라도 tie가 나오면 편향 여부를 판단할 수 없으므로 분모(total)에서 제외한다.
 */
export async function positionBiasRate(
    pairs: Pair[],
    chatFn: ChatFn = chat,
): Promise<{ biased: number; total: number }> {
    let biased = 0;
    let total = 0;
    for (const pair of pairs) {
        const result = await swapPairwise(
            pair.question,
            pair.answerA,
            pair.answerB,
            chatFn,
        );
        if (result.order1.winner === "tie" || result.order2.winner === "tie") {
            continue;
        }
        total++;
        if (result.positionBiased) biased++;
    }
    return { biased, total };
}
