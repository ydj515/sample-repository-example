### 아키텍처 패키지 구조

```
com.example.project
├── presentation
│   └── user              # 기능/도메인 단위 그룹
│       ├── UserController.java
│       └── dto
│           ├── UserSignupRequest.java
│           └── UserDetailResponse.java
│
├── application
│   └── user
│       └── UserSignupService.java  # UseCase 서비스
│
├── domain
│   ├── user
│   │   ├── model
│   │   │   └── User.java         # JPA Entity
│   │   ├── service
│   │   │   └── UserValidator.java # 도메인 서비스
│   │   └── repository
│   │       └── UserRepository.java # Repository 인터페이스
│
└── infrastructure
    └── persistence
        └── user
            └── UserRepositoryImpl.java # Repository 구현체
```

-----

### 레이어별 역할 및 DTO 흐름

#### **1. Domain Layer**

* **`domain/user/model`**: **JPA Entity**가 위치합니다. 데이터와 핵심적인 비즈니스 로-직을 가집니다. 예를 들어, `User` 엔티티 내에 `changePassword()` 같은 메서드를 둘 수 있습니다.
* **`domain/user/service`**: **도메인 서비스**가 위치합니다. **하나의 엔티티에 포함시키기 어려운, 여러 도메인 모델을 넘나드는 순수한 비즈니스 로직**을 처리합니다. 예를 들어, '사용자 이메일이 중복되는지 확인'하는 `UserValidator`나 '주문 시 쿠폰 적용 및 재고 차감'을 처리하는 `OrderProcessor` 등이 해당됩니다. 이 서비스는 상태를 가지지 않아야 합니다.
* **`domain/user/repository`**: 데이터 영속성을 위한 **인터페이스**만 정의합니다.

#### **2. Application Layer (`application/service`)**

* **역할**: 애플리케이션 서비스 (Use Case)의 역할을 합니다. 트랜잭션을 시작하고, `domain` 계층의 리포지토리와 도메인 서비스를 호출하여 실제 작업을 위임하고 그 결과를 조합합니다. **Entity를 DTO로 변환하는 책임**을 가집니다.
* **예시**: `UserSignupService`는 요청 DTO를 받아 `UserValidator`를 호출해 중복을 검사하고, `UserRepository`를 통해 `User` 엔티티를 저장한 후, 최종적으로 `UserDetailResponse` DTO를 만들어 반환합니다.

#### **3. Presentation Layer (`presentation/controller`)**

* **역할**: HTTP 요청을 받고 응답하는 역할에만 집중합니다. `application` 서비스 계층을 호출하고, 반환된 DTO를 JSON 형태로 클라이언트에 전달합니다.
* **DTO 사용**:
    * **요청(Request)**: Controller는 `@RequestBody`를 통해 요청 DTO를 받습니다.
    * **응답(Response)**: `application` 서비스로부터 받은 응답 DTO를 `ResponseEntity`에 담아 반환합니다.

-----

네, 알겠습니다. 'DTO 네이밍 규칙 및 사용 가이드'에 방금 논의한 **도메인 결과 객체**에 대한 내용을 통합하여 업데이트해 드리겠습니다.

-----

### DTO 및 도메인 객체 네이밍/사용 가이드

애플리케이션의 각 계층은 명확한 역할을 가지며, 계층 간 데이터 이동 시에는 목적에 맞는 객체를 사용해야 합니다.

#### **1. Presentation 계층 DTO (클라이언트 통신용)**

이 DTO는 **오직 `Presentation` 계층에서만 사용**하며, 외부 클라이언트와의 데이터 교환(Request/Response)을 위해 존재합니다.

* **Request DTO**

    * **역할**: 클라이언트의 요청 데이터를 담는 객체입니다.
    * **네이밍**: `[기능][도메인]Request.java` (e.g., `UserSignupRequest`, `OrderCancelRequest`)
    * **위치**: `presentation/{도메인}/dto/`
    * **특징**: 입력값 검증을 위한 `jakarta.validation` 어노테이션을 포함합니다.

* **Response DTO**

    * **역할**: 클라이언트에게 응답할 데이터를 담는 객체입니다.
    * **네이밍**: `[도메인][상세내용]Response.java` (e.g., `UserDetailResponse`, `OrderSimpleResponse`)
    * **위치**: `presentation/{도메인}/dto/`
    * **특징**: `Application` 서비스가 **Entity를 변환**하여 생성합니다. 민감 정보를 제외하고 화면에 필요한 데이터만 포함해야 합니다.

#### **2. Domain 계층 결과 객체 (내부 통신용)**

이 객체는 **`Domain` 계층의 일부**이며, `Domain Service`가 비즈니스 로직의 결과를 `Application` 계층으로 반환할 때 사용합니다. **절대 `DTO`라는 이름을 사용하지 않습니다.**

* **역할**: 하나의 엔티티로 표현하기 힘든 도메인 로직의 복잡한 결과를 담는 값 객체(Value Object)입니다.
* **네이밍**: `[도메인][정보]Info.java` (e.g., `OrderPriceInfo`)
* **위치**: `domain/{도메인}/service/`
* **특징**: **도메인 모델의 일부**이며, `Application` 계층에서 이 객체를 받아 비즈니스 흐름을 제어하거나 `Response DTO`로 변환하는 데 사용합니다.

#### 데이터 객체 흐름 요약

```
[Client]
   ↑↓
(Request/Response DTO)  <- Presentation 계층에서만 사용
   ↑↓
[Presentation: Controller]
   ↑↓
[Application: Service]      <- Entity를 Response DTO로 변환하는 책임
   ↑↓
(Entity, Domain Result Object) <- Domain 계층의 결과물
   ↑↓
[Domain: Service, Repository]
```

### 코딩 가이드 규칙
1. 한국어로 대답할 것.
2. 리스트조회의 경우 반드시 page 조회를 하고 결과도 page반환을 합니다.
3. lombok을 사용할 수 있는 곳은 lombok을 사용할 것.
4. transaction은 정합성을 지킬수 있는 최소의 범위로 잡을 것.
5. application 계층의 UseCase 서비스는 연관된 서비스들로 분리할것.