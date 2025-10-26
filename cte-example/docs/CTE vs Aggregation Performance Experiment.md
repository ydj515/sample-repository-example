# CTE vs. Aggregation Performance Experiment

이 문서는 동일한 기능(포스트별 상위 댓글 트리 조회)을 **CTE 기반 쿼리**와 **애플리케이션 레벨 집계** 두 가지 방식으로 구현했을 때의 성능을 비교하기 위한 실험 절차를 정리한다. Spring Boot + Kotlin 애플리케이션과 MySQL, k6, Prometheus/Grafana 조합을 사용하며, 실험 준비부터 모니터링, 데이터 수집/분석까지의 전체 과정을 다룬다.

## 1. 기술 스택과 구성요소
- **애플리케이션**: Kotlin 1.9.25, Spring Boot 3.5.6, Java 17 (`build.gradle.kts`)
- **데이터베이스**: MySQL 8.x (로컬 혹은 외부 인스턴스)
- **쿼리 비교 대상**
    - CTE: `PostCommentRepositoryImpl.findTopCommentTreesUsingCte`에서 재귀 CTE + 윈도우 함수 사용 (`src/main/kotlin/com/example/cteexample/post/repository/PostCommentRepositoryImpl.kt`)
    - Aggregation: `PostCommentService.findTopCommentTreesWithAggregation`에서 전체 레코드 조회 후 애플리케이션에서 트리 구축 (`src/main/kotlin/com/example/cteexample/post/service/PostCommentService.kt`)
- **부하 테스트**: `k6/top-comment-trees-cte.js`, `k6/top-comment-trees-aggregation.js`
- **모니터링**: Actuator + Micrometer + Prometheus + Grafana (`docker-compose.yml`, `monitoring/*`)

## 2. 실험 환경 요구사항
| 항목 | 요구 버전/설명 |
| --- | --- |
| JDK | 17 (Gradle wrapper에서 자동 사용 가능) |
| Kotlin/Spring | 이미 프로젝트에 정의 |
| MySQL | 8.x, `application.yml`의 `spring.datasource` 정보에 맞춰 계정/DB 준비 |
| Docker / Docker Compose | Prometheus, Grafana 실행용 |
| k6 | v0.49 이상 권장 (Prometheus remote write 사용 시 `K6_OUT=experimental-prometheus-rw`) |

> 실험 시 하드웨어 스펙(CPU, 메모리, 디스크)과 OS 정보를 별도로 기록해 두면 결과 해석에 도움이 된다.

## 3. 프로젝트 구조 한눈에 보기
```
cte-example
├─ src/main/kotlin/.../post
│  ├─ domain/PostComment.kt
│  ├─ repository/PostCommentRepository*.kt
│  ├─ service/PostCommentService.kt
│  └─ web/PostCommentController.kt
├─ database/post_comment_dummy_data.sql
├─ database/post_comment10000in_one_post_dummy_data.sql
├─ k6/top-comment-trees-cte.js
├─ k6/top-comment-trees-aggregation.js
└─ monitoring/
   ├─ prometheus/prometheus.yml
   └─ grafana/...
```

## 4. 테스트 데이터 준비
1. **MySQL 부팅**
   ```bash
   docker run -d \
     --name cte-mysql \
     -e MYSQL_ROOT_PASSWORD=rootpass \
     -e MYSQL_DATABASE=mydatabase \
     -e MYSQL_USER=myuser \
     -e MYSQL_PASSWORD=mypassword \
     -p 3306:3306 \
     mysql:8.4
   ```
    - `application.yml`의 `spring.datasource` 정보와 일치하도록 사용자/비밀번호/DB명을 맞춘다.
2. **기본 스키마**
    - `PostComment` 엔티티만 사용하므로 `post_comment` 테이블을 생성한다. JPA `ddl-auto=update`가 활성화되어 있어 애플리케이션 최초 실행 시 자동 생성되지만, 명시적으로 스키마를 관리해도 된다.
