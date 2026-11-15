/**
 * 회귀 분석.
 * 두 스냅샷(이전 버전, 새 버전)을 케이스 단위로 비교하고,
 * 악화(regressed) 건수로 배포 게이트를 판정하며, 부트스트랩으로 개선 폭의 신뢰구간을 구한다.
 * 모든 함수가 파일 데이터만 다루므로 Ollama 없이 실행된다.
 */
import type { CaseResult, Snapshot } from "./dataset.js"


// 비교하는데에 필요한 값만
export type CaseDiffSide = {
    verdict: CaseResult["verdict"];
    scores: CaseResult["scores"];
};

// 케이스 하나의 비교 결과 전체
export type CaseDiff = {
    id: string;
    before: CaseDiffSide | null;
    after: CaseDiffSide | null;
    changed: boolean;
    direction: "improved" | "regressed" | "same";
};

// verdict를 비교 가능한 순위값으로 바꾼다. 클수록 좋다.
const VERDICT_RANK: Record<CaseResult["verdict"], number> = {
    correct: 2,
    partial: 1,
    incorrect: 0,
};

export function verdictRank(verdict: CaseResult["verdict"]): number {
    return VERDICT_RANK[verdict];
}

// 케이스에서 비교에 필요한 verdict, scores만 뽑는 함수
function toSide(c: CaseResult): CaseDiffSide {
    return { verdict: c.verdict, scores: c.scores };
}

// 파라메터의 두 scores가 같은지를 필드별로 확인
function scoresEqual(a: CaseResult["scores"], b: CaseResult["scores"]): boolean {
    return (
        a.keyword === b.keyword &&
        a.citation === b.citation &&
        a.refusal === b.refusal &&
        a.semantic === b.semantic
    );
}

/**
 * 두 스냅샷을 케이스 id 기준으로 비교한다.
 * - direction: verdict 순위가 오르면 improved, 내리면 regressed, 같으면 same
 * - changed: verdict 또는 채점 점수 중 하나라도 바뀌었는지
 * - 한쪽에만 있는 케이스는 changed: true, direction: "same"으로 기록한다(게이트 집계에서 제외).
 * 결과 순서: a의 케이스 순서 -> b에만 있는 케이스 순서
 */
export function diffSnapshots(a: Snapshot, b: Snapshot): CaseDiff[] {
    const afterById = new Map(b.cases.map((c) => [c.id, c]));
    const beforeIds = new Set(a.cases.map((c) => c.id));
    const diffs: CaseDiff[] = [];

    for (const beforeCase of a.cases) {
        const afterCase = afterById.get(beforeCase.id);
        if (!afterCase) {
            diffs.push({
                id: beforeCase.id,
                before: toSide(beforeCase),
                after: null,
                changed: true,
                direction: "same",
            });
            continue;
        }

        const rankBefore = VERDICT_RANK[beforeCase.verdict];
        const rankAfter = VERDICT_RANK[afterCase.verdict];
        const direction =
            rankAfter > rankBefore
                ? "improved"
                : rankAfter < rankBefore
                    ? "regressed"
                    : "same";

        diffs.push({
            id: beforeCase.id,
            before: toSide(beforeCase),
            after: toSide(afterCase),
            changed:
                beforeCase.verdict !== afterCase.verdict ||
                !scoresEqual(beforeCase.scores, afterCase.scores),
            direction,
        });

        // before, after : partial -> changed : true, direction same
    }

    for (const afterCase of b.cases) {
        if (beforeIds.has(afterCase.id)) continue;
        diffs.push({
            id: afterCase.id,
            before: null,
            after: toSide(afterCase),
            changed: true,
            direction: "same",
        });
    }

    return diffs;
}


// 게이트 : regressed 케이스가 몇 건이냐??, 허용 임계값과 비교

export type GateResult = {
    pass: boolean;
    regressions: number;
    regressedIds: string[];
};

/** regressed 케이스 수가 maxRegressions 이하이면 통과한다. CI에서 머지 허용 여부 판단에 쓴다. */
export function gate(diffs: CaseDiff[], maxRegressions: number): GateResult {
    const regressedIds = diffs
        .filter((d) => d.direction === "regressed")
        .map((d) => d.id);
    return {
        pass: regressedIds.length <= maxRegressions,
        regressions: regressedIds.length,
        regressedIds,
    };
}

