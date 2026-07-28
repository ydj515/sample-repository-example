# sample repository example
> 필요해서 만든 example과 [blog](https://ydj515.github.io/)에서 사용한 예제 코드 입니다.
> 사용된 stack 은 아래에 적혀있습니다.

## Stack

### java & kotlin
주로 kotlin1.9 이상, java17 이상, springboot3.3이상, gradle.kts 8이상으로 작성된 예시입니다.

> [!NOTE]
> 2025-12-01 기준 kotlin 2.2 이상(java 25 미지원 → java 21 기준),
> java 25 이상, spring boot 4 이상, gradle 9 이상으로 작성되었습니다.

## TOC
- [bucket4j-example](./bucket4j-example/) — Bucket4j로 IP와 엔드포인트별 요청 제한을 구현한 예제
- [runner-example](./runner-example/) — Spring의 `ApplicationRunner`, `CommandLineRunner`, 애플리케이션 이벤트 실행 순서를 비교하는 예제
- [warmup-example](./warmup-example/) — `@Warmup` 대상을 탐색해 애플리케이션 시작 시 동기·비동기로 준비 작업을 실행하는 예제
- [enum-bean-example](./enum-bean-example/) — enum과 Spring Bean을 연결해 결제 유형별 로직을 호출하는 예제
- [coroutines-basic-example](./coroutines-basic-example/) — Kotlin Coroutine의 `Job`, `Scope`, 취소, suspend, 구조적 동시성을 학습하는 예제
- [local-cache-example](./local-cache-example/) — Caffeine 기반 로컬 캐시와 캐시 통계를 구현한 상품 조회 예제
- [global-cache-example](./global-cache-example/) — Redis와 Spring Cache로 여러 인스턴스가 공유하는 글로벌 캐시를 구현한 예제
- [two-tier-cache-example](./two-tier-cache-example/) — Caffeine 로컬 캐시와 Redis 글로벌 캐시를 결합한 2단계 캐시 예제
- [webflux-with-mongo-example](./webflux-with-mongo-example/) — Spring WebFlux 함수형 라우팅과 Reactive MongoDB로 사용자 API를 구현한 예제
- [webflux-with-redis-example](./webflux-with-redis-example/) — Reactive Redis의 자료형, Lua, Pub/Sub, Stream, 캐시 전략을 다루는 WebFlux 예제
- [resilience4j-example](./resilience4j-example/) — Circuit Breaker, Retry, Rate Limiter, Time Limiter를 조합하고 메트릭을 수집하는 예제
- [uri-strange-example](./uri-strange-example/) — 커스텀 스킴 URI를 `URI`와 `UriComponentsBuilder`로 변환할 때의 차이를 검증하는 예제
- [proxy-query-plan-example](./proxy-query-plan-example/) — datasource-proxy로 느린 쿼리를 감지하고 `EXPLAIN ANALYZE` 실행 계획을 출력하는 예제
- [cte-example](./cte-example/) — 댓글 트리 조회에서 재귀 CTE와 애플리케이션 집계 방식의 성능을 비교하는 예제
- [load-test-example](./load-test-example/) — k6와 Locust로 동일한 부하 테스트 시나리오를 작성하고 실행하는 예제
- [timescaledb-api-stats-example](./timescaledb-api-stats-example/) — API 호출을 Redis Stream으로 수집해 TimescaleDB에 저장하고 시계열 통계를 조회하는 예제
- [jpa-java-example](./jpa-java-example/) — 공연 예매 도메인으로 JPA 연관관계, 조회 최적화, DTO 프로젝션, 쿠폰 다형성을 다루는 Java 예제
- [stream-example](./stream-example/) — Java Stream API 연산을 연습 문제와 정답 코드로 학습하는 예제
- [git-master-to-main](./git-master-to-main/) — GitLab 그룹 프로젝트의 기본 브랜치를 `master`에서 `main`으로 일괄 전환하는 스크립트 예제
- [graphql-example](./graphql-example/) — Spring GraphQL로 사용자, 상품, 장바구니, 검색, Subscription을 구현한 예제
- [oidc-simple-example](./oidc-simple-example/) — Keycloak OIDC 로그인, Redis 세션, 세션 재검증, 관리자 강제 로그아웃 예제
- [oidc-multi-app-example](./oidc-multi-app-example/) — 하나의 Keycloak realm을 공유하는 두 Spring Boot 앱의 멀티 앱 SSO 예제
- [oidc-multi-app-hmac-gateway-example](./oidc-multi-app-hmac-gateway-example/) — 멀티 앱 SSO에 Gateway와 Backend 간 HMAC 서명 검증을 추가한 예제
- [oidc-multi-app-realm-broker-example](./oidc-multi-app-realm-broker-example/) — 서비스별 Keycloak realm과 broker realm을 연결해 공통 SSO를 구성하는 예제
- [kotlin-notebook-example](./kotlin-notebook-example/) — Kotlin Notebook에서 Kotlin 코드와 Spring Boot 애플리케이션을 실험하는 예제
- [mysql8-example](./mysql8-example/) — MySQL 8의 설정, 권한, 성능 진단, 실행 계획, 인덱스, 파티셔닝을 실습하는 예제
- [spring-ai-example](./spring-ai-example/) — Spring AI의 Chat, RAG, 멀티모달, Tool Calling, MCP Client 기능을 통합 실험하는 예제
- [spring-ai-mcpserver-stdio-example](./spring-ai-mcpserver-stdio-example/) — Spring AI로 구현한 STDIO 전송 방식의 MCP 서버 예제
- [spring-ai-mcpserver-webflux](./spring-ai-mcpserver-webflux/) — Spring AI WebFlux와 SSE 전송 방식으로 구현한 MCP 서버 예제
- [spring-ai-webmvc-example](./spring-ai-webmvc-example/) — Spring AI WebMVC와 SSE 전송 방식으로 구현한 MCP 서버 예제
- [openclaw-local-mcp-example](./openclaw-local-mcp-example/) — TypeScript와 MCP SDK로 도구, 리소스, 프롬프트, Sampling을 제공하는 로컬 MCP 서버 예제