3. **데이터 적재**
    - `database/post_comment_dummy_data.sql`: 여러 포스트에 루트/자식 댓글 블록을 만들어 최대 깊이를 제어할 수 있는 스크립트.
    - `database/post_comment10000in_one_post_dummy_data.sql`: 단일 포스트에 1만 건 이상의 댓글을 집중시켜 극단적인 케이스 실험용.
   ```bash
   mysql -h 127.0.0.1 -u myuser -p mydatabase < database/post_comment_dummy_data.sql
   ```
4. **데이터 검증**
    - 스크립트 마지막에 포함된 `WITH RECURSIVE tree ...` 쿼리 결과로 루트 수, 최대 깊이, 건수를 확인하고 실험에 사용할 `post_id`를 결정한다.

## 5. 애플리케이션 실행 및 핵심 로직
1. **애플리케이션 실행**
   ```bash
   SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
   ```
    - Actuator endpoint(`/actuator/prometheus`)가 노출되고 Micrometer에 `application=cte-example` 태그가 붙는다.
2. **엔드포인트**
    - `GET /api/posts/{postId}/comments/top-trees/cte?limit=3`
    - `GET /api/posts/{postId}/comments/top-trees/aggregation?limit=3`
    - 응답(`CommentTreeQueryResponse`)에는 `durationMillis`(서비스 메서드 실행 시간), `treeCount`, `limit`, `trees`(트리 구조)가 포함된다.
3. **CTE 접근**
    - `PostCommentRepositoryImpl`에서 재귀 CTE(`post_comment_score`) → 윈도우 함수(`SUM(score) OVER ...`, `DENSE_RANK()`)를 사용해 상위 트리를 DB 레벨에서 걸러낸다.
    - 결과는 `PostCommentCteRow`로 매핑되며, 서비스 계층에서는 이를 트리 구조로 재조립한다.
4. **Aggregation 접근**
    - 동일한 `postId` 레코드를 JPA로 모두 읽어온 뒤, Kotlin에서 직접 부모-자식 연결과 누적 스코어 계산을 수행한다.
5. **보조 로직**
    - `PostCommentController`는 `measureNanoTime`으로 각 메서드의 실행 시간을 측정해 `durationMillis`에 저장하므로, 애플리케이션 내부 지표와 k6 지표를 쉽게 비교할 수 있다.

## 6. 모니터링 스택 준비
1. **Prometheus & Grafana 기동**
   ```bash
   docker compose up -d
   ```
    - `monitoring/prometheus/prometheus.yml`: 5초 주기로 `host.docker.internal:8080/actuator/prometheus`를 스크레이프한다.
    - Grafana는 `monitoring/grafana/provisioning`에 포함된 Datasource/Dashboard 설정을 자동으로 불러온다.
    - 기본 로그인: `admin / admin`.
2. **대시보드**
    - `demo/jvm-micrometer.json`: JVM, HTTP, DB 등 Micrometer 메트릭을 확인.
    - `demo/k6-prometheus.json`: k6 Prometheus Remote Write 데이터를 시각화(요청률, 응답시간, Trend 메트릭 등).
3. **k6 ↔ Prometheus 연동(선택)**
   ```bash
   K6_OUT=experimental-prometheus-rw \
   K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
   k6 run k6/top-comment-trees-cte.js
   ```
    - `docker-compose.yml`에서 Prometheus `remote-write-receiver` 기능을 활성화했으므로, k6에서 직접 Prometheus로 메트릭을 밀어 넣을 수 있다.

## 7. k6 부하 테스트 시나리오
1. **공통 옵션**
    - `BASE_URL`: 애플리케이션 주소 (기본 `http://localhost:8080`)
    - `POST_ID`: 실험에 사용할 게시글 ID
    - `LIMIT`, `LIMIT_MIN`, `LIMIT_MAX`: `limit` 파라미터 범위
    - `STAGES`: `"30s:10,1m:50,30s:0"` 형태로 VU 변화를 정의 (없으면 스크립트 기본값 사용)
2. **CTE 시나리오 실행 예**
   ```bash
   POST_ID=1 LIMIT_MIN=1 LIMIT_MAX=5 \
   STAGES="30s:10,2m:100,30s:0" \
   k6 run k6/top-comment-trees-cte.js
   ```
