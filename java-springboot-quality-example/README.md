# Java Spring Boot Quality Example

Java 21 + Spring Boot 3.5.16 + H2 예제로, 여섯 품질 도구의 규칙 선택과 빌드 실패 기준을 보여준다.
업무 기능은 Todo 등록·조회만 제공한다. 수정·삭제까지 포함한 CRUD 예제는 아니다.
Gradle Wrapper는 8.14.5이며 플러그인과 분석 엔진 버전은 [version catalog](gradle/libs.versions.toml)에 고정한다.

## 실행과 전체 검증

인접 예제와 template-project의 mise 구성을 따라 Java 21.0.2를 고정한다.
이 버전은 레포의 재현 가능한 실행 기준이며 최신 보안 패치 버전이라는 의미는 아니다.
`mise.toml`은 mise 2026.9.18 이상을 기준으로 한다.

```shell
mise trust
mise run bootstrap
mise run config:check
mise run format
mise run ci
mise run run
```

`bootstrap`은 JDK와 Wrapper 실행 환경만 준비하며 애플리케이션을 띄우지 않는다.
H2를 사용하므로 외부 DB용 `.env`, Docker, 별도 infra 태스크가 필요하지 않다.
Gradle은 mise에서 별도 설치하지 않고 커밋된 Wrapper를 사용한다.

| mise 태스크 | 역할 |
| --- | --- |
| default / run | 애플리케이션 실행 |
| bootstrap | 고정 JDK 설치 및 Gradle 런타임 확인 |
| config:check | mise 태스크 참조 검증 |
| test | 단위·H2 통합 테스트와 커버리지 리포트 |
| build | 품질 검사와 실행 JAR 빌드 |
| check / ci | 전체 품질 검사 / 설정 검증 후 전체 품질 검사 |
| lint | Spotless·Checkstyle·PMD·SpotBugs 검사 |
| format | Java 소스 포맷 수정 |
| architecture-test | ArchUnit 검사 |
| coverage | 테스트 실행과 커버리지 기준 검증 |

Gradle 실행 JDK도 21을 사용한다. 컴파일 toolchain 설정만으로 Gradle daemon의 JDK가 바뀌지는 않는다.
Docker나 외부 DB는 필요 없다. 첫 실행에는 Maven Central과 Gradle 플러그인 저장소 접근이 필요하다.

```shell
./gradlew --version
./gradlew spotlessApply
./gradlew check
./gradlew bootRun
```

`spotlessApply`는 개발자가 포맷을 수정할 때 사용한다. CI에서는 소스를 변경하지 않는
`./gradlew check --console=plain`을 실행하고 종료 코드가 0이 아니면 실패로 처리한다.
`build`도 `check`를 포함하므로 배포용 JAR까지 필요하면 `./gradlew build`를 사용한다.

```shell
curl -i -H 'Content-Type: application/json' \
  -d '{"title":"learn quality gates"}' http://localhost:8080/todos
curl -i http://localhost:8080/todos
```

H2는 메모리 DB이며 프로세스 종료 시 데이터가 사라진다. `create-drop`은 이 예제 전용이다.
H2 웹 콘솔은 기본 비활성화한다. 운영 DB와 H2의 SQL·잠금 동작이 같다는 보장은 없다.
서비스는 null/공백 제목을 거부하지만, API의 표준 오류 응답과 페이지 조회는 이 예제 범위 밖이다.

## 패키지와 의존 방향

```text
com.example.quality
├── presentation/todo/TodoController
├── application/todo/TodoService
├── domain/todo
│   ├── Todo
│   └── TodoRepository                 순수 Java 인터페이스
└── repository/todo
    └── JpaTodoRepository              JpaRepository + TodoRepository 상속
```

서비스는 도메인의 `TodoRepository`만 주입받는다. `JpaTodoRepository`는 두 인터페이스를
함께 상속하고 Spring Data가 생성하는 프록시가 실제 구현을 제공한다. 별도 Impl 클래스는 없다.
도메인 인터페이스의 `<S extends Todo> S save(S todo)`는 Spring Data의 저장 메서드와
시그니처를 맞추기 위한 것으로 Spring 타입 자체를 노출하지 않는다.

허용 방향은 presentation → application → domain, repository → domain이다.
presentation은 응답 직렬화를 위해 도메인 엔티티를 사용할 수 있지만 저장소에 직접 접근할 수 없다.
현재 `Todo`에 JPA 매핑을 허용하는 실용적인 계층 구조이며, 완전히 영속성 독립적인 도메인 모델은 아니다.
트랜잭션 경계는 서비스 메서드에 두며 조회는 `readOnly = true`로 선언한다.
도메인 저장소의 메서드명도 Spring Data 파생 쿼리 규약에 맞춰져 있다는 결합은 남아 있다.

