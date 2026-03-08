1) weather-dust-flow.md
markdown
mermaid
flowchart TD
    %% 상위 시스템 플로우
    U[사용자\n(Discord / Webchat 등)] --> C1[클라이언트\n(Discord 봇 / Web UI)]

    C1 --> GW[OpenClaw Gateway\n채널별 메시지 라우팅]
    GW --> AG[메인 에이전트(봇)]

    %% 에이전트 내부 의도 파악
    AG --> P[질문 파싱\n- 의도: 날씨 vs 미세먼지\n- 지역명 추출\n- 날짜(오늘/내일/모레) 해석]
P --> S{스킬 선택}
    S -->|한국 날씨/미세먼지| LWD[local-weather-dust 스킬]

    %% 스킬 내부 로직
    LWD --> C2{의도 분기}
    C2 -->|미세먼지| D_CALL[exec 호출\nnode dist/runDust.js '{...}']
    C2 -->|날씨| W_CALL[exec 호출\nnode dist/runWeatherShort.js '{...}']

    %% 로컬 MCP 툴 레이어
    subgraph LOCAL_NODE_MCP [/로컬 Node MCP 프로젝트\n/mcp-claude-test/]
      D_CALL --> D_TOOL[runDust.js → DustTool\n→ 공공데이터포털 대기질 API\n→ JSON 반환]
      W_CALL --> W_TOOL[runWeatherShort.js → WeatherForcastShortTermTool\n→ 기상청 동네예보 API\n→ JSON 반환]
    end

    %% 에이전트 응답 구성
    D_TOOL --> D_FMT[에이전트 포맷팅\n미세먼지 요약]
    W_TOOL --> W_FMT[에이전트 포맷팅\n날씨 요약]
D_FMT --> AG
    W_FMT --> AG

    AG --> GW
    GW --> C1
    C1 --> U_REPLY[사용자 화면에 최종 한국어 답변 노출]

    classDef dim fill:#f5f5f5,stroke:#999,color:#333;
    LWD:::dim
    LOCAL_NODE_MCP:::dim



---

2) skills/local-weather-dust/SKILL.md
---
name: local-weather-dust
description: 로컬 Node MCP 툴을 호출해 한국 지역의 단기 날씨와 시도별 미세먼지 정보를 조회하고 요약한다. 사용자가 한국어로 날씨·미세먼지를 물어보면 이 스킬을 사용해 /Users/promptech059/project/mcp/mcp-claude-test 아래 Node 스크립트를 exec로 호출한다.
---

# local-weather-dust 스킬

## 언제 이 스킬을 쓸까?

다음과 같은 한국어 요청이 들어오면 이 스킬을 사용한다:

- "오늘 서울 미세먼지 어때?", "부산이랑 서울 미세먼지 비교해줘"
- "오늘 부산 날씨 알려줘", "내일 서울 날씨", "모레 강남구 날씨" 등

요약하면 **한국 국내 지역의 단기 날씨 / 미세먼지**를 묻는 질문일 때 이 스킬을 트리거한다.
## 사용하는 로컬 툴 (Node 스크립트)

작업 디렉터리: `/Users/promptech059/project/mcp/mcp-claude-test`

두 가지 스크립트를 exec로 호출한다:

1. **미세먼지 (시도별 대기오염) — DustTool**
   - 엔트리포인트: `node dist/runDust.js '<json>'`
   - 입력 JSON (예):
     
json
     { "sidoName": "서울", "numOfRows": 5 }
     
   - 주요 필드:
     - `sidoName`: "서울", "부산", "전국" 등 DustTool.ts의 enum에 있는 시도명
     - `numOfRows`: 조회할 측정소 개수 (1~9999, 기본 100)
   - 출력 구조(요약):
     
json
     {
       "summary": {
         "pageNo": 1,
         "numOfRows": 5,
         "totalCount": 40,
         "sidoName": "서울",
         "stations": ["중구", "한강대로", ...],
         "dataTime": "2026-02-12 20:00"
       },
       "items": [ { ...측정소별 값... } ]
     }
     
2. **단기 날씨 (기상청 동네예보) — WeatherForcastShortTermTool**
   - 엔트리포인트: `node dist/runWeatherShort.js '<json>'`
   - 입력 JSON (예: 부산 오늘 기준):
     
