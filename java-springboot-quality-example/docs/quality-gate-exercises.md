# 품질 게이트 위반 재현 실습

이 예제 폴더에서 실행한다. 한 번에 한 항목만 변경하고, 각 실습의 변경만 복원한 뒤 다음으로 넘어간다.
기존 작업을 지우는 reset 명령은 사용하지 않는다. 임시 코드와 실패 리포트는 커밋하지 않는다.
먼저 `mise run ci`가 통과하는지 확인한다. 검사 중에는 자동 포맷을 실행하지 않는다.
아래 명령은 mise의 고정 JDK를 사용하며, 명시한 파일은 모두 기존 파일이다.

## 1. ArchUnit: 서비스에서 JPA 구현 인터페이스 직접 참조

대상: `src/main/java/com/example/quality/application/todo/TodoService.java`.
기존 도메인 인터페이스 import와 생성자는 유지하고 아래 import를 추가한다.

```java
import com.example.quality.repository.todo.JpaTodoRepository;
```

서비스에 다음 메서드를 추가한다. 생성자 타입을 유지하므로 기존 단위 테스트도 컴파일된다.

```java
public Class<?> persistenceType() {
  return JpaTodoRepository.class;
}
```

```shell
mise exec -- ./gradlew architectureTest
```

예상: application → repository 참조를 금지하는 규칙이 실패한다.
수정: 추가한 메서드와 import를 제거한다. 서비스는 domain의 TodoRepository만 참조한다.
같은 명령을 다시 실행하면 통과한다. 클래스 리터럴도 바이트코드 의존이므로 탐지 대상이다.

## 2. Checkstyle: 잘못된 메서드명

대상: `src/main/java/com/example/quality/domain/todo/Todo.java` 클래스 내부에 추가한다.

```java
public String BadName() {
  return title;
}
```

```shell
mise exec -- ./gradlew checkstyleMain
```

예상: MethodName 규칙에서 실패한다.
수정: 메서드명을 `goodName`으로 바꾸고 같은 명령을 실행하면 통과한다.
Spotless는 이 이름을 고치지 않는다. 실습 종료 시 추가한 메서드를 제거한다.

## 3. PMD: 사용하지 않는 지역변수

대상: `src/main/java/com/example/quality/domain/todo/Todo.java` 클래스 내부에 추가한다.

```java
public String titleForDisplay() {
  int count = 1;
  return title;
}
```

```shell
mise exec -- ./gradlew pmdMain
```

예상: UnusedLocalVariable 규칙에서 count가 보고되고 실패한다.
수정: `int count = 1;`만 제거한 뒤 같은 명령을 실행하면 통과한다.
PMD가 의도적인 미사용으로 취급할 수 있는 `unused` 접두사는 실습 변수명으로 쓰지 않는다.
실습 종료 시 추가한 메서드를 제거한다.

## 4. SpotBugs: 확정적인 null 역참조

대상: `src/main/java/com/example/quality/domain/todo/Todo.java` 클래스 내부에 추가한다.
이 메서드는 호출하지 않는다. SpotBugs는 컴파일된 바이트코드를 검사한다.

```java
public int titleLength() {
  String text = null;
  return text.length();
}
```

```shell
mise exec -- ./gradlew spotbugsMain
```

예상: null 역참조 finding으로 실패한다. HTML/XML에서 NP 계열 bug pattern과 소스 위치를 확인한다.
수정 예시는 다음과 같다. 같은 명령을 실행해 finding이 사라지는지 확인한다.

```java
public int titleLength() {
  return title == null ? 0 : title.length();
}
```

실습 종료 시 추가한 메서드를 제거한다. 단순히 exclude-filter에 넣는 것은 결함 수정이 아니다.

## 5. JaCoCo: 도달하지 않은 코드가 있는 상태에서 100% 요구

대상: `build.gradle.kts`의 첫 번째 bundle 규칙에서 LINE minimum만
`"0.80".toBigDecimal()`에서 `"1.00".toBigDecimal()`로 임시 변경한다.
애플리케이션 클래스별 규칙이나 BRANCH 값은 바꾸지 않는다.

```kotlin
limit {
    counter = "LINE"
    minimum = "1.00".toBigDecimal()
}
```

```shell
mise run coverage
```

예상: 현재 테스트가 main 진입 메서드를 호출하지 않으므로 LINE 기준에서 실패한다.
리포트의 빨간 줄을 확인한다. 테스트 자체는 통과해도 coverage gate는 실패할 수 있다.
수정: 실습에서 바꾼 minimum을 원래 0.80으로 복원하고 같은 명령을 재실행해 통과를 확인한다.
이는 임시 강화한 기준의 복원이며, 업무 코드의 미검증 분기를 숨기기 위한 기준 하향 예시가 아니다.
실제 커버리지 부족은 필요한 동작 테스트로 보완하고 직접 작성한 코드를 제외하지 않는다.

## 6. Spotless: 포맷 차이

대상: `src/main/java/com/example/quality/domain/todo/Todo.java`의 기존 getId를 다음처럼 바꾼다.

```java
public Long getId(){return id;}
```

```shell
mise exec -- ./gradlew spotlessCheck
```

예상: 해당 파일의 포맷 diff가 출력되고 실패한다. 수정과 재검증:

```shell
mise run format
mise exec -- ./gradlew spotlessCheck
```

수정 결과는 다음과 같으며 검사는 통과한다.

```java
public Long getId() {
  return id;
}
```

## 마무리

추가 메서드·import와 임계치 변경을 모두 복원하고 아래를 실행한다.

```shell
git diff -- src build.gradle.kts
mise run ci
```

예상: 여섯 도구와 테스트가 모두 통과한다. 실패 검증 때는 개별 태스크를 사용해야 다른 도구의
선행 실패와 혼동하지 않는다. 캐시 상태가 의심되면 해당 Gradle 명령에 `--rerun-tasks`를 추가한다.
