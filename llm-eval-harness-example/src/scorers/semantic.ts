/**
 * 의미 유사도 채점기.
 * 기준 답(reference)과 표현은 달라도 뜻이 같은 답변을 점수로 인정하기 위해 쓴다.
 */

// bge-m3는 어떤 텍스트든 1024개의 차원 벡터로 표현
// -> 벡터는 의미가 비슷한 텍스트들을 벡터 공간에서 비슷한 방향을 가르킨다.

// -> 방향이 비슷하다 = 코사인 유사도
// --> 코사인 유사도 : dot(a, b) / (|a| * |b|)

import { embed } from "../ollama.js";

/**
 * 두 벡터의 코사인 유사도(-1 ~ 1)를 계산한다.
 * 전제: 두 벡터의 길이가 같아야 한다. 영벡터가 들어오면 0으로 나누어 NaN이 된다.
 */
export function cosine(a: number[], b: number[]): number {
    let dot = 0;
    let normA = 0;
    let normB = 0;
    for (let i = 0; i < a.length; i++) {
        // noUncheckedIndexedAccess: 길이가 같다는 전제이므로 non-null 단언
        const x = a[i]!;
        const y = b[i]!;
        dot += x * y;
        normA += x * x;
        normB += y * y;
    }
    return dot / (Math.sqrt(normA) * Math.sqrt(normB));
}

// console.log(cosine([1, 0], [0, 1]));   // 직교
// console.log(cosine([1, 0], [1, 0]));   // 동일
// console.log(cosine([1, 0], [-1, 0]));  // 반대

/**
 * 답변과 기준 답을 한 번의 embed 호출로 임베딩한 뒤 코사인 유사도를 반환한다.
 * 점수의 절대값은 임베딩 모델에 따라 달라지므로, 통과 기준(임계값)은 모델별로 정해야 한다.
 */
export async function semanticScore(answer: string, reference: string): Promise<number> {
    const { vectors } = await embed([answer, reference]);
    const [answerVec, referenceVec] = vectors;
    if (!answerVec || !referenceVec) {
        throw new Error(`semanticScore: expected 2 embeddings, got ${vectors.length}`);
    }
    return cosine(answerVec, referenceVec);
}

// const related = await semanticScore(
//     "근무 중 개인 사유로 자리를 비울 때 어떻게 처리하나요?",
//     "재택근무는 주 최대 2일까지 허용됩니다."
// );

// const unrelated = await semanticScore(
//     "근무 중 개인 사유로 자리를 비울 때 어떻게 처리하나요?",
//     "사내 카페테리아는 오전 8시부터 오후 7시까지 운영합니다."
// );

// console.log(related, unrelated);
