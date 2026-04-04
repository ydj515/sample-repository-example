# spring-ai-webmvc-example

Spring AI MCP WebMVC 서버 예제입니다.

## 개요
- 포트: `8088`
- 프로토콜: MCP SSE
- 엔드포인트: `http://localhost:8088/custom-sse`
- 서버 이름: `spring-ai-webmvc-example`

## 실행 방법
```bash
sh gradlew clean bootJar
sh gradlew bootRun
```

## 동작 확인
```bash
curl -i http://127.0.0.1:8088/custom-sse
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
            spring-ai-webmvc-example:
              url: http://localhost:8088
              sse-endpoint: /custom-sse
```

## 참고
- 이 예제도 HTTP 서버 형태로 먼저 띄워둔 뒤 `spring-ai-example` 을 실행하는 구성을 권장합니다.
