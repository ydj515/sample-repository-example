## API

- 유저 정보 조회 : 유저의 이름, 이메일, 생성 날짜 정보를 조회할 수 있어야 합니다. 유저의 고유한 ID를 입력 받아 해당 유저의 정보를 반환. 데이터베이스에 없는 유저를 조회 시 예외처리
- 유저 추가 : 새로운 유저를 추가할 수 있어야 합니다. 이름과 이메일을 입력받아 새로운 유저를 생성하고, 서버 시간 기준으로 생성 날짜 지정. 정상적으로 유저가 생성되면, 생성된 유저의 정보를 반환

- 제품 목록 조회 : 모든 제품의 이름, 가격, 제품 타입 정보 목록을 조회할 수 있어야 합니다. 제품 타입은 전자제품과 의류가 있고, 전자제품은 보증기간, 의류는 사이즈 정보가 있습니다. 모든 제품의 정보를 반환. 전자 제품일 경우 보증 기간, 의류의 경우 사이즈 정보가 포함
제품 추가 : 새로운 제품을 추가할 수 있어야 합니다. 제품의 이름, 가격, 제품 종류, 종류별 고유 정보를 받아 새로운 제품 생성. 정상적으로 제품이 생성되면, 생성된 제품의 정보를 반환

- 유저 장바구니 조회 : 특정 유저의 장바구니에 담긴 아이템 목록과 총 금액을 조회할 수 있어야 합니다. 유저의 ID를 입력받아 해당 유저의 장바구니 정보를 반환. 장바구니 정보에는 아이템 목록과 등록된 아이템의 총 가격이 포함
- 유저 장바구니 추가 : 특정 유저의 장바구니에서 아이템을 추가할 수 있어야 합니다. 유저의 ID, 추가하려는 제품의 ID, 개수를 입력받아 장바구니에 아이템을 생성. 아이템이 정상적으로 생성되면 유저의 장바구니 정보를 반환
- 유저 장바구니 삭제 : 특정 유저의 장바구니에서 아이템을 삭제할 수 있어야 합니다. 삭제하려는 아이템의 ID와 유저의 ID를 입력받아 해당 아이템을 삭제. 아이템이 정상적으로 삭제되면 유저의 장바구니 정보를 반환

- 새로운 제품 실시간 알림 : 새로운 제품이 추가되면 실시간으로 알림을 받을 수 있어야 합니다. 입력 받은 키워드가 이름에 포함된 제품이 등록되면, 해당 제품 정보를 유저에게 실시간 전송. 키워드를 입력하지 않으면 모든 새로운 제품에 대해 실시간 전송

- 키워드 검색 : 키워드를 입력받아 키워드를 포함하는 이름을 가진 모든 유저, 전자제품, 의류 정보를 반환

User 엔티티
- id: 고유 식별자
- name: 유저 이름
- email: 유저 이메일
- createdAt: 생성 일시


Product 엔티티
- id: 고유 식별자
- name: 유저 이름
- price; 제품 가격($)
- productType: 제품 종류
- warrantyPeriod: 보증기간(전자제품의 고유 정보)
- size: 사이즈(의류의 고유 정보)

Cart 엔티티
- id: 고유 식별자
- userld: 유저의 Id (User 엔티티와 1:1 연관 관계 설정)
  -> 총금액은 계산해서 내려줄 예정

CartItem 엔티티
- id: 고유 식별자
- productld: 제품의 ID (Product 엔티티와 N:1 관계 설정)
- cartid: 장바구니의 ID (Cart 엔티티와 N:1 관계 설정)
- quantity: 수량


---


# Spring Boot + GraphQL 실습 TODO

---

## A. 기능 요구사항 (자연어 Task 정리)

### A-1. 유저 관련

1. 유저 정보 조회
    - 입력: `userId`
    - 동작:
        - 해당 ID의 유저 정보를 조회
        - 없으면 예외 처리 (NotFound)
    - 출력: `id, name, email, createdAt`
    - GraphQL 예: `query user(id: ID!)`

2. 유저 추가
    - 입력: `name, email`
    - 동작:
        - 새로운 유저 생성
        - `createdAt`은 서버 시간 기준 설정
    - 출력: 생성된 유저 정보 (`id, name, email, createdAt`)
    - GraphQL 예: `mutation createUser(...)`

---

### A-2. 제품 관련

