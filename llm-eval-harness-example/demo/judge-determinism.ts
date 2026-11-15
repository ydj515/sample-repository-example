/**
 * Judge 재현성 데모 (Ollama 필요).
 * 같은 질문·답변을 temperature 0 + 고정 seed로 5번씩 판정해 verdict와 reason이 매번 같은지 확인한다.
 * 결과에 따라 스냅샷 비교 시 케이스별 verdict를 정확 일치로 고정해도 되는지 권고를 출력한다.
 *
 * 실행: mise run determinism
 */
import { chat } from "../src/ollama.js";

const SYSTEM = `You are an answer quality judge.
Given a question and an answer, evaluate whether the answer is correct.

Respond ONLY with valid JSON in this exact format:
{"verdict":"correct|incorrect|partial","reason":"one sentence explanation"}

Do not include any text outside the JSON object.`;

const CASES = [
    {
        label: "clearly-correct",
        question: "What is the capital of France?",
        answer: "The capital of France is Paris.",
    },
    {
        label: "clearly-incorrect",
        question: "What is the capital of France?",
        answer: "The capital of France is Berlin.",
    },
    {
        label: "ambiguous",
        question: "Is Python a good language for beginners?",
        answer: "Python has simple syntax but can hide performance issues.",
    },
];

const RUNS = 5;

type JudgeOutput = { verdict: string; reason: string };

async function runJudge(question: string, answer: string): Promise<JudgeOutput | null> {
    const messages = [
        { role: "system" as const, content: SYSTEM },
        {
            role: "user" as const,
            content: `Question: ${question}\nAnswer: ${answer}`,
        },
    ];
    const result = await chat(messages, { json: true });
    try {
        const parsed = JSON.parse(result.text) as JudgeOutput;
        if (!parsed.verdict || !parsed.reason) return null;
        return parsed;
    } catch {
        console.error(`  [parse error] raw text: ${result.text.slice(0, 120)}`);
        return null;
    }
}

async function main() {
    const summary: {
        label: string;
        verdicts: (string | null)[];
        reasons: (string | null)[];
        verdictStable: boolean;
        reasonStable: boolean;
        parseFailures: number;
    }[] = [];

    for (const { label, question, answer } of CASES) {
        console.log(`\n=== ${label} ===`);
        console.log(`  Q: ${question}`);
        console.log(`  A: ${answer}`);

        const outputs: (JudgeOutput | null)[] = [];
        for (let i = 0; i < RUNS; i++) {
            const out = await runJudge(question, answer);
            outputs.push(out);
            const v = out?.verdict ?? "PARSE_FAIL";
            console.log(`  run${i + 1}: verdict=${v}  reason=${out?.reason?.slice(0, 80) ?? "N/A"}`);
        }

        const verdicts = outputs.map((o) => o?.verdict ?? null);
        const reasons = outputs.map((o) => o?.reason ?? null);
        const parseFailures = outputs.filter((o) => o === null).length;

        const validVerdicts = verdicts.filter((v) => v !== null);
        const verdictStable = validVerdicts.length === RUNS && new Set(validVerdicts).size === 1;

        const validReasons = reasons.filter((r) => r !== null);
        const reasonStable = validReasons.length === RUNS && new Set(validReasons).size === 1;

        console.log(
            `  --> verdict stable: ${verdictStable} | reason stable: ${reasonStable} | parse failures: ${parseFailures}/${RUNS}`,
        );

        summary.push({ label, verdicts, reasons, verdictStable, reasonStable, parseFailures });
    }

    console.log("\n========== SUMMARY ==========");
    let allVerdictsStable = true;
    for (const s of summary) {
        console.log(
            `${s.label}: verdict_stable=${s.verdictStable}, reason_stable=${s.reasonStable}, parse_fail=${s.parseFailures}/${RUNS}`,
        );
        if (!s.verdictStable) allVerdictsStable = false;
    }

    console.log("\n========== POLICY RECOMMENDATION ==========");
    if (allVerdictsStable) {
        console.log(
            "RESULT: All 3 cases produced identical verdicts across 5 runs at temp=0+seed.",
        );
        console.log(
            "RECOMMENDATION: verdict per-case 정확 락 가능(같은-빌드). A4에서 per-case verdict를 정확 락으로 처리해도 된다.",
        );
    } else {
        const unstable = summary.filter((s) => !s.verdictStable).map((s) => s.label);
        console.log(
            `RESULT: Unstable verdict detected in: ${unstable.join(", ")}.`,
        );
        console.log(
            "RECOMMENDATION: 집계(일치율)만 락, per-case verdict는 대표결과로 처리. A4에서 정확 락 사용 금지.",
        );
    }
}

main().catch(console.error);
