import { z } from "zod";

/*
    채점에는 두가지 방향이 있어요.
    
    1. 기준 답이 있는 방향 -> "이 질문에는 이 답이 맞아"
    --> "1년 근속 시 연차는 몇 일인가요?" -> 답 : "15일"

    2. 기준 답이 없는 방향 -> "정답이 뭔지 미리 정하지 않아도, 결과만보고 채점"
    --> "JSON 형태로 반환해야 한다.", "각 자료에 없는 질문이면 거부해라"
*/

export type ScorerResult = { pass: boolean; detail?: string };
// pass : 채점 통과 유무
// detail : 선택 필요 -> 실패 에러 설명문


// 정확 일치
export function exactMatch(answer: string, expected: string): ScorerResult {
    return { pass: answer.trim() === expected.trim() };
}

// 정규식 매칭
export function regexMatch(answer: string, pattern: RegExp): ScorerResult {
    return { pass: pattern.test(answer) };
}

// /\d+/ : 답에 숫자가 있어야 한다.
// /\d+일\ : "며칠"에 해당하는 숫자


// console.log(exactMatch(" 5 ", "5")); -> pass
// console.log(exactMatch("Hello", "hello")); -> fail
// console.log(regexMatch("총 391원", /\d+원/)); -> pass
// console.log(regexMatch("총 삼백구십일원", /\d+원/)); -> fail

// JSON Schema 계약 검증 함수
export function jsonSchemaValid(answer: string, schema: z.ZodType): ScorerResult {
    try {
        return { pass: schema.safeParse(JSON.parse(answer)).success };
    } catch {
        return { pass: false, detail: "JSON 파싱 실패" };
    }
}

// const leaveSchema = z.object({
//     days: z.number(),
//     deadline: z.string(),
// });

// console.log(jsonSchemaValid('{"days": 15, "deadline": "3일 전"}', leaveSchema)); // pass
// console.log(jsonSchemaValid('{"days": "fifteen"}', leaveSchema)); // fail
// console.log(jsonSchemaValid("네, 아래와 같습니다: 연차는 15일", leaveSchema)); // fail
// console.log(jsonSchemaValid("", leaveSchema)); // fail

// const listSchema = z.array(z.string());

// console.log(jsonSchemaValid('["연차", "재택근무"]', listSchema)); // true
// console.log(jsonSchemaValid('[1, 2, 3]', listSchema)); // fail

/**
 * 필수 키워드가 답변에 모두 들어 있는지 확인한다(부분 문자열 일치).
 * 실패 시 detail에 빠진 키워드 목록을 담는다. 빈 배열이면 항상 통과한다.
 */
export function keywordPresent(answer: string, keywords: string[]): ScorerResult {
    const missing = keywords.filter((kw) => !answer.includes(kw));
    if (missing.length === 0) return { pass: true };
    return { pass: false, detail: `빠진 키워드: ${missing.join(", ")}` };
}

// console.log(keywordPresent("연차 휴가는 15일입니다", ["연차", "15"])); // true
// console.log(keywordPresent("연차 휴가는 15일입니다", ["연차", "신청"])); // fail
// console.log(keywordPresent("오늘 날씨가 맑습니다", ["연차", "휴가", "규정"])); // fail
// console.log(keywordPresent("아무 내용이든", [])); // true

/**
 * 답변이 기대 문서 id(doc-1 등) 중 하나 이상을 인용했는지 확인한다.
 * 기대 문서가 없으면 인용 여부를 판단할 수 없으므로 실패로 처리한다.
 */
export function citationPresent(answer: string, docIds: string[]): ScorerResult {
    if (docIds.length === 0) return { pass: false };
    const found = docIds.some((id) => answer.includes(id));
    return { pass: found };
}

// console.log(citationPresent("doc-1에 따르면 연차는 15일입니다", ["doc-1"])); // true
// console.log(citationPresent("출처: doc-1 및 doc-3 참고", ["doc-2", "doc-1"])); // true
// console.log(citationPresent("연차는 15일입니다", ["doc-1", "doc-2"])); // false
// console.log(citationPresent("doc-1 참고", [])); // fail

// 자료에 없는 질문에 대해 "거부"로 인정하는 표현 목록
// PROMPT_V2가 지시하는 "자료에 없습니다"를 포함한다.
const REFUSAL_PHRASES = [
    "자료에 없습니다",
    "답할 수 없습니다",
    "모르겠습니다",
];

/**
 * 답변에 거부 표현이 포함되어 있는지 확인한다.
 * expectRefusal 케이스(자료에 답이 없는 질문)에서만 사용한다.
 */
export function refusalDetected(answer: string): ScorerResult {
    const matched = REFUSAL_PHRASES.some((phrase) => answer.includes(phrase));
    return { pass: matched };
}

// console.log(refusalDetected("자료에 없습니다")); // true
// console.log(refusalDetected("그 질문에는 답할 수 없습니다")); // true
// console.log(refusalDetected("저는 모르겠습니다")); // true
// console.log(refusalDetected("연차는 15일입니다")); // false
// console.log(refusalDetected("이 회사의 연차 정책은 입사 1년 후 15일이 부여됩니다"));// false