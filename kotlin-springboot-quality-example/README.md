# Kotlin Spring Boot Quality Example

Kotlin + Spring Boot + H2로 Todo 등록·조회를 구현하고, 다섯 품질 도구를 Gradle의
`check`에 연결하는 예제다. 애플리케이션 구조와 기능 범위는 인접한
[java-springboot-quality-example](../java-springboot-quality-example/README.md)을 따른다.
`koteset`은 테스트 프레임워크인 **Kotest**로 해석했다.

## 실행

아래 명령은 이 예제 디렉터리에서 실행한다.

```shell
cd kotlin-springboot-quality-example
mise trust
mise run bootstrap
mise run ci
mise run run
```

`bootstrap`은 고정 JDK 설치와 Gradle 런타임 확인을 수행한다.
Gradle은 mise로 별도 설치하지 않고 포함된 Wrapper를 사용한다.
H2 메모리 DB를 사용하므로 Docker와 외부 DB 설정은 필요하지 않다.
첫 실행에는 JDK·Gradle·Maven 의존성을 내려받을 네트워크 연결이 필요하다.

```shell
curl -i -H 'Content-Type: application/json' \
  -d '{"title":"learn quality gates"}' http://localhost:8080/todos
curl -i http://localhost:8080/todos
```

POST는 생성된 `id`와 `title`을 HTTP 201로 반환한다.
GET은 ID 오름차순의 전체 목록을 HTTP 200으로 반환한다.
서비스는 null·빈 문자열·공백 제목을 거부한다.
Java 예제와 동일하게 표준 오류 응답 변환, 페이지 조회, 수정·삭제는 다루지 않는다.
따라서 제목 검증 예외를 HTTP 400으로 변환하는 API 계약은 제공하지 않는다.

## 고정 버전과 mise

| 구성 | 버전 |
| --- | --- |
| Java / Gradle Wrapper | 21.0.2 / 8.14.5 |
| Kotlin / Spring Boot | 2.4.10 / 3.5.16 |
| ktlint Gradle plugin / engine | 14.2.0 / 1.8.0 |
| Detekt | 2.0.0-alpha.6 |
| Konsist / Kover | 0.17.3 / 0.9.9 |
| Kotest / Kotest Spring extension | 5.7.2 / 1.1.3 |

Java 예제와 `template-project/kotlin-springboot-sample`의 버전 구성을 참고했다.
최신 버전 목록이나 최신 보안 패치 기준을 의미하지 않는다.
플러그인·테스트 도구 버전은 [version catalog](gradle/libs.versions.toml)에,
JDK 버전은 [mise.toml](mise.toml)에 고정한다.
H2와 Spring 라이브러리 버전은 Spring Boot의 dependency management를 따른다.
Kotlin 컴파일러 버전으로 Detekt의 내부 컴파일러 의존성을 강제 치환하지 않는다.
Detekt는 2.x alpha 버전이며, 업그레이드 시 plugin ID·설정·실행 결과를 함께 확인해야 한다.

`mise.lock`에는 JDK 다운로드 URL과 체크섬을 기록한다.
현재 JDK의 메타데이터가 제공되는 macOS arm64/x64, Linux arm64/x64, Windows x64가 포함된다.
Linux musl 대상 메타데이터는 없으므로 Alpine 환경은 이 lockfile의 지원 대상이 아니다.
mise 태스크의 다중 명령은 POSIX shell 환경을 기준으로 한다.
개인 설정은 Git에서 제외한 `mise.local.toml`에 둔다.

| mise 명령 | 실행 내용 |
| --- | --- |
| `mise run bootstrap` | JDK 준비 및 Wrapper 버전 확인 |
| `mise run run` | H2를 사용하는 Spring Boot 실행 |
| `mise run config:check` | mise 태스크 참조 검증 |
| `mise run format` | Kotlin 및 Gradle Kotlin DSL 자동 포맷 |
| `mise run lint` | ktlint + Detekt 기본·타입 분석 |
| `mise run test` | Kotest 단위·통합 테스트와 Konsist 검사 |
| `mise run architecture-test` | `*KonsistTest`만 실행 |
| `mise run coverage` | 전체 테스트, Kover HTML/XML 및 임계치 검사 |
| `mise run verify` / `ci` / `check` | mise 설정 검증 후 Gradle `check` |
| `mise run build` | 전체 품질 검사 및 실행 JAR 생성 |

