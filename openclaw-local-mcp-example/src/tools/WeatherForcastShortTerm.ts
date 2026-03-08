import { z } from "zod";

/**
 * 초단기실황조회 API 입력 파라미터 스키마
 * - 각 필드에 .describe()를 부여하여 MCP 툴 밸리데이터 요구 충족
 */
const InputSchema = z.object({
  pageNo: z
    .number()
    .int()
    .min(1)
    .max(9999)
    .default(1)
    .optional()
    .describe("페이지 번호(1~9999). 기본값: 1"),
  numOfRows: z
    .number()
    .int()
    .min(1)
    .max(9999)
    .default(1000)
    .optional()
    .describe("한 페이지 결과 수(1~9999). 기본값: 1000"),
  base_date: z
    .string()
    .regex(
      /^\d{8}$/,
      "base_date는 기본적으로 오늘날짜를 입력하고, YYYYMMDD 형식의 날짜이어야 합니다."
    )
    .describe(
      "발표 날짜(YYYYMMDD). base_date는 기본적으로 오늘날짜를 입력하고, YYYYMMDD 형식의 날짜이어야 합니다."
    ),
  base_time: z
    .string()
    .regex(
      /^\d{4}$/,
      "base_time은 고정으로 0500을 넣고, HHMM 형식(정시)이어야 합니다."
    )
    .default("0500")
    .describe(
      "발표 시각(HHMM, 매 정시). base_time은 고정으로 0500을 넣고, HHMM 형식(정시)이어야 합니다."
    ),
  nx: z.number().int().min(0).max(999).describe("예보지점 X 좌표값"),
  ny: z.number().int().min(0).max(999).describe("예보지점 Y 좌표값"),
  targetOffsetDays: z
    .number()
    .int()
    .min(0)
    .max(3)
    .default(3)
    .optional()
    .describe(
      "base_date로부터 며칠 뒤를 반환할지정합니다. 오늘은 targetOffsetDays=0, 내일은 targetOffsetDays=1, 모레는 targetOffsetDays=2"
    )
});

type ForecastInput = z.infer<typeof InputSchema>;

// 응답 타입
interface VillageFcstItem {
  baseDate: string;
  baseTime: string;
  category: string; // TMP, PTY, POP, SKY, UUU, VVV, VEC, WSD 등
  fcstDate: string; // YYYYMMDD
  fcstTime: string; // HHMM
  fcstValue: string;
  nx: number;
  ny: number;
}

interface VillageFcstResponse {
  response: {
    header: {
      resultCode: string;
      resultMsg: string;
    };
    body: {
      dataType: "XML" | "JSON";
      items: { item: VillageFcstItem[] };
      pageNo: number;
      numOfRows: number;
      totalCount: number;
    };
  };
}

/**
 * 초단기실황조회 MCP 도구
 */
function addDaysYMD(ymd: string, days: number): string {
  const y = Number(ymd.slice(0, 4));
  const m = Number(ymd.slice(4, 6));
  const d = Number(ymd.slice(6, 8));
  const dt = new Date(Date.UTC(y, m - 1, d));
  dt.setUTCDate(dt.getUTCDate() + days);
  return dt.toISOString().slice(0, 10).replace(/-/g, "");
}

/** 배열 유틸 */
const uniq = <T, K extends string | number>(arr: T[], key: (x: T) => K) => {
  const set = new Set<K>();
  const out: T[] = [];
  for (const v of arr) {
    const k = key(v);
    if (!set.has(k)) {
      set.add(k);
      out.push(v);
    }
  }
  return out;
};

class WeatherForcastShortTermTool {
  name = "get_short_term_forcast";
  description =
    "날씨를 조회합니다. 기상청 동네예보(단기예보)에서 base_date(YYYYMMDD)를 기준으로 예보만 추려 LLM이 해석하기 좋은 형태로 반환합니다.";

  schema = InputSchema;

  private serviceKey: string;

  constructor(serviceKey: string) {
    this.serviceKey = serviceKey;
  }

  async execute(input: ForecastInput) {
    const params = InputSchema.parse({
      ...input,
      pageNo: input.pageNo ?? 1,
      numOfRows: input.numOfRows ?? 1000,
      targetOffsetDays: input.targetOffsetDays ?? 3
    });

    const BASE_URL =
      "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst";

    const qs = new URLSearchParams({
      serviceKey: this.serviceKey,
      pageNo: String(params.pageNo),
      numOfRows: String(params.numOfRows),
      dataType: "JSON",
      base_date: params.base_date,
      base_time: params.base_time,
      nx: String(params.nx),
      ny: String(params.ny)
    });

    try {
      const res = await fetch(`${BASE_URL}?${qs.toString()}`);
      if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);

      const data = (await res.json()) as VillageFcstResponse;
      const body = data.response.body;
      const items = body.items?.item ?? [];

      // ---- 타겟 날짜 계산 ----
      const targetDate = addDaysYMD(
        params.base_date,
        params.targetOffsetDays ?? 3 // 기본값 보장
      );
      const targetItems = items.filter((i) => i.fcstDate === targetDate);

      // ---- 결과 구조 ----
      return {
        summary: {
          request: {
            base_date: params.base_date,
            base_time: params.base_time,
            nx: params.nx,
            ny: params.ny,
            targetOffsetDays: params.targetOffsetDays
          },
          targetDate,
          availableDates: uniq(items, (i) => i.fcstDate).map((i) => i.fcstDate)
        },
        items: {
          byTime: targetItems.reduce((acc, i) => {
            acc[i.fcstTime] = acc[i.fcstTime] || {};
            acc[i.fcstTime][i.category] = i.fcstValue;
            return acc;
          }, {} as Record<string, Record<string, string>>)
        },
        warnings:
          targetItems.length === 0
            ? [
                `타겟 날짜(${targetDate}) 데이터가 없습니다. 가능 날짜: ${uniq(
                  items,
                  (i) => i.fcstDate
                )
                  .map((i) => i.fcstDate)
                  .join(", ")}`
              ]
            : undefined
      };
    } catch (err: any) {
      return {
        error: "단기예보 조회 실패",
        detail: String(err?.message ?? err)
      };
    }
  }
}

export default WeatherForcastShortTermTool;
