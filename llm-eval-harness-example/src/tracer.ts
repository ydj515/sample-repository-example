/*
    우리가 기록하고자 하는거 -> 실행 단계
    -> retrieve 함수가 언제 시작 - 언제 끝나
    -> sut관련된 chat 호출이 어떤 속성

    span : 이름, 시작 시간, 종료 시각, 임의 메타데이터(속성)
*/

export type Span = {
    traceId: string; // 하나의 실행 흐름 전체를 식별하는 값
    spanId: string; // span 관점에서의 하나의 식별 값
    parentId: string | null; // span을 감싸는 부모 span의 spandId
    name: string; // span에 붙이는 이름 (retrieve, sut-chat)
    startMs: number;
    endMs: number;
    attributes: Record<string, number | string>; // 메타데이터
};

export class Tracer {
    private readonly traceId: string;
    private counter = 0;
    private readonly spanList: Span[] = [];
    private readonly stack: Span[] = [];  // 현재 열린 span 스택

    constructor(traceId: string) {
        this.traceId = traceId;
    }

    // fn 실행을 span으로 감싼다. parent 스택 관리. fn 반환값 그대로 반환.
    async span<T>(
        name: string,
        fn: () => Promise<T>,
        attributes?: Record<string, number | string>,
    ): Promise<T> {
        const spanId = `s${++this.counter}`; // s1 -> s2 -> s3
        const parentId =
            this.stack.length > 0 ? this.stack[this.stack.length - 1]!.spanId : null;
        const startMs = Date.now();

        const current: Span = {
            traceId: this.traceId,
            spanId,
            parentId,
            name,
            startMs,
            endMs: 0,
            attributes: attributes ? { ...attributes } : {},
        };

        this.stack.push(current);
        try {
            return await fn();
        } finally {
            current.endMs = Date.now();
            this.stack.pop();
            this.spanList.push(current);
        }
    }

    // 완료된 span 목록(완료 순서)
    spans(): Span[] {
        return this.spanList;
    }

    // span당 한 줄 JSON(JSONL)
    toJSONL(): string {
        if (this.spanList.length === 0) return "";
        return this.spanList.map((s) => JSON.stringify(s)).join("\n");
    }

    // 프롬프트 카운트 -> 입력으로 들어간 프롬프트를 모델이 인코딩하면서, 사용한 토큰 수
    // -> 프롬프트 토큰 수는 같은 입력이 주어지면, 항상 같은 값이

    // eval_count -> 모델이 실제로 생성한 답변에 대한 토큰 수
    // -> 모델이 실제로 만들어 낸 답변
    setAttribute(key: string, value: number | string): void {
        if (this.stack.length === 0) return;
        this.stack[this.stack.length - 1]!.attributes[key] = value;
    }
}

// const tracer = new Tracer("trace-1");

// await tracer.span("parent", async () => {
//     await tracer.span("child", async () => {
//         await tracer.span("grandchild", async () => {
//             // leaf
//         });
//     });
// });

// const spans = tracer.spans();

// const sutAnswer = await tracer.span("sut-chat", async () => {
//     const { text, usage } = await chat(messages, { model: SUT_MODEL });
//     tracer.setAttribute("promptEvalCount", usage.promptEvalCount);
//     tracer.setAttribute("evalCount", usage.evalCount);
//     return text;
// });

// await tracer.span("semantic-embed", async () => {
//     const { vectors, promptEvalCount } = await embed([question, sutAnswer]);
//     tracer.setAttribute("promptEvalCount", promptEvalCount);
//     return vectors;
// });

/**
 * span attributes의 promptEvalCount(입력 토큰), evalCount(생성 토큰)를 합산한다.
 * 전체 합계와 span 이름별 합계·호출 수를 함께 반환한다.
 * 토큰 attribute가 없는 span(retrieve 등)은 0으로 집계하되 호출 수에는 포함한다.
 */
export function tokenAccount(spans: Span[]): {
    totalPromptTokens: number;
    totalEvalTokens: number;
    byName: Record<string, { promptTokens: number; evalTokens: number; calls: number }>;
} {
    let totalPromptTokens = 0;
    let totalEvalTokens = 0;
    const byName: Record<
        string,
        { promptTokens: number; evalTokens: number; calls: number }
    > = {};

    for (const span of spans) {
        const rawPrompt = span.attributes["promptEvalCount"];
        const rawEval = span.attributes["evalCount"];

        // 숫자가 아닌 attribute는 무시(0 취급)
        const prompt = typeof rawPrompt === "number" ? rawPrompt : 0;
        const eval_ = typeof rawEval === "number" ? rawEval : 0;

        totalPromptTokens += prompt;
        totalEvalTokens += eval_;

        if (byName[span.name] === undefined) {
            byName[span.name] = { promptTokens: 0, evalTokens: 0, calls: 0 };
        }
        byName[span.name]!.promptTokens += prompt;
        byName[span.name]!.evalTokens += eval_;
        byName[span.name]!.calls += 1;
    }

    return { totalPromptTokens, totalEvalTokens, byName };
}

