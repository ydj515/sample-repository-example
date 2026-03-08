# OIDC Multi App Example

`oidc-simple-example`를 기반으로, 하나의 Keycloak realm을 두 개의 독립 Spring Boot 앱이 함께 신뢰하는 멀티앱 예제입니다.

## 구성

- `app1`: 포트 `8081`, Keycloak client `oidc-app1`
- `app2`: 포트 `8082`, Keycloak client `oidc-app2`
- `oidc-common`: 공통 OIDC 보안 설정, Redis 세션 조회, 세션 재검증, Keycloak 사용자 매핑
- `redis`: Spring Session 저장소
- `keycloak`: 공통 OIDC Provider

## 빠른 실행

```bash
docker compose up --build
```

접속 주소:

- app1: `http://localhost:8081`
- app2: `http://localhost:8082`
- Keycloak: `http://localhost:9000`

## 예제 계정

- app1 전용 사용자
  - 아이디: `app1-user`
  - 비밀번호: `app1user1234`
- app1, app2 공용 사용자
  - 아이디: `multi-user`
  - 비밀번호: `multi1234`
- app2 전용 사용자
  - 아이디: `app2-user`
  - 비밀번호: `app2user1234`
- app1 관리자
  - 아이디: `app1-admin`
  - 비밀번호: `app1admin1234`
- app2 관리자
  - 아이디: `app2-admin`
  - 비밀번호: `app2admin1234`
- master 관리자
  - 아이디: `master-admin`
  - 비밀번호: `master1234`

## 확인 흐름

1. `http://localhost:8081`에서 로그인합니다.
2. 같은 브라우저에서 `http://localhost:8082`로 이동합니다.
3. `multi-user` 또는 `master-admin`으로 로그인했다면 app2에서도 인증 흐름이 자연스럽게 이어집니다.
4. `app1-user`는 app2에서, `app2-user`는 app1에서 403으로 차단됩니다.

## 로컬 실행

Keycloak과 Redis만 먼저 띄운 뒤, 각 앱을 별도로 실행할 수 있습니다.

```bash
docker compose up keycloak redis
./gradlew :app1:bootRun --args='--spring.profiles.active=local'
./gradlew :app2:bootRun --args='--spring.profiles.active=local'
```

## 주요 엔드포인트

- `GET /public`
- `GET /api/me`
- `GET /api/sensitive`
- `POST /api/admin/users/{username}/logout-all`