3. 제품 목록 조회
    - 입력: 없음
    - 동작:
        - 모든 제품 조회
        - 전자제품인 경우: 보증기간(`warrantyPeriod`) 포함
        - 의류인 경우: 사이즈(`size`) 포함
    - 출력:
        - `name, price, productType` + 타입별 고유 정보
    - GraphQL 예: `query productList`

4. 제품 추가
    - 입력:
        - `name, price, productType(ELECTRONICS/CLOTHING)`
        - 전자제품: `warrantyPeriod`
        - 의류: `size`
    - 동작:
        - 제품 종류에 따라 필수 필드 검증
        - 새 제품 생성
    - 출력: 생성된 제품 정보
    - GraphQL 예: `mutation addProduct(...)`

---

### A-3. 장바구니 관련

5. 유저 장바구니 조회
    - 입력: `userId`
    - 동작:
        - 해당 유저의 장바구니 조회
        - 장바구니에 포함된 아이템 목록 + 총 금액
            - 총 금액 = Σ(각 아이템 `product.price * quantity`)
            - **엔티티에는 총 금액 필드 없음 → GraphQL 리졸버에서 계산**
    - 출력:
        - `items: [CartItem]`, `totalAmount`
    - GraphQL 예: `query cart(userId: ID!)`

6. 유저 장바구니 추가 (아이템 추가)
    - 입력: `userId, productId, quantity`
    - 동작:
        - 해당 유저의 장바구니에 제품을 아이템으로 추가
        - 장바구니 없으면 생성 or 정책 정의
    - 출력: 변경된 장바구니 정보 (`items, totalAmount`)
    - GraphQL 예: `mutation addCartItem(...)`

7. 유저 장바구니 삭제 (아이템 삭제)
    - 입력: `userId, cartItemId`
    - 동작:
        - 해당 유저의 장바구니에서 cartItem 삭제
    - 출력: 변경된 장바구니 정보 (`items, totalAmount`)
    - GraphQL 예: `mutation removeCartItem(...)`

---

### A-4. 실시간 알림 & 검색

8. 새로운 제품 실시간 알림 (Subscription)
    - 입력: `keyword` (옵션)
        - keyword가 있으면: 이름에 keyword가 포함된 신상품만 알림
        - keyword가 없으면: 모든 신상품에 대해 알림
    - 동작:
        - 새 제품 생성 시 이벤트 발행
        - keyword 조건에 맞는 클라이언트에게 실시간 전송
    - 출력: 생성된 제품 정보
    - GraphQL 예: `subscription newProduct(keyword: String)`

9. 키워드 검색
    - 입력: `keyword`
    - 동작:
        - 이름에 keyword가 포함된
            - 모든 유저
            - 모든 전자제품
            - 모든 의류
        - 를 모두 검색해서 하나의 리스트로 반환
    - 출력:
        - `User`, `Electronics`, `Clothing` 타입들을 포함하는 결과 리스트
    - GraphQL 예: `query search(keyword: String!)`

---

## B. 도메인 모델 & 엔티티 정의

### B-1. User 엔티티

- [ ] `User` 엔티티
    - [ ] `id`: 고유 식별자 (PK)
    - [ ] `name`: 유저 이름
    - [ ] `email`: 유저 이메일 (유니크 고려)
    - [ ] `createdAt`: 생성 일시
    - [ ] `cart`: Cart (1:1 연관관계, 선택)

### B-2. Product 엔티티

- [ ] `ProductType` enum
    - [ ] `ELECTRONICS`
    - [ ] `CLOTHING`

- [ ] `Product` 엔티티
    - [ ] `id`: 고유 식별자
    - [ ] `name`: 제품 이름
    - [ ] `price`: 제품 가격
    - [ ] `productType`: 제품 종류 (ELECTRONICS / CLOTHING)
    - [ ] `warrantyPeriod`: 보증기간 (전자제품용 필드)
    - [ ] `size`: 사이즈 (의류용 필드)

### B-3. Cart / CartItem 엔티티

- [ ] `Cart` 엔티티
    - [ ] `id`: 고유 식별자
    - [ ] `userId`: 유저 ID (User와 1:1)
    - [ ] `user`: User (연관관계)
    - [ ] `items`: List<CartItem> (1:N)
    - [ ] **총금액 필드 없음**
        - 총 금액은 `items`의 `product.price * quantity`를 합산해서
            - Service 또는 GraphQL 리졸버에서 계산