`ci`는 로컬에서 호출 가능한 동일 검증 진입점이며, 별도의 원격 GitHub Actions workflow를
설치하지는 않는다. CI에서도 이 디렉터리에서 `mise run ci`를 실행하면 된다.
`format`만 소스를 수정하며 `lint`, `check`, `ci`는 소스를 수정하지 않는다.

## 패키지와 책임

```text
com.example.quality
├── QualityExampleApplication
├── presentation/todo/TodoController
├── application/todo/TodoService
├── domain/todo
│   ├── Todo
│   └── TodoRepository
└── repository/todo/JpaTodoRepository
```

허용 방향은 presentation → application → domain, repository → domain이다.
presentation은 응답 직렬화에 필요한 도메인 엔티티를 참조할 수 있지만 저장소에는 접근하지 않는다.
`TodoService`는 Spring Data 타입 대신 도메인의 `TodoRepository`를 주입받는다.
`JpaTodoRepository`가 `JpaRepository`와 도메인 인터페이스를 함께 상속하며,
Spring Data 프록시가 구현을 제공한다. 별도 어댑터 구현 클래스는 없다.

도메인 저장소의 `fun <S : Todo> save(todo: S): S`는 Spring Data의 메서드 계약에 맞췄다.
Spring 타입은 노출하지 않지만 파생 쿼리 명명 규약과 JPA 엔티티 매핑에는 결합되어 있다.
JPA 엔티티를 순수한 영속성 독립 모델이라고 설명하지 않는다.
등록 트랜잭션과 읽기 전용 조회 트랜잭션은 서비스 메서드에 둔다.

엔티티는 일반 클래스로 정의한다. Kotlin JPA 플러그인이 기본 생성자를 제공하고,
all-open 설정은 JPA 엔티티의 프록시 생성을 지원한다. Kotlin Spring 플러그인은
서비스 등 Spring 클래스의 프록시 생성을 지원한다.

## 도구별 검사 범위

| 도구 | 책임 | 실패 기준 |
| --- | --- | --- |
| ktlint | main/test Kotlin 및 Gradle Kotlin DSL 포맷 | 포맷 규칙 위반 |
| Detekt | main/test 소스의 복잡도와 잠재 오류, 타입 기반 검사 | 활성 규칙 위반 |
| Konsist | 계층 의존 방향, 저장소 선언과 패키지 규칙 | 아키텍처 테스트 실패 |
| Kover | 직접 작성한 모든 main 클래스의 테스트 커버리지 | 전체 LINE 80%, BRANCH 70% 미달 |
| Kotest | 서비스 동작 및 HTTP/H2 통합 시나리오 | 테스트 assertion 실패 |

모든 도구는 [build.gradle.kts](build.gradle.kts)의 `check` 경로에 연결되어 있다.
Konsist는 별도 Gradle 플러그인이 아니라 Kotest에서 실행하는 테스트 라이브러리다.
Kover도 별도의 테스트 프레임워크가 아니라 Kotest 실행 결과를 계측한다.

## ktlint와 Detekt

[.editorconfig](.editorconfig)는 이 예제의 포맷 기준이며 상위 설정에 의존하지 않는다.
ktlint 플러그인과 엔진을 각각 고정해 플러그인의 기본 엔진 버전에 의존하지 않는다.

```shell
./gradlew ktlintFormat
./gradlew ktlintCheck
./gradlew detekt detektMain detektTest
```

[detekt.yml](config/detekt/detekt.yml)은 기본 규칙을 상속하고,
순환 복잡도 허용값 10과 인지 복잡도 허용값 15를 명시한다.
파일에 쓰지 않은 기본 규칙도 활성일 수 있다. naming, import와 줄 길이 등
포맷 소유권은 ktlint에 두고 중복 규칙은 비활성화한다.
`detektMain`과 `detektTest`는 컴파일 classpath와 JVM target 21로 타입 분석을 실행한다.

baseline이나 광범위한 소스 제외는 사용하지 않는다.
유일한 코드 suppression은 진입 함수의 `SpreadOperator`다.
Spring Boot의 vararg API에 시작 인자를 전달하기 위한 한 번의 배열 복사이며,
함수에만 예외와 사유를 기록했다.

## Konsist

[ArchitectureKonsistTest](src/test/kotlin/com/example/quality/ArchitectureKonsistTest.kt)는
production 소스를 대상으로 계층 존재 여부, 의존 방향, 도메인의 Spring 의존 금지,
컨트롤러의 저장소 접근 금지, 저장소 인터페이스 선언을 확인한다.
빈 scope를 통과시키지 않고 네 계층의 파일 존재를 각각 검사한다.
소스 파일을 Gradle test input으로 등록해 소스 변경이 캐시에 가려지지 않도록 한다.

