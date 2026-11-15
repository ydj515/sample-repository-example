/**
 * Judge 캘리브레이션 데모 (Ollama 필요).
 * 1. 골드셋 일치율: fixtures/gold-set.json의 사람 라벨과 Judge 판정을 비교한다.
 * 2. 위치 편향률: fixtures/pairs.json의 고정 쌍을 A/B, B/A 두 순서로 판정한다.
 *
 * 실행: mise run calibration
 */
import { readFileSync } from "node:fs";
import {
    agreementRate,
    positionBiasRate,
    CALIBRATION_RUBRIC,
} from "../src/calibration.js";
import type { GoldCase, Pair } from "../src/calibration.js";

/** 이 파일 기준 상대 경로의 JSON 픽스처를 읽는다. */
function loadJson<T>(relPath: string): T {
    const url = new URL(relPath, import.meta.url);
    return JSON.parse(readFileSync(url, "utf-8")) as T;
}

/** 분모가 0이면 비율 대신 "분모 0"을 표시한다. */
function percent(n: number, total: number): string {
    return total === 0 ? "분모 0" : `${((n / total) * 100).toFixed(1)}%`;
}

async function main() {
    const goldSet = loadJson<GoldCase[]>("../fixtures/gold-set.json");
    const pairs = loadJson<Pair[]>("../fixtures/pairs.json");

    // 1. 골드셋 일치율: 라벨 분포를 먼저 출력해 일치율을 해석할 기준을 보여 준다.
    const byLabel = { correct: 0, incorrect: 0, partial: 0 };
    for (const goldCase of goldSet) byLabel[goldCase.label]++;

    console.log("=== judge 캘리브레이션: 골드셋 일치율 ===");
    console.log(
        `골드셋 ${goldSet.length}케이스 (correct ${byLabel.correct} / incorrect ${byLabel.incorrect} / partial ${byLabel.partial})`,
    );
    const agreement = await agreementRate(goldSet, CALIBRATION_RUBRIC);
    console.log(
        `일치율: ${agreement.agree}/${agreement.total} (${percent(agreement.agree, agreement.total)})`,
    );

    // 2. 위치 편향률: tie가 나온 쌍은 편향 여부를 판단할 수 없어 분모에서 제외된다.
    console.log("\n=== judge 캘리브레이션: 위치편향률 ===");
    console.log(`고정 쌍: ${pairs.length}쌍 (쌍마다 A/B, B/A 두 배치 판정)`);
    const bias = await positionBiasRate(pairs);
    const ties = pairs.length - bias.total;
    console.log(
        `위치편향률: ${bias.biased}/${bias.total} (${percent(bias.biased, bias.total)}, tie ${ties}쌍 제외)`,
    );
}

main().catch((error: unknown) => {
    console.error(error instanceof Error ? error.message : String(error));
    process.exit(1);
});