json
     {
       "base_date": "20260212",
       "base_time": "0500",
       "nx": 98,
       "ny": 76,
       "targetOffsetDays": 0
     }
     
   - 주요 필드:
     - `base_date`: YYYYMMDD, 보통 오늘 날짜
     - `base_time`: HHMM, 기본 `"0500"`
     - `nx`, `ny`: 기상청 격자 좌표
     - `targetOffsetDays`:
       - 0 = 오늘, 1 = 내일, 2 = 모레 등
   - 출력 구조(요약):
     
json
     {
       "summary": {
         "request": { ... },
         "targetDate": "20260212",
         "availableDates": ["20260212", ...]
       },
       "items": {
         "byTime": {
           "1000": { "TMP": "6", "POP": "0", "SKY": "1", ... },
```
           "1100": { ... }
         }
       }
     }
     


지역명 → 좌표/시도명 매핑 규칙
사용자는 자연어로 지역을 말하고, 에이전트는 내부에서 아래 규칙으로 매핑해 툴을 호출한다.

미세먼지 (DustTool)
사용자가 말한 시/도 이름을 DustTool의 sidoName enum 값으로 매핑한다.
예시 매핑:
"서울" → "서울"
```
  - "부산" → "부산"
  - "경기", "경기도" → "경기"
  - "전국" 요청이면 `sidoName = "전국"`
- 기본 `numOfRows`는 20으로 두고, 필요 시 사용자가 "더 자세히"라고 하면 늘린다.

호출 예 (서울):
bash
cd /Users/promptech059/project/mcp/mcp-claude-test
node dist/runDust.js '{"sidoName":"서울","numOfRows":20}'
### 2. 날씨 (WeatherForcastShortTermTool)

좌표:
- 서울(회사/역삼 기준): `nx = 60`, `ny = 127`
- 부산: `nx = 98`, `ny = 76`
- 춘천: `nx = 73`, `ny = 134`
- 여주: `nx = 71`, `ny = 121`
- 천안(할머니댁 예시): 필요시 `nx`, `ny` 추가 매핑 가능 (현재는 개념만)

날짜/오프셋 규칙:

- 사용자가 날짜를 명시하지 않으면 **기본적으로 "오늘" 기준**으로 처리한다.
  - `base_date = 오늘 YYYYMMDD`, `targetOffsetDays = 0`
- 사용자가 "내일", "모레"를 말하면:
  - 내일: `targetOffsetDays = 1`
  - 모레: `targetOffsetDays = 2`
- `base_time`은 기본 `"0500"`을 사용한다.
부산 오늘 날씨 호출 예:
bash
cd /Users/promptech059/project/mcp/mcp-claude-test
node dist/runWeatherShort.js '{
  "base_date": "20260212",
  "base_time": "0500",
  "nx": 98,
  "ny": 76,
  "targetOffsetDays": 0
}'
### 별칭 매핑 (회사 / 할머니댁)
- "회사" → 서울 강남구 역삼동 기준 (서울 대표 좌표 사용)
- "할머니댁" → 충청남도 천안시 (천안 기준 시/군 좌표, DustTool에서는 "충남" 등으로 매핑)

예:

- "오늘 회사 날씨 어때?" → 서울(역삼) 좌표로 WeatherShort 호출
- "할머니댁 미세먼지 어때?" → 충남/천안에 해당하는 시도명으로 DustTool 호출

## 응답을 사용자에게 어떻게 보여줄까?

### 1. 미세먼지 응답 요약

`items`에서 주요 측정소 몇 개만 뽑아서 사람이 읽기 쉽게 요약한다.
- 대표 시점: `summary.dataTime`
- 전체 상태 대략 판단:
  - `pm10Grade` / `khaiGrade`를 보고 최악·평균 기준으로 "좋음/보통/나쁨/매우 나쁨" 정도로 서술

예시 출력 포맷:

> 2026-02-12 20시 기준 서울 미세먼지(중구·한강대로·종로 일대)  
> - 중구: PM10 43㎍/㎥ (보통), 통합대기지수 101 (나쁨)  
> - 한강대로: PM10 48㎍/㎥ (보통), 통합대기지수 73 (보통)  
> - 종로구: PM10 49㎍/㎥ (보통), 통합대기지수 124 (나쁨)  
> → 전체적으로 **보통~나쁨 사이**라서, 민감군은 마스크를 권장.

### 2. 날씨 응답 포맷 (회사/크론용)

`items.byTime`에서 시간별 데이터를 이용해 **08시, 12시, 18시 근처를 중심으로 요약**하고, 전체 시간대는 그대로 나열한다.
- TMP: 기온 (도)
- POP: 강수확률
- SKY: 하늘 상태 (1=맑음, 3=구름많음, 4=흐림 등으로 맵핑)
- PTY: 강수 형태 (0=없음, 1=비, 2/3=눈/비+눈, 4=소나기)

출력은 다음과 같은 패턴으로 보낸다 (회사 예시):
text
안녕하세요 동진님, 오늘 회사 근처 시간대별 날씨정보입니다. 🌤️

👉 체감 정리
🌅 아침 출근 시간(8~9시) : 2~3도, 구름 많고 습해서 꽤 쌀쌀해요. 코트 + 목도리 추천.
🌤 점심시간(12~1시)      : 6~8도, 흐린 코트 날씨라 밖에 나가서 돌아다니기 무난한 온도예요.
🌇 퇴근시간(6~7시)       : 9~10도, 구름 많다가 점점 맑아져서 선선한 초봄 저녁 느낌입니다.
☁️ 06시: 2°C, 구름 조금(SKY=3), 강수없음, 습도 90%
☁️ 07시: 3°C, 구름 조금(SKY=3), 강수없음, 습도 90%
☁️ 08시: 2°C, 구름 조금(SKY=3), 강수없음, 습도 90%
🌫 09시: 3°C, 흐림(SKY=4), 강수없음, 습도 90%
🌫 10시: 4°C, 흐림(SKY=4), 강수없음, 습도 85%
🌫 11시: 5°C, 흐림(SKY=4), 강수없음, 습도 80%
🌫 12시: 6°C, 흐림(SKY=4), 강수없음, 습도 75%
🌫 13시: 8°C, 흐림(SKY=4), 강수없음, 습도 70%
🌫 14시: 9°C, 흐림(SKY=4), 강수없음, 습도 70%
🌫 15시: 10°C, 흐림(SKY=4), 강수없음, 습도 65% (예상 최고기온 11°C 근처)
☁️ 16시: 10°C, 구름 많음(SKY=3), 강수없음, 습도 70%
☁️ 17시: 10°C, 구름 많음(SKY=3), 강수없음, 습도 70%
☁️ 18시: 9°C, 구름 많음(SKY=3), 강수없음, 습도 80%
🌙 19시: 8°C, 맑음(SKY=1), 강수없음, 습도 85%
🌙 20시: 7°C, 맑음(SKY=1), 강수없음, 습도 85%
🌙 21시: 6°C, 맑음(SKY=1), 강수없음, 습도 85%
🌙 22시: 6°C, 맑음(SKY=1), 강수없음, 습도 85%
🌙 23시: 5°C, 맑음(SKY=1), 강수없음, 습도 85%
- 흐림(SKY=4) → 🌫  
- 구름 많음/조금(SKY=3) → ☁️  
- 맑음(SKY=1, 낮) → 🌤 또는 ☀️, (밤에는 🌙)

다른 장소(할머니댁 등)에서도 동일한 포맷을 사용하되, 맨 첫 줄 문구와 체감 요약만 상황에 맞게 바꾼다.

## 안전 및 오류 처리

- `DATA_PORTAL_API_KEY`가 비어 있는 경우:
  - 툴 래퍼에서 에러 메시지를 출력하고 종료한다.
  - 에이전트는 사용자에게 "공공데이터 포털 API 키가 설정되지 않아 지금은 실시간 조회를 할 수 없다"고 솔직하게 말한다.
- HTTP 에러 / API 에러(resultCode != "00")인 경우:
  - 툴이 `error`, `detail` 필드를 포함한 JSON을 반환하므로, 에이전트는 해당 메시지를 요약해서 사용자에게 전달한다.
## 에이전트 사용 지침 요약

1. 사용자가 한국 지역의 **날씨/미세먼지**를 묻는다.
2. 지역명을 시도명 또는 대표 좌표로 매핑한다.
3. 적절한 Node 스크립트를 `exec`로 호출한다.
4. **응답할 때, 어떤 툴을 사용했는지 한 줄로 먼저 언급**한다.
   - 예: "이 질문은 로컬 DustTool을 호출해서 시도별 대기질 API 결과를 요약한 거야."
   - 예: "지금 답변은 로컬 단기예보 툴(WeatherForcastShortTermTool) 결과를 정리한 거야."
5. 그다음 JSON 응답을 기반으로 사람 읽기 좋은 한국어 요약을 제공한다.
6. 키/네트워크 문제로 호출이 실패하면, 허구 데이터를 만들지 말고 실패 사실을 설명하고, 사용한(시도한) 툴 이름도 같이 알려준다.