## 도구별 책임과 실패 기준

| 도구 | 책임 | 적용 범위 | 실패 기준 |
| --- | --- | --- | --- |
| ArchUnit | 계층 의존·저장소 위치·순환 참조 | main 바이트코드 | 아키텍처 규칙 위반 |
| Checkstyle | 명명·소스 구조 규약 | main/test Java | 오류 또는 경고 1건 이상 |
| PMD | 소스 오류 패턴·복잡도 | main/test Java | 선택한 규칙 위반 |
| SpotBugs | 바이트코드 결함 후보 | main/test 클래스 | MEDIUM 이상 confidence finding |
| JaCoCo | 테스트 실행 커버리지 | 모든 직접 작성한 main 클래스 | 전체 LINE 80%, BRANCH 70% 미달 |
| Spotless | 코드 포맷 | src 아래 Java | Google Java Format 결과와 차이 |

애플리케이션 클래스별로는 LINE 90%, BRANCH 80%를 추가 검증한다.
이 수치는 예제의 시작 기준이며, 커버리지 비율만으로 검증 품질을 판단하지 않는다.
리포트만 생성하고 빌드를 성공시키는 구성이 되지 않도록 모든 검사를 `check`에 연결한다.

## ArchUnit: 의존 규칙을 테스트로 고정

[ArchitectureTest](src/test/java/com/example/quality/ArchitectureTest.java)는 다음을 검사한다.

- domain은 Spring, application, presentation, 구현 저장소 패키지에 의존하지 않는다.
- application은 presentation과 구현 저장소 패키지에 의존하지 않는다.
- presentation은 도메인 저장소와 구현 저장소에 직접 의존하지 않는다.
- repository는 application과 presentation에 의존하지 않는다.
- 도메인 저장소는 인터페이스이고 JPA 저장소는 repository 패키지에 위치한다.
- 최상위 계층 패키지 사이에 순환 의존이 없어야 한다.

`ImportOption.DoNotIncludeTests`로 테스트 코드를 분석 대상에서 제외한다.
빈 대상 규칙을 성공 처리하지 않는다. 패키지 이름을 변경하면 규칙도 함께 변경한다.
`test`에서 `*ArchitectureTest*`를 제외하고 별도의 `architectureTest`에서만 실행해 중복을 막는다.
ArchUnit은 런타임 DI를 보장하지 않으므로 H2 통합 테스트가 프록시 주입과 실제 저장·조회를 검증한다.

```shell
./gradlew architectureTest
```

규칙을 확인하려면 서비스가 `JpaTodoRepository`를 직접 참조하도록 임시 변경해 실패를 확인한 뒤 복원한다.
도메인 엔티티의 JPA 매핑은 의도적으로 허용하므로 `jakarta.persistence` 전체 금지 규칙은 두지 않는다.

## Checkstyle: 포맷과 팀 규약 분리

[checkstyle.xml](config/checkstyle/checkstyle.xml)은 이름과 구조 규칙을 담당한다.
들여쓰기·줄바꿈·import 정렬은 Spotless에 맡겨 서로 다른 포맷 기준이 충돌하지 않게 한다.

| 규칙 | 목적 |
| --- | --- |
| AvoidStarImport, UnusedImports | 의존 타입을 명시하고 불필요한 import 방지 |
| TypeName, MethodName, MemberName | 타입·메서드·필드 이름 규약 |
| ParameterName, LocalVariableName | 매개변수·지역변수 이름 규약 |
| OneTopLevelClass, OuterTypeFilename | 소스 파일과 타입의 대응 유지 |
| NeedBraces | 조건·반복문 블록의 중괄호 강제 |
| EqualsHashCode | equals/hashCode 구현의 짝 확인 |
| FallThrough | 의도하지 않은 switch fall-through 방지 |
| ModifierOrder, FinalClass | 접근 한정자 순서와 상속 불가능한 클래스 표현 |

`maxWarnings = 0`, `isIgnoreFailures = false`로 경고도 통과시키지 않는다.
현재 활성 suppression은 없다. 예외가 필요하면 파일 전체 제외 대신 규칙·클래스·라인 범위를
좁힌 suppression을 추가하고 사유·담당자·제거 조건을 함께 기록한다.

```shell
./gradlew checkstyleMain checkstyleTest
```

