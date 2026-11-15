/**
 * 회귀 게이트 데모 (Ollama 불필요).
 * 저장된 스냅샷 두 개를 비교해 요약표, 케이스별 변화, 게이트 PASS/FAIL, 부트스트랩 신뢰구간을 출력한다.
 * 기본 비교: v2 vs v2-broken (의도적으로 악화시킨 스냅샷으로 게이트 FAIL을 재현)
 *
 * 실행: mise run gate
 */
import { fileURLToPath } from "node:url";
import { loadSnapshot } from "../src/dataset.js";
import type { Snapshot } from "../src/dataset.js";
import {
    diffSnapshots,
    gate,
    summaryTable,
    verdictRank,
    bootstrapCI
} from "../src/regression.js";
import type { CaseDiff } from "../src/regression.js";

function snapshotPath(name: string): string {
    return fileURLToPath(
        new URL(`../fixtures/snapshots/${name}.json`, import.meta.url),
    );
}

/** 양쪽에 모두 있는 케이스의 verdict 순위값(correct 2 / partial 1 / incorrect 0) 짝 배열 */
function pairedRanks(diffs: CaseDiff[]): { before: number[]; after: number[] } {
    const before: number[] = [];
    const after: number[] = [];
    for (const d of diffs) {
        if (!d.before || !d.after) continue; // 한쪽에만 있는 id는 짝 비교 불가
        before.push(verdictRank(d.before.verdict));
        after.push(verdictRank(d.after.verdict));
    }
    return { before, after };
}

function compare(
    label: string,
    a: Snapshot,
    b: Snapshot,
    maxRegressions: number,
): void {
    console.log(`\n=== ${label} ===\n`);
    console.log(summaryTable(a));
    console.log();
    console.log(summaryTable(b));

    const diffs = diffSnapshots(a, b);
    const counts = { improved: 0, regressed: 0, same: 0 };
    for (const d of diffs) counts[d.direction] += 1;

    console.log(
        `\ndiff: improved ${counts.improved} / regressed ${counts.regressed} / same ${counts.same}`,
    );
    for (const d of diffs) {
        if (d.direction === "same") continue;
        console.log(
            `  ${d.id}  ${d.before?.verdict ?? "-"} → ${d.after?.verdict ?? "-"}  (${d.direction})`,
        );
    }

    const gateResult = gate(diffs, maxRegressions);
    const idList =
        gateResult.regressedIds.length > 0
            ? `: ${gateResult.regressedIds.join(", ")}`
            : "";
    console.log(
        `gate(maxRegressions=${maxRegressions}): ${gateResult.pass ? "PASS" : "FAIL"} (regressions ${gateResult.regressions}${idList})`,
    );

    // // verdict 순위값으로 paired bootstrap — 기본 seed 7 / iters 2000이라 결정적
    const { before, after } = pairedRanks(diffs);
    const ci = bootstrapCI(before, after);
    console.log(
        `bootstrapCI(verdict 순위): delta ${ci.delta.toFixed(4)}, 95% CI [${ci.low.toFixed(4)}, ${ci.high.toFixed(4)}]`,
    );
}

// const v1 = loadSnapshot(snapshotPath("v1"));
const v2 = loadSnapshot(snapshotPath("v2"));
const v2Broken = loadSnapshot(snapshotPath("v2-broken"));

// compare("v1 vs v2 (프롬프트 개선 A/B)", v1, v2, 2);
compare("v2 vs v2-broken (게이트 fail 재현)", v2, v2Broken, 2);


// CI 게이트
// -> 코드 변경이 생길 떄마다, 자동으로 검사를 실행하라

/*
    1. 개발자가 프롬프트를 수정 -> SUT를 평가셋에 돌려서 새로운 스냅샷을 생성
    2. 이 스냅샷 JSON으로 저장(커밋)
    3. PR Open
    4. CI가 훅 받아서 트리거 -> 스크립트를 실행
    5. 게이트가 PASS라면, 머지 허용 or FAIL
*/