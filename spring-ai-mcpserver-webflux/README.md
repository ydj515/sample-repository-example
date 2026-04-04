# spring-ai-mcpserver-webflux

Spring AI MCP WebFlux 서버 예제입니다.

## 개요
- 포트: `8089`
- 프로토콜: MCP SSE
- 엔드포인트: `http://localhost:8089/sse`
- 서버 이름: `spring-ai-mcpserver-webflux`

## 실행 방법
```bash
sh gradlew clean bootJar
sh gradlew bootRun
```

## 동작 확인
```bash
curl -i http://127.0.0.1:8089/sse
```

정상 실행 시 `200` 과 `text/event-stream` 응답을 확인할 수 있습니다.

## 현재 프로젝트와 연결
`spring-ai-example` 프로젝트의 `application.yaml` 에 아래 MCP SSE 연결이 활성화되어 있습니다.

```yaml
spring:
  ai:
    mcp:
      client:
        sse:
          connections:
            spring-ai-mcpserver-webflux:
              url: http://localhost:8089
              sse-endpoint: /sse
```

## 참고
- 이 예제는 HTTP 서버 형태로 떠 있어야 클라이언트가 연결할 수 있습니다.
- `spring-ai-example` 실행 전에 먼저 띄워두는 편이 안전합니다.