예를 들어 `public void BadName()`은 MethodName 위반이다. Spotless는 메서드명을 고치지 않으므로
자동 포맷을 적용한 뒤에도 Checkstyle에서 실패한다.

## PMD: 오류 패턴과 복잡도

[ruleset.xml](config/pmd/ruleset.xml)은 카테고리 전체를 켜지 않고 필요한 규칙을 명시한다.
`ruleSets = emptyList()`는 기본 규칙 집합의 암묵적 추가를 막고 이 파일을 기준으로 사용하게 한다.

| 선택 규칙 | 확인할 문제 |
| --- | --- |
| UnusedLocalVariable, UnusedPrivateMethod | 사용하지 않는 코드 |
| AvoidReassigningParameters | 입력 파라미터를 재대입하는 흐름 |
| UseTryWithResources, CloseResource | 자동 자원 해제와 누락 |
| EmptyCatchBlock, ReturnFromFinallyBlock | 예외 유실 또는 반환값 덮어쓰기 |
| BrokenNullCheck, CompareObjectsWithEquals | 잘못된 null 조건과 객체 동일성 비교 |
| CyclomaticComplexity | 메서드 10, 클래스 합계 60 이상 보고 |
| CognitiveComplexity | 인지 복잡도 15 이상 보고 |

복잡도 임계치는 성능 수치가 아니라 검토 시작점이다. 조건문 수를 줄이기 위한 기계적 분리보다
업무 책임과 테스트 경계를 먼저 검토한다. 분석 스레드는 1로 고정해 Gradle 병렬 실행과 중첩을 제한한다.

```shell
./gradlew pmdMain pmdTest
```

오탐으로 확인된 경우에만 메서드에 `@SuppressWarnings("PMD.규칙명")`을 사용하고 이유를 주석으로 남긴다.
`NOPMD`로 여러 규칙을 한꺼번에 숨기거나 category 전체를 제외하지 않는다.
예를 들어 사용하지 않는 지역변수 `int count = 1;`를 넣으면 UnusedLocalVariable에서 실패한다.
PMD는 `unused`로 시작하는 변수명을 의도적인 미사용으로 취급할 수 있으므로 위반 확인용 이름으로 쓰지 않는다.

## SpotBugs: 바이트코드 결함 후보

플러그인 6.0.27과 엔진 4.8.6을 별도 고정한다. `effort = MAX`는 분석 노력 수준,
`reportLevel = MEDIUM`은 보고할 confidence 범위이며 MEDIUM과 HIGH를 포함한다.
LOW finding은 현재 gate에 포함하지 않는다. confidence와 업무 영향도는 같은 개념이 아니다.
MAX는 실행 비용이 증가할 수 있으므로 큰 저장소에 적용할 때 CI 실행 시간을 측정한다.

`spotbugsMain`과 `spotbugsTest`를 모두 검사하고 XML·HTML을 생성한다.
현재 [exclude-filter.xml](config/spotbugs/exclude-filter.xml)은 비어 있다.
근거 없이 Spring CGLIB 클래스나 도메인 패키지 전체를 제외하지 않는다.

```shell
./gradlew spotbugsMain spotbugsTest
```

다음은 오탐을 재현한 뒤 추가할 수 있는 필터 형식 예시이며 현재 활성 설정은 아니다.
class, method, bug pattern을 함께 지정하고 원인·담당자·제거 조건을 주석으로 남긴다.

```xml
<Match>
  <Class name="com.example.quality.repository.legacy.LegacyAdapter"/>
  <Method name="getDelegate"/>
  <Bug pattern="EI_EXPOSE_REP"/>
</Match>
```

리포트에서 소스 위치와 호출 흐름을 확인한 후 수정 또는 예외를 결정한다.
SpotBugs는 의존성 취약점 검사와 런타임 테스트를 대체하지 않는다.

## JaCoCo: 리포트와 검증의 범위를 동일하게

직접 작성한 엔티티·진입점까지 모든 main 클래스를 포함한다.
커버리지를 통과시키기 위한 `@Generated` 표시나 엔티티 전체 제외는 사용하지 않는다.
애플리케이션 클래스별 기준을 추가해 다른 클래스의 높은 커버리지가 서비스의 미검증 경로를 가리지 않게 한다.

`test`는 단위 테스트와 H2 통합 테스트를 함께 실행하고 `test.exec`에 기록한다.
`architectureTest`는 바이트코드 구조 검사이므로 커버리지 데이터에 합산하지 않는다.
`jacocoTestReport`는 `test`에 의존하고, 검증 태스크는 리포트에 의존한다.
`test`의 finalizer로 리포트 생성을 요청하므로 테스트 실패 시에도 실행 경로를 조사할 수 있다.

