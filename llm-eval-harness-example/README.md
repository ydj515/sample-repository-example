# llm-eval-harness-example

로컬 Ollama(`llama3.2`, `bge-m3`) 기반의 사내 HR 정책 RAG 질의응답 시스템(SUT)과 이를 검증하는 LLM 평가 하네스 예제입니다.

## 주요 구성

- `src/sut.ts`: 키워드 중첩 기반 문서 검색(`retrieve`) 및 프롬프트 버전(`v1`, `v2`)별 답변 생성
- `src/scorers/deterministic.ts`: 정확 일치, 정규식, JSON 스키마(Zod), 필수 키워드, 출처 인용, 답변 거부 감지 등 결정론적 채점기
- `src/scorers/semantic.ts`: `bge-m3` 임베딩 코사인 유사도 기반 의미 유사도 채점기
- `src/judge.ts`: 루브릭 기반 단일 채점(`pointwiseJudge`) 및 쌍 비교(`pairwiseJudge`) LLM-as-a-Judge
- `src/calibration.ts`: 골드셋 일치율(`agreementRate`) 및 A/B 순서 교차 검증을 통한 위치 편향률(`positionBiasRate`) 측정
- `src/dataset.ts`: 평가셋(`fixtures/eval-set.json`) 실행 및 스냅샷 저장/로드
- `src/regression.ts`: 스냅샷 간 비교(`diffSnapshots`), 회귀 게이트(`gate`), 부트스트랩 95% 신뢰구간(`bootstrapCI`) 산출
- `src/tracer.ts`: 실행 단계별 Span 수집, 토큰 회계(`tokenAccount`), 워터폴 HTML 렌더링(`renderWaterfall`)

## 사전 준비

Node.js는 `mise.toml`에 `24.22.3`으로 지정되어 있습니다. 스냅샷 생성·재현성 검증·트레이싱 데모는 로컬 Ollama 서버(`http://localhost:11434`) 및 모델 설치가 필요합니다(저장된 스냅샷을 비교하는 `mise run gate`와 `mise run ci`는 Ollama 없이 실행 가능).

```bash
ollama pull llama3.2
ollama pull bge-m3
```

## 실행 방법 (mise)

```bash
cd llm-eval-harness-example
mise trust
mise install
mise run install

# 타입 체크 및 오프라인 회귀 게이트 통합 검증 (Ollama 불필요)
mise run ci

# 저장된 스냅샷(v2 vs v2-broken) 회귀 비교 및 CI 게이트 검증 (Ollama 불필요)
mise run gate

# 프롬프트 버전별 스냅샷 생성 (Ollama 필요)
mise run snapshot:v1
mise run snapshot:v2

# Judge 판정 재현성 확인 (Ollama 필요)
mise run determinism

# Judge 골드셋 일치율 및 위치 편향률 측정 (Ollama 필요)
mise run calibration

# Span 트레이싱, 토큰 회계 및 artifacts/trace.html 워터폴 생성 (Ollama 필요)
mise run trace
```
