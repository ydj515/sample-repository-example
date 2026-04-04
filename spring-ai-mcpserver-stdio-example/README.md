# spring-ai-mcpserver-stdio-example

Spring AI MCP STDIO 서버 예제입니다.

## 개요
- 실행 방식: STDIO
- 서버 이름: `spring-ai-mcpserver-stdio-example`
- 현재 프로젝트 연결 파일: `spring-ai-example/src/main/resources/mcp-servers.json`

## 빌드 방법
```bash
sh gradlew clean bootJar
```

빌드가 끝나면 아래 jar 가 생성됩니다.

```bash
build/libs/spring-ai-mcpserver-stdio-example-0.0.1-SNAPSHOT.jar
```

## 실행 방법
보통은 단독 실행보다 MCP 클라이언트가 프로세스를 직접 띄우는 방식으로 사용합니다.

```bash
java -Dspring.ai.mcp.server.stdio=true -jar build/libs/spring-ai-mcpserver-stdio-example-0.0.1-SNAPSHOT.jar
```

또는 `spring-ai-example` 에서 활성화된 `mcp-servers.json` 설정을 통해 자동 실행됩니다.

## 현재 프로젝트와 연결
현재 `spring-ai-example` 의 `mcp-servers.json` 은 아래 jar 경로를 사용합니다.

```json
{
  "mcpServers": {
    "spring-ai-mcpserver-stdio-example": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.server.stdio=true",
        "-jar",
        "~/sample-repository-example/spring-ai-mcpserver-stdio-example/build/libs/spring-ai-mcpserver-stdio-example-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

## 참고
- 이 예제는 jar 가 먼저 빌드되어 있어야 현재 프로젝트에서 바로 사용할 수 있습니다.
- web 서버가 아니라서 브라우저나 `curl` 로 직접 확인하는 용도보다는 MCP 클라이언트 연동용에 가깝습니다.
