/**
 * 평가 대상 시스템(SUT: System Under Test).
 * 사내 HR 규정 문서(fixtures/docs.ts)를 검색해 LLM으로 답하는 단순 RAG 파이프라인이다.
 * 프롬프트 버전(v1, v2)을 바꿔 가며 평가 결과가 어떻게 달라지는지 비교하는 것이 목적이다.
 */
import { chat, SUT_MODEL } from "./ollama.js";
import type { ChatMessage } from "./ollama.js";
import { DOCS } from "../fixtures/docs.js";

export type Doc = { id: string; text: string };

/**
 * 질문과 문서의 키워드(토큰) 겹침 개수로 점수를 매겨 상위 2개 문서를 반환한다.
 * 임베딩 검색이 아닌 단순 문자열 비교이므로 결과가 항상 같다(결정적).
 */
export function retrieve(question: string): Doc[] {
    const qTokens = tokenize(question);
    const scored = DOCS.map((doc) => {
        const docTokens = tokenize(doc.text);
        const overlap = qTokens.filter((t) => docTokens.includes(t)).length;
        return { doc, overlap };
    });
    // 내림차순, 동점 시 원래 순서 유지 (stable sort)
    scored.sort((a, b) => b.overlap - a.overlap);
    return scored.slice(0, 2).map((s) => s.doc);
}

/**
 * 구두점을 공백으로 바꾸고 공백 기준으로 나눈 뒤 2글자 이상 토큰만 남긴다.
 * 형태소 분석을 하지 않으므로 "연차는"과 "연차"는 다른 토큰으로 취급된다.
 */
function tokenize(text: string): string[] {
    return text
        .replace(/[.,?!:·。、()\[\]]/g, " ")
        .split(/\s+/)
        .filter((t) => t.length >= 2);

}

/** v1(순진한 프롬프트): "다음 자료를 참고해서 답하라"만 지시한다. 자료 밖 내용 생성을 막지 않는다. */
export function PROMPT_V1(question: string, context: Doc[]): ChatMessage[] {
    const ctx = context.map((d) => `[${d.id}] ${d.text}`).join("\n");
    return [
        {
            role: "system",
            content: `다음 자료를 참고해서 질문에 답하세요.\n\n${ctx}`,
        },
        { role: "user", content: question },
    ];
}

/** v2(근거 기반 프롬프트): 자료 외 내용 생성을 금지하고, 자료에 답이 없으면 "자료에 없습니다"로 거부하게 한다. */
export function PROMPT_V2(question: string, context: Doc[]): ChatMessage[] {
    const ctx = context.map((d) => `[${d.id}] ${d.text}`).join("\n");
    return [
        {
            role: "system",
            content: `자료 안의 내용만 사용해서 질문에 답하세요. 자료에 없는 내용은 추측하거나 생성하지 마세요. 자료에 답이 없으면 반드시 "자료에 없습니다"라고만 답하세요.\n\n${ctx}`,
        },
        { role: "user", content: question },
    ];
}

/** SUT 실행: retrieve -> 프롬프트 버전 선택 -> chat 호출 -> 답변 텍스트 반환 */
export async function runSUT(
    question: string,
    version: "v1" | "v2",
): Promise<string> {
    const context = retrieve(question);
    const messages =
        version === "v1"
            ? PROMPT_V1(question, context)
            : PROMPT_V2(question, context);
    const { text } = await chat(messages, { model: SUT_MODEL });
    return text;
}
