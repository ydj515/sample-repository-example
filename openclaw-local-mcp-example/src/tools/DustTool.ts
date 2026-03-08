import { z } from "zod";

// Based on swagger/dust.json
const SidoNameEnum = z.enum([
  "전국",
  "서울",
  "부산",
  "대구",
  "인천",
  "광주",
  "대전",
  "울산",
  "경기",
  "강원",
  "충북",
  "충남",
  "전북",
  "전남",
  "경북",
  "경남",
  "제주",
  "세종"
]);

/**
 * 시도별 실시간 측정정보 API 입력 파라미터 스키마
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
    .default(100)
    .optional()
    .describe("한 페이지 결과 수(1~9999). 기본값: 100"),
  sidoName: SidoNameEnum.default("서울").describe(
    "시도 이름 (전국, 서울, 부산, ... 등)"
  ),
  searchDate: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, "searchDate는 YYYY-MM-DD 형식이어야 합니다.")
    .default(new Date().toISOString().slice(0, 10))
    .optional()
});

type DustInput = z.infer<typeof InputSchema>;

// 응답 타입 (based on swagger/dust.json)
interface DustResponse {
  response: {
    header: {
      resultCode: string;
      resultMsg: string;
    };
    body: {
      dataType?: "XML" | "JSON";
      items: Array<{
        so2Grade: string | null;
        coFlag: string | null;
        khaiValue: string | null;
        so2Value: string | null;
        coValue: string | null;
        pm10Flag: string | null;
        o3Grade: string | null;
        pm10Value: string | null;
        khaiGrade: string | null;
        sidoName: string | null;
        no2Flag: string | null;
        no2Grade: string | null;
        o3Flag: string | null;
        so2Flag: string | null;
        dataTime: string | null;
        coGrade: string | null;
        no2Value: string | null;
        stationName: string | null;
        pm10Grade: string | null;
        o3Value: string | null;
      }>;
      pageNo: number;
      numOfRows: number;
      totalCount: number;
    };
  };
}

/**
 * 시도별 실시간 대기오염 측정정보 조회 MCP 도구
 */
class DustTool {
  name = "get_sido_realtime_dust";
  description =
    "시도별 실시간 대기오염 측정정보를 조회합니다. 시도명(sidoName)을 입력하면 해당 지역의 측정소별 미세먼지, 오존, 아황산가스 등의 농도와 등급 정보를 반환합니다.";

  schema = InputSchema;

  private serviceKey: string;

  constructor(serviceKey: string) {
    this.serviceKey = serviceKey;
  }

  async execute(input: DustInput) {
    const params = InputSchema.parse(input);

    const BASE_URL =
      "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty";

    const qs = new URLSearchParams({
      serviceKey: this.serviceKey,
      pageNo: String(params.pageNo),
      numOfRows: String(params.numOfRows),
      returnType: "json",
      sidoName: params.sidoName
    });

    const url = `${BASE_URL}?${qs.toString()}`;
    try {
      const res = await fetch(url, { method: "GET" });
      if (!res.ok) {
        throw new Error(`HTTP error! status: ${res.status}`);
      }

      const data = (await res.json()) as DustResponse;

      const resultCode = data?.response?.header?.resultCode;
      const resultMsg = data?.response?.header?.resultMsg;

      if (resultCode !== "00") {
        throw new Error(
          `API 오류(resultCode=${resultCode ?? "UNKNOWN"}): ${
            resultMsg ?? "No message"
          }`
        );
      }

      const body = data.response.body;
      const items = body?.items ?? [];

      return {
        summary: {
          pageNo: body.pageNo,
          numOfRows: body.numOfRows,
          totalCount: body.totalCount,
          sidoName: params.sidoName,
          stations: Array.from(
            new Set(items.map((i) => i.stationName).filter(Boolean))
          ),
          dataTime: items[0]?.dataTime ?? null
        },
        items
      };
    } catch (error: any) {
      console.error("시도별 실시간 대기오염 측정정보 조회 실패:", error);
      return {
        error: "시도별 실시간 대기오염 측정정보를 조회하는 데 실패했습니다.",
        detail: String(error?.message ?? error)
      };
    }
  }
}

export default DustTool;