- [ ] `CartItem` 엔티티
    - [ ] `id`: 고유 식별자
    - [ ] `productId`: Product ID (N:1)
    - [ ] `cartId`: Cart ID (N:1)
    - [ ] `product`: Product
    - [ ] `cart`: Cart
    - [ ] `quantity`: 수량

---

## C. 프로젝트 기본 설정 (Spring Boot + GraphQL)

- [ ] Gradle/Maven 기반 Spring Boot 프로젝트 생성
    - spring-boot-starter-web
    - spring-boot-starter-data-jpa
    - spring-graphql (또는 사용하려는 GraphQL 스타터)
    - spring-boot-starter-websocket (Subscription용)
    - H2 (인메모리 DB)
    - lombok (선택)

- [ ] 패키지 구조
    - `controller`
    - `service`
    - `domain` (엔티티)
    - `repository`
    - `graphql` (schema, resolver)
    - `config`

- [ ] `application.yml` 설정
    - H2 콘솔
    - JPA ddl-auto=create / update
    - GraphQL endpoint, playground 설정

---

## D. Repository & Service 계층

- [ ] `UserRepository`, `ProductRepository`, `CartRepository`, `CartItemRepository`
- [ ] `UserService`
    - [ ] 유저 조회 (예외 처리)
    - [ ] 유저 생성 (createdAt = 서버 시간)
- [ ] `ProductService`
    - [ ] 제품 목록 조회
    - [ ] 제품 생성 (productType별 필수 필드 검증)
    - [ ] 제품 생성 시 이벤트 발행 (Subscription용)
- [ ] `CartService`
    - [ ] 유저 장바구니 조회
    - [ ] 장바구니 아이템 추가
    - [ ] 장바구니 아이템 삭제
    - [ ] 총 금액 계산 로직 제공

---

## E. GraphQL 스키마 (schema.graphqls)

### E-1. 타입 정의

- [ ] `scalar DateTime`
- [ ] `enum ProductType { ELECTRONICS, CLOTHING }`
- [ ] `type User { id, name, email, createdAt }`
- [ ] `interface Product { id, name, price, productType }`
- [ ] `type Electronics implements Product { ..., warrantyPeriod }`
- [ ] `type Clothing implements Product { ..., size }`
- [ ] `type CartItem { id, product: Product!, quantity }`
- [ ] `type Cart { id, user: User!, items: [CartItem!]!, totalAmount: Int! }`
- [ ] `union SearchResult = User | Electronics | Clothing`

### E-2. Query

- [ ] `user(id: ID!): User`
- [ ] `productList: [Product!]!`
- [ ] `cart(userId: ID!): Cart`
- [ ] `search(keyword: String!): [SearchResult!]!`

### E-3. Mutation

- [ ] `createUser(name: String!, email: String!): User!`
- [ ] `addProduct(...): Product!`
- [ ] `addCartItem(userId: ID!, productId: ID!, quantity: Int!): Cart!`
- [ ] `removeCartItem(userId: ID!, cartItemId: ID!): Cart!`

### E-4. Subscription

- [ ] `newProduct(keyword: String): Product!`

---

## F. 리졸버 구현

- [ ] Query 리졸버
    - [ ] `user`
    - [ ] `productList`
    - [ ] `cart` (totalAmount 계산)
    - [ ] `search` (User + Electronics + Clothing 통합 반환)
- [ ] Mutation 리졸버
    - [ ] `createUser`
    - [ ] `addProduct` (productType별 검증 + Subscription 발행)
    - [ ] `addCartItem`
    - [ ] `removeCartItem`
- [ ] Subscription 리졸버
    - [ ] `newProduct(keyword)` (keyword 필터링)

---

## G. 예외 처리 & 검증

- [ ] NotFound / InvalidInput 예외 정의
- [ ] GraphQL 에러 핸들러 설정
- [ ] 이메일, 가격, 수량, 타입별 필수 값 검증

---

## H. 샘플 데이터 & 테스트

- [ ] 샘플 데이터 삽입 (data.sql or CommandLineRunner)
- [ ] 시나리오별 GraphQL 테스트
    - 유저 CRUD, 제품 CRUD, 장바구니 조회/추가/삭제,
    - 키워드 검색, Subscription 동작 확인

---
