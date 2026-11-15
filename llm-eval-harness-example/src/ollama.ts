/**
 * 로컬 Ollama HTTP API 클라이언트.
 * - chat: /api/chat 호출. SUT 답변 생성과 Judge 판정에 공용으로 쓴다.
 * - embed: /api/embed 호출. 의미 유사도 채점용 임베딩을 만든다.
 * 재현성을 위해 temperature 0, 고정 seed로 호출한다.
 */

/** Ollama 서버 주소 */
const OLLAMA = "http://localhost:11434";
/** 샘플링 시드. temperature > 0일 때 재현성을 높이는 용도다. */
export const SEED = 7;
/** 평가 대상 시스템(SUT)이 답변 생성에 쓰는 모델 */
export const SUT_MODEL = "llama3.2";
/** 채점자(Judge) 모델. SUT와 같은 모델이면 자기 답변을 선호하는 편향이 생길 수 있다. */
export const JUDGE_MODEL = "llama3.2";
/** 의미 유사도 채점용 임베딩 모델 */
export const EMBED_MODEL = "bge-m3";

export type ChatMessage = { role: "system" | "user" | "assistant"; content: string };
export type Usage = { promptEvalCount: number; evalCount: number; totalDurationMs: number };
// promptEvalCount : 입력 토큰 수
// evalCount : 생성된 토큰 수
// totalDurationMs : 전체 처리 시간

/** judge/calibration에서 Ollama 대신 가짜 함수를 주입할 때 쓰는 시그니처 */
export type ChatFn = typeof chat;

/**
 * 채팅 완성을 요청하고 응답 텍스트와 토큰 사용량을 반환한다.
 * @param opts.json true면 Ollama JSON 모드(format: "json")로 호출한다. Judge 판정용.
 * @param opts.model 생략하면 JUDGE_MODEL을 쓴다.
 * @throws HTTP 오류 또는 네트워크 오류(Ollama 미실행 등)
 */
export async function chat(
    messages: ChatMessage[],
    opts: { json?: boolean; model?: string } = {},
): Promise<{ text: string; usage: Usage }> {
    const body: Record<string, unknown> = {
        model: opts.model ?? JUDGE_MODEL,
        messages,
        stream: false, // 전체 응답을 한 번에 받는다(토큰 수도 이때 함께 내려온다)
        options: { temperature: 0, seed: SEED },
        // temperature greedy 디코딩 -> 같은 입력과 같은 모델 가중치라면, 같은 빌드 안에서는 항상 같은 선택 경로를 따른다.
        // seed : greedy 디코딩에서는 영향이 X, temperature > 0일 떄 확률 기반 샘플링에 시드를 고정해서 재현성을 높이는 방식
    };
    if (opts.json) body.format = "json";
    try {
        const res = await fetch(`${OLLAMA}/api/chat`, {
            method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body),
        });
        if (!res.ok) throw new Error(`Ollama chat 실패: ${res.status}`);
        const data = (await res.json()) as {
            message: { content: string }; prompt_eval_count: number; eval_count: number; total_duration: number;
        };
        return {
            text: data.message.content,
            // total_duration은 나노초 단위이므로 밀리초로 변환한다.
            usage: { promptEvalCount: data.prompt_eval_count, evalCount: data.eval_count, totalDurationMs: data.total_duration / 1e6 },
        };
    } catch (error: unknown) {
        if (error instanceof Error) throw error;
        throw new Error("Ollama chat 알 수 없는 오류: " + String(error));
    }
}

/**
 * 여러 텍스트를 한 번에 임베딩한다. 반환 벡터 순서는 입력 순서와 같다.
 * @throws HTTP 오류 또는 네트워크 오류(Ollama 미실행 등)
 */
export async function embed(texts: string[]): Promise<{ vectors: number[][]; promptEvalCount: number }> {
    try {
        const res = await fetch(`${OLLAMA}/api/embed`, {
            method: "POST", headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ model: EMBED_MODEL, input: texts }),
        });
        if (!res.ok) throw new Error(`Ollama embed 실패: ${res.status}`);
        const data = (await res.json()) as { embeddings: number[][]; prompt_eval_count: number };
        return { vectors: data.embeddings, promptEvalCount: data.prompt_eval_count };
    } catch (error: unknown) {
        if (error instanceof Error) throw error;
        throw new Error("Ollama embed 알 수 없는 오류: " + String(error));
    }
}