```shell
mise run architecture-test
```

`doesNotDependOn`으로 금지 방향을 명시하고 도메인은 `dependsOnNothing`으로 검사한다.
`dependsOn`에 허용 방향만 나열하는 것만으로 나머지 방향이 차단된다고 가정하지 않는다.
외부 Spring 의존과 도메인 저장소 접근은 import 규칙으로 보완한다.
이 import 검사는 완전 수식 이름이나 reflection을 통한 모든 접근을 검증하는 장치는 아니다.
Spring 프록시 주입과 H2 저장 동작은 통합 테스트가 검증한다.

Konsist 0.17.3의 import는
`com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture`를 사용한다.
참고한 workflow 문서의 짧은 import 경로 대신 실제 라이브러리 API에 맞췄다.

## Kotest와 Kover

[TodoServiceTest](src/test/kotlin/com/example/quality/TodoServiceTest.kt)는
정상 등록·조회, 빈 목록, null·빈 문자열·공백 입력 거부, 저장소 실패 전파를 검사한다.
작은 in-memory fake를 사용하므로 서비스 테스트에는 Spring context가 필요하지 않다.

[TodoIntegrationTest](src/test/kotlin/com/example/quality/TodoIntegrationTest.kt)는
Kotest Spring extension으로 애플리케이션 context를 구성하고 MockMvc와 실제 H2를 사용한다.
빈 목록 조회, HTTP 201과 생성 ID, 여러 Todo의 저장·정렬 조회, 잘못된 JSON의 거부를 확인한다.
각 테스트 전에 DB를 비워 테스트 순서에 의존하지 않게 한다.

```shell
mise run test
mise run coverage
```

단위·H2 통합·Konsist 테스트는 하나의 `test` 태스크에서 실행한다.
Kover는 모든 main 클래스를 동일한 report·verification 범위에 포함하며
엔티티나 진입점을 커버리지 때문에 제외하지 않는다.
임계치 미달은 빌드를 실패시킨다. 커버리지 수치가 assertion의 충분함을 보장하지는 않는다.

주요 리포트:

- 테스트: `build/reports/tests/test/index.html`
- ktlint: `build/reports/ktlint/`
- Detekt: `build/reports/detekt/main.html`, `test.html` 및 SARIF
- Kover HTML: `build/reports/kover/html/index.html`
- Kover XML: `build/reports/kover/report.xml`

## 검증 사례

초기 예제에서 단위 테스트 7개, H2 통합 테스트 3개, Konsist 테스트 4개가 실행된다.
직접 작성한 main 코드를 제외하지 않은 Kover 측정값은 LINE 87.5%, BRANCH 100%다.
코드를 변경하면 테스트 수와 측정값도 달라질 수 있으므로 실제 리포트를 확인한다.

품질 검사가 실제 실패하는지도 다음 임시 변경으로 확인하고 원복했다.

- 컨트롤러에 `JpaTodoRepository` 의존 추가: Konsist 계층 검사와 저장소 접근 검사 실패.
- Kover LINE 기준을 100%로 변경: 측정값 87.5%로 `koverVerify` 실패.
- Detekt 타입 분석: 불필요한 `filter(...).isEmpty()`를 `none(...)`으로 바꾸도록 지적하여 수정.

## 주의사항

- 제약: Todo 등록·전체 조회에 집중한 단일 모듈 예제다. 인증과 API 오류 응답 표준화는 포함하지 않는다.
- 위험: H2 메모리 DB와 `create-drop` 설정은 프로세스 종료 시 데이터를 잃는다. 운영 환경에 사용하지 않는다.
- 예외: H2의 SQL·잠금 동작은 운영 DB와 다를 수 있다. 운영 DB를 도입하면 해당 DB 통합 테스트가 필요하다.

## 참고 문서

- 로컬 `workflow/dev-standards/standards/tools/languages/kotlin/`의 ktlint, Detekt, Konsist, Kover 문서
- 로컬 `workflow/dev-standards/standards/runtime/mise.md` 및 Gradle/mise 템플릿
- 로컬 `template-project/kotlin-springboot-sample`의 Kotlin·Kotest·품질 도구 구성
- [Konsist architecture assertion](https://docs.konsist.lemonappdev.com/writing-tests/architecture-assert)
- [Kotest Spring extension](https://kotest.io/docs/5.5.x/extensions/spring.html)
- [Detekt 호환성 안내](https://detekt.dev/docs/introduction/compatibility/)
