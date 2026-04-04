# spring-ai-example

Spring AI 기능을 한 프로젝트에서 단계적으로 실험할 수 있도록 정리한 예제입니다.

이 프로젝트에는 아래 기능이 포함되어 있습니다.

- Prompt Template
- Structured Output
- Basic Chat
- Advisor
- Chat Playground
- Multimodal Image Generation / Vision
- Audio Speech / Transcription
- Tool Calling
- MCP Client

## 기술 스택

- Java 21
- Spring Boot 3.5.12
- Spring AI 1.1.4
- Gradle Kotlin DSL
- OpenAI
- Anthropic
- MCP Client WebFlux Starter

## 프로젝트 구조

### 핵심 기능
- `PromptTemplateController`
  - 프롬프트 템플릿, 리스트/맵/빈 응답, shops 구조화 응답
- `AiChatController`
  - OpenAI / Anthropic 모델 직접 호출
- `AdvisorController`
  - completion, stream, 구조화 출력
- `MultimodalController`
  - 이미지 URL 생성, base64 이미지 생성, 업로드 이미지 분석
- `AudioController`
  - TTS, TTS + Chat, 스트리밍 음성, STT, STT + Chat
- `ToolCallingController`
  - 날짜/시간, 날씨, 고객 조회, 추천, 출입 제어
- `McpController`
  - MCP 기반 날씨 질의, 이미지 기반 출입 제어

### 관련 디렉터리
- `src/main/java/com/example/springaisample/controller`
  - HTTP 엔드포인트
- `src/main/java/com/example/springaisample/service`
  - 기능 구현
- `src/main/java/com/example/springaisample/service/tool`
  - tool calling 서비스
- `src/main/java/com/example/springaisample/service/mcp`
  - MCP 클라이언트 연동 서비스
- `src/main/java/com/example/springaisample/tool`
  - 로컬 tool 구현체
- `scripts/test-curl.sh`
  - 예제 호출 스크립트
- `scripts/samples`
  - 업로드 테스트용 샘플 파일 위치

## 사전 준비

### 필수 환경변수

실제 모델 호출을 하려면 아래 환경변수를 설정해야 합니다.

```bash
export OPENAI_API_KEY=your-openai-key
export ANTHROPIC_API_KEY=your-anthropic-key
```

기본 `application.yaml` 에는 테스트용 placeholder 값이 들어 있으므로, 키 없이도 애플리케이션이 뜰 수는 있지만 실제 AI 호출은 실패합니다.

### 샘플 업로드 파일

멀티모달, 오디오 업로드 테스트를 하려면 아래 파일을 준비하면 됩니다.

```bash
cp /absolute/path/my-image.png scripts/samples/sample-image.png
cp /absolute/path/my-audio.mp3 scripts/samples/sample-audio.mp3
```

또는 환경변수로 직접 지정할 수 있습니다.

```bash
IMAGE_FILE=/absolute/path/other-image.png \
AUDIO_FILE=/absolute/path/other-audio.mp3 \
./scripts/test-curl.sh
```

## 일반 실행

### 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 포트는 `8080` 입니다.

### 테스트 실행

```bash
./gradlew test
```

테스트는 `src/test/resources/application.yaml` 에서 MCP 클라이언트를 비활성화해 두었기 때문에 외부 MCP 서버 없이도 실행됩니다.

## MCP 연동 실행

현재 `application.yaml` 은 MCP 클라이언트 설정이 실제로 활성화되어 있습니다.

- STDIO 서버: `spring-ai-mcpserver-stdio-example`
- SSE 서버: `spring-ai-webmvc-example`
- SSE 서버: `spring-ai-mcpserver-webflux`

### 권장 실행 순서

1. `spring-ai-mcpserver-stdio-example` 에서 jar 생성
2. `spring-ai-webmvc-example` 실행
3. `spring-ai-mcpserver-webflux` 실행
4. `spring-ai-example` 실행

예시:

```bash
cd ../spring-ai-mcpserver-stdio-example
sh gradlew clean bootJar

cd ../spring-ai-webmvc-example
sh gradlew bootRun

cd ../spring-ai-mcpserver-webflux
sh gradlew bootRun

cd ../spring-ai-example
./gradlew bootRun
```

### 현재 연결 설정

`src/main/resources/application.yaml`

- `spring-ai-webmvc-example`
  - `http://localhost:8088/custom-sse`
- `spring-ai-mcpserver-webflux`
  - `http://localhost:8089/sse`

`src/main/resources/mcp-servers.json`

- `spring-ai-mcpserver-stdio-example`
  - `build/libs/spring-ai-mcpserver-stdio-example-0.0.1-SNAPSHOT.jar`

## curl 예제

전체 예제 호출은 아래 스크립트로 빠르게 확인할 수 있습니다.

```bash
./scripts/test-curl.sh
```

포트가 다르거나 원격 서버에 붙고 싶으면 `BASE_URL` 을 지정하면 됩니다.

```bash
BASE_URL=http://localhost:8080 ./scripts/test-curl.sh
```

## 주요 엔드포인트

### Basic Chat
- `GET /ai/chat`

### Chat Playground
- `GET /test/chat/completion`
- `GET /test/chat/stream`
- `GET /test/chat/chain`
- `GET /test/chat/few`
- `GET /test/chat/memory`

### Prompt Template
- `GET /test/prompt/template`
- `GET /test/prompt/list`
- `GET /test/prompt/map`
- `GET /test/prompt/bean`
- `GET /test/prompt/shops`

### Advisor
- `GET /test/advisor/completion`
- `GET /test/advisor/stream`
- `GET /test/advisor/bean-output`

### Multimodal
- `GET /test/multimodal/generate-image-url`
- `GET /test/multimodal/generate-image`
- `POST /test/multimodal/image-analysis`

### Audio
- `GET /test/audio/text-to-speech`
- `GET /test/audio/text-to-speech-chat`
- `GET /test/audio/text-to-speech-chat-stream`
- `POST /test/audio/speech-to-text`
- `POST /test/audio/speech-to-text-chat`
- `POST /test/audio/speech-to-text-chat-voice`

### Tool Calling
- `GET /test/tools/date-time`
- `GET /test/tools/customer-inquiry-json`
- `GET /test/tools/customer-inquiry-string`
- `GET /test/tools/recommendation`
- `POST /test/tools/access-system`

### MCP
- `GET /test/mcp/weather`
- `POST /test/mcp/access`

## 참고

- 이 프로젝트는 OpenAI와 Anthropic 설정을 모두 가지고 있지만, 기본 chat provider 흐름은 OpenAI 기준으로 맞춰져 있습니다.
- MCP 서버 예제는 상위 디렉터리의 sibling 프로젝트로 분리되어 있습니다.
- `scripts/test-curl.sh` 는 멀티모달, 오디오, tool calling, MCP 예제를 한 번에 점검할 수 있도록 구성되어 있습니다.