3. **Aggregation 시나리오 실행 예**
   ```bash
   POST_ID=1 LIMIT=3 \
   STAGES="30s:10,2m:100,30s:0" \
   k6 run k6/top-comment-trees-aggregation.js
   ```
4. **수집 메트릭**
    - `http_req_duration`, `http_req_failed`
    - 사용자 정의 Trend: `cte_service_duration`, `aggregation_service_duration` (응답 페이로드의 `durationMillis` 값 기반)
5. **비교 절차**
    1. 동일한 데이터셋/limit/스테이지를 사용해 CTE 시나리오를 먼저 실행(혹은 번갈아 가며 2~3회 반복).
    2. Aggregation 시나리오를 동일 조건으로 실행.
    3. Prometheus/Grafana에서 두 Trend와 HTTP 지표를 나란히 조회해 p95, p99, 평균, VU 대비 throughput 등을 비교한다.

## 8. 실험 런북
1. **사전 점검**
    - DB 연결, `post_comment` 데이터 건수, 애플리케이션 로그(쿼리/예외) 확인.
    - Prometheus와 Grafana 대시보드가 정상적으로 데이터를 수신하는지 검증.
2. **Warm-up**
    - 각 엔드포인트에 대해 30초 정도의 저부하 테스트를 수행하여 캐시/JIT 영향을 줄인다.
3. **본 실험**
    - 목표 부하(VU, RPS, 기간)를 정의하고 CTE → Aggregation 순으로 실행.
    - 각 실행마다 k6 출력, Grafana 스냅샷, DB 주요 메트릭(슬로우 쿼리 로그/Performance Schema)을 보관.
4. **분석**
    - k6 Trend 비교: `cte_service_duration` vs `aggregation_service_duration`.
    - HTTP 지연과 에러율 비교.
    - Prometheus에서 JVM 메모리/스레드, DB 연결 수 등 시스템 리소스 변화를 확인.
    - 필요 시 MySQL `EXPLAIN ANALYZE` 결과나 `SHOW PROFILE`로 쿼리 비용을 추가 조사.
5. **결론 도출**
    - 데이터셋 크기별/limit별 차이, CPU/메모리 사용량, 쿼리 부하를 기반으로 CTE 접근이 유리한 조건과 애플리케이션 집계가 더 적합한 조건을 문서화한다.

## 9. 모범 기록 템플릿
| 항목 | 기록 예시 |
| --- | --- |
| 실험 일시 | 2025-02-03 14:00 KST |
| 데이터셋 | `post_comment_dummy_data.sql`, `POST_ID=5`, `limit 1~5` |
| 부하 프로파일 | `30s:20,2m:120,1m:200,30s:0` |
| 평균 응답시간 | CTE 85ms / Aggregation 190ms |
| p95 응답시간 | CTE 120ms / Aggregation 310ms |
| 애플리케이션 CPU | 45% vs 65% |
| 주의 사항 | Aggregation 경로에서 GC가 잦음 |

## 10. 트러블슈팅 체크리스트
- **`Communications link failure`**: MySQL 포트/방화벽, `application.yml` 자격 증명 확인.
- **`BadSqlGrammarException`**: 실험용 SQL이 최신 스키마와 맞지 않을 때. `post_comment` 테이블 컬럼을 재확인.
- **Prometheus가 스크레이프 실패**: 애플리케이션이 호스트에서 실행 중인지, 방화벽/포트 충돌이 없는지 점검. Docker Desktop을 사용할 경우 `host.docker.internal` 지원 여부 확인.
- **k6 실행 시 Stage 파싱 오류**: `STAGES` 값 포맷을 `"duration:vus"` 형태로 맞춘다 (예: `"30s:10,1m:50,30s:0"`).
- **Grafana 대시보드가 비어 있음**: 데이터소스 상태 확인 후 필요하면 `docker compose down -v && docker compose up -d`로 재배포.

---
이 문서를 기반으로 실험 로그와 수집된 메트릭을 꾸준히 정리하면, CTE 도입 여부를 결정할 때 근거가 되는 데이터를 쉽게 확보할 수 있다. 필요 시 추가 지표(예: MySQL slow query log, Performance Schema, flame graph 등)를 연동해 분석 범위를 확장할 수 있다.