// sut-chat(152, 19), judge(145, 63), semantic-embed(34, 0) ==> 3개의 숫자가 나오는데, 이거 다 집계
// -> 145 * 500 ==> 72,000

/**
 * span 목록을 외부 의존성 없는 단일 HTML 워터폴 차트로 렌더링한다.
 * - 가로축: 첫 span 시작 ~ 마지막 span 종료까지의 시간 비율(%)
 * - 들여쓰기: parentId 체인 깊이
 * - 라벨: span 이름, 토큰 수(p: 입력, e: 생성), 소요 시간
 * 주의: span 이름을 이스케이프하지 않으므로 신뢰할 수 있는 이름만 넣어야 한다.
 */
export function renderWaterfall(spans: Span[]): string {
    if (spans.length === 0) {
        return [
            "<!DOCTYPE html><html><head><meta charset=\"utf-8\">",
            "<title>Trace Waterfall</title>",
            "<style>body{background:#1d2127;color:#c9d1d9;font-family:monospace;padding:24px}</style>",
            "</head><body><p>No spans recorded.</p></body></html>",
        ].join("");
    }

    // spanId → Span 조회용 맵
    const idToSpan = new Map<string, Span>(spans.map((s) => [s.spanId, s]));

    // 조상 체인을 따라 깊이 계산
    function depth(span: Span): number {
        let d = 0;
        let cur: Span | undefined = span;
        while (cur !== undefined && cur.parentId !== null) {
            const parent = idToSpan.get(cur.parentId);
            if (parent === undefined) break;
            d++;
            cur = parent;
        }
        return d;
    }

    // startMs 오름차순 정렬 → 워터폴 시간축 순서
    const sorted = [...spans].sort((a, b) => a.startMs - b.startMs);

    const minStart = Math.min(...sorted.map((s) => s.startMs));
    const maxEnd = Math.max(...sorted.map((s) => s.endMs));
    const totalDuration = Math.max(maxEnd - minStart, 1); // 0 나누기 방지
    const totalMs = maxEnd - minStart;

    // 각 span 행 HTML
    const rows = sorted
        .map((span) => {
            const d = depth(span);
            const indentPx = d * 20;
            const leftPct = (((span.startMs - minStart) / totalDuration) * 100).toFixed(2);
            const rawWidth = ((span.endMs - span.startMs) / totalDuration) * 100;
            const widthPct = Math.max(rawWidth, 0.5).toFixed(2); // 최소 0.5% 가시성 보장

            const rawPrompt = span.attributes["promptEvalCount"];
            const rawEval = span.attributes["evalCount"];
            const hasTokens =
                typeof rawPrompt === "number" || typeof rawEval === "number";
            const tokenLabel = hasTokens
                ? ` <span class="token-label">[p:${typeof rawPrompt === "number" ? rawPrompt : "-"} e:${typeof rawEval === "number" ? rawEval : "-"}]</span>`
                : "";

            const durationMs = span.endMs - span.startMs;

            return (
                `<div class="span-row">` +
                `<div class="span-label" style="padding-left:${indentPx + 8}px">` +
                `${span.name}${tokenLabel}` +
                `<span class="dur-label">${durationMs}ms</span>` +
                `</div>` +
                `<div class="span-bar-track">` +
                `<div class="span-bar" style="left:${leftPct}%;width:${widthPct}%"></div>` +
                `</div>` +
                `</div>`
            );
        })
        .join("\n");

    return `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>Trace Waterfall</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{background:#1d2127;color:#c9d1d9;font-family:monospace;font-size:13px;padding:24px}
h1{color:#e0b04a;font-size:15px;margin-bottom:16px;letter-spacing:.05em}
.meta{color:#8b949e;font-size:12px;margin-bottom:20px}
.span-row{display:flex;align-items:center;height:28px;border-bottom:1px solid #2a2f38}
.span-row:hover{background:#22272e}
.span-label{width:300px;min-width:300px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.token-label{color:#e0b04a;font-size:11px;margin-left:4px}
.dur-label{color:#8b949e;font-size:11px;margin-left:6px}
.span-bar-track{flex:1;background:#2a2f38;position:relative;height:16px;border-radius:2px;overflow:hidden}
.span-bar{position:absolute;top:0;height:100%;background:#e0b04a;opacity:.75;border-radius:2px;min-width:2px}
</style>
</head>
<body>
<h1>Trace Waterfall</h1>
<div class="meta">spans: ${spans.length} | total: ${totalMs}ms</div>
${rows}
</body>
</html>`;
}