```shell
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

H2 통합 테스트는 POST로 등록한 뒤 `flush`와 `clear`를 수행하고 GET으로 다시 조회한다.
1차 캐시에서만 반환하는 테스트가 되지 않게 하며, 테스트 트랜잭션은 종료 시 롤백한다.
단위 테스트는 null·공백·정상 제목을 구분해 분기를 검증한다.
HTTP 통합 테스트는 MockMvc를 사용하므로 실제 TCP 서버나 운영 DB 동작까지 보장하지 않는다.

향후 integrationTest를 별도 Test 태스크로 분리하면 해당 태스크를 report·verification의
선행 작업과 executionData 양쪽에 연결해야 한다. 기존 test.exec만 읽어서는 통합 커버리지가 반영되지 않는다.

## Spotless: 개발자 자동 수정, CI 변경 감지

플러그인 7.0.2와 Google Java Format 1.25.2를 별도로 고정한다.
`src/**/*.java`만 대상으로 하므로 build 산출물과 Gradle Kotlin DSL은 포맷 대상이 아니다.
unused import 제거, 후행 공백 제거, 마지막 개행을 적용한다.
`.editorconfig`는 편집기의 기본값을 제공하고 Java의 최종 포맷은 Google Java Format이 결정한다.

```shell
./gradlew spotlessApply
git diff -- src
./gradlew spotlessCheck
```

신규 예제이므로 ratchet을 적용하지 않고 전체 소스를 검사한다.
기존 대형 저장소에 도입할 때 `ratchetFrom`을 선택하면 기준 ref를 CI에서 fetch해야 한다.
변경 파일만 검사하는 기간과 전체 검사로 전환할 조건을 정하고, 포맷 변경과 기능 변경은 분리한다.

## 리포트와 CI 적용

모든 경로는 이 예제 디렉터리 기준이다.

| 검사 | 리포트 |
| --- | --- |
| ArchUnit | build/reports/tests/architectureTest/index.html |
| 단위·H2 통합 테스트 | build/reports/tests/test/index.html |
| Checkstyle | build/reports/checkstyle/main.html, test.html 및 XML |
| PMD | build/reports/pmd/main.html, test.html 및 XML |
| SpotBugs | build/reports/spotbugs/main.html, test.html 및 XML |
| JaCoCo | build/reports/jacoco/test/html/index.html, test/jacocoTestReport.xml |
| Spotless | 콘솔의 파일별 포맷 차이 |

CI의 working directory를 이 폴더로 지정하고 `mise run bootstrap`, `mise run ci`를 실행한다.
mise를 사용하지 않는 환경에서는 JDK 21에서 `./gradlew check --console=plain`으로 동일한 Gradle gate를 실행한다.
실패 시에도 `build/reports/**`와 `build/test-results/**`를 artifact로 보관하면 원인을 추적하기 쉽다.
실제 GitHub Actions workflow는 이 예제에 추가하지 않는다.

```shell
./gradlew check --dry-run
./gradlew check --rerun-tasks --console=plain
```

최초 도입 시 NO-SOURCE나 실행된 테스트 0건을 성공 근거로 삼지 않는다.
규칙 변경 시 위반 소스를 임시로 넣어 개별 태스크가 실패하는지 확인한 뒤 복원하고 전체 검사를 다시 실행한다.
임계치 하향·패키지 제외·baseline 추가는 검증 범위를 줄이는 변경이므로 근거와 후속 해소 계획을 검토한다.

## 참고

설정 구성은 인접 workflow 저장소의 `dev-standards/standards/tools/languages/java/`와
`dev-standards/templates/gradle/`를 참고했다.

- [ArchUnit 가이드](https://www.archunit.org/userguide/html/000_Index.html)
- [Gradle Checkstyle](https://docs.gradle.org/8.14.5/userguide/checkstyle_plugin.html)
- [PMD 7.8.0 Java 규칙](https://docs.pmd-code.org/pmd-doc-7.8.0/pmd_rules_java.html)
- [SpotBugs Gradle 플러그인](https://github.com/spotbugs/spotbugs-gradle-plugin)
- [Gradle JaCoCo](https://docs.gradle.org/8.14.5/userguide/jacoco_plugin.html)
- [Spotless Gradle 플러그인](https://github.com/diffplug/spotless/tree/main/plugin-gradle)