// summaryTable에서 통과율을 보여 줄 boolean 채점기 (semantic은 숫자라 제외)
const SCORER_NAMES = ["keyword", "citation", "refusal"] as const;

/**
 * 스냅샷 하나의 verdict 분포와 채점기별 통과율을 고정폭 텍스트 표로 만든다.
 * 통과율의 분모는 해당 채점 기준이 있는 케이스 수다.
 */
export function summaryTable(s: Snapshot): string {
    const counts: Record<CaseResult["verdict"], number> = {
        correct: 0,
        partial: 0,
        incorrect: 0,
    };
    for (const c of s.cases) counts[c.verdict] += 1;

    const lines: string[] = [];
    lines.push(`snapshot ${s.version} | model ${s.model} | cases ${s.cases.length}`);
    lines.push("-".repeat(46));
    for (const verdict of ["correct", "partial", "incorrect"] as const) {
        lines.push(
            `verdict  ${verdict.padEnd(10)} ${String(counts[verdict]).padStart(5)}`,
        );
    }
    for (const name of SCORER_NAMES) {
        const scored = s.cases.filter((c) => c.scores[name] !== undefined);
        const passed = scored.filter((c) => c.scores[name] === true).length;
        const rate =
            scored.length > 0
                ? `${((passed / scored.length) * 100).toFixed(1)}%`
                : "-";
        lines.push(
            `scorer   ${name.padEnd(10)} ${`${passed}/${scored.length}`.padStart(5)} (${rate.padStart(6)})`,
        );
    }
    return lines.join("\n");
}

// 난수 생성기(PRNG)
export function mulberry32(seed: number): () => number {
    // mulberry32 : 숫자를 계속 뒤죽박죽 섞어서 다음 숫자를 만든다.
    // 1234 -> 891273(0.891273) -> 452189 -> ...
    let state = seed >>> 0;
    return () => {
        state = (state + 0x6d2b79f5) >>> 0;
        let t = state;
        t = Math.imul(t ^ (t >>> 15), t | 1);
        t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
        return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
}

/**
 * 짝지은(paired) 부트스트랩으로 평균 개선 폭(B - A)의 95% 신뢰구간을 구한다.
 * 1. 케이스별 차이(scoresB[i] - scoresA[i])를 만든다.
 * 2. 차이 배열에서 복원 추출로 n개를 뽑아 평균을 내는 과정을 iters번 반복한다.
 * 3. 반복 평균을 정렬해 2.5%, 97.5% 위치 값을 구간으로 쓴다.
 * 구간이 0을 포함하면 개선(또는 악화)이 우연일 가능성을 배제할 수 없다.
 * 시드를 고정한 PRNG를 쓰므로 같은 입력이면 항상 같은 구간이 나온다.
 * @throws 두 배열 길이가 다르거나 비어 있을 때
 */
export function bootstrapCI(
    scoresA: number[],
    scoresB: number[],
    opts: { iters?: number; seed?: number } = {},
): { delta: number; low: number; high: number } {
    const { iters = 2000, seed = 7 } = opts;
    if (scoresA.length !== scoresB.length) {
        throw new Error(
            `bootstrapCI: 길이 불일치 (${scoresA.length} vs ${scoresB.length}) — 케이스별 짝 점수가 필요하다`,
        );
    }
    if (scoresA.length === 0) {
        throw new Error("bootstrapCI: 빈 배열은 비교할 수 없다");
    }

    const n = scoresA.length;
    const perCaseDelta = scoresA.map((a, i) => scoresB[i]! - a);
    const delta = perCaseDelta.reduce((sum, d) => sum + d, 0) / n;

    const rand = mulberry32(seed);
    const resampled: number[] = new Array(iters);
    for (let iter = 0; iter < iters; iter++) {
        let sum = 0;
        for (let j = 0; j < n; j++) {
            sum += perCaseDelta[Math.floor(rand() * n)]!;
        }
        resampled[iter] = sum / n;
    }
    resampled.sort((x, y) => x - y);

    const low = resampled[Math.floor(iters * 0.025)]!; // 하위권
    const high = resampled[Math.ceil(iters * 0.975) - 1]!; // 순위권
    return { delta, low, high };
}