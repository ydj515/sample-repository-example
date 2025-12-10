## GraphQL Example

Spring Boot + GraphQL 예제 프로젝트입니다. TODO에 정의된 요구사항(유저, 제품, 장바구니, 실시간 신상품 알림, 키워드 검색)을 구현합니다.

> 예시 graphQL query sample은 examples 폴더에서 확인 가능합니다.

### 주요 기능
- 유저 조회/생성 (이메일 중복 검증, createdAt 서버시간)
- 제품 조회/생성 (전자제품/의류 타입별 고유 필드, 입력 검증)
- 장바구니 조회/아이템 추가/삭제, 총 금액 계산 리졸버에서 처리
- 신상품 Subscription (선택적 키워드 필터)
- 키워드 검색 (User + Electronics + Clothing 통합 결과)

### 그래프QL 스키마
- `src/main/resources/graphql/` 하위에 Query/Mutation/Subscription 및 타입 정의가 분리되어 있습니다.
- 커스텀 스칼라: `DateTime` (`GraphQLConfig`에서 등록)

### 코드 구조
현재 레이어만 나눈 구조를 채택

```
com.example.graphqlexample
 ├─ domain
 │   ├─ user
 │   ├─ product
 │   └─ cart
 ├─ service
 │   ├─ user
 │   ├─ product
 │   └─ cart
 ├─ web        # REST API (@RestController)
 │   ├─ user
 │   └─ product
 └─ graphql    # GraphQL resolver
     ├─ query
     ├─ mutation
     ├─ subscription
     └─ type   # (필요하면) 타입별 DataFetcher/Resolver
```
- 도메인/엔티티: `src/main/kotlin/com/example/graphqlexample/domain`
- 레포지토리: `src/main/kotlin/com/example/graphqlexample/repository`
- 서비스: `src/main/kotlin/com/example/graphqlexample/service`
- GraphQL 리졸버: `src/main/kotlin/com/example/graphqlexample/graphql`
- 설정 및 초기데이터: `src/main/kotlin/com/example/graphqlexample/config`

> 추후 아래의 패키지로 구성 고려
```
com.example.graphqlexample
 ├─ domain       # 엔티티, 리포지토리 인터페이스, 도메인 서비스
 ├─ application  # use case / service
 └─ presentation
     ├─ rest     # REST Controller
     └─ graphql  # GraphQL Resolver
```

### 실행 방법
```bash
./gradlew bootRun
```
- H2 메모리 DB 사용, `application.yml`에서 콘솔(`/h2-console`) 설정.
- GraphQL Web: `http://localhost:8080/graphiql`
- GraphQL 엔드포인트: `/graphql` (WebSocket 경로 동일).
    - 예시 query
      - endpoint : `POST` http://localhost:8080/graphql
      - query
        ```graphql
        query getUser {
          user(id: 1) {
            id
            name
            email
            createdAt
          }
        }
        ```
      - response
        ```json
        {
          "data": {
            "user": {
              "id": 1,
              "name": "Alice",
              "email": "",
              "createdAt": "2025-11-08T04:16:03.560+09:00"
            }
          }
        }
        ```

> [!NOTE]
> Spring GraphQL 4는 graphql-transport-ws만 지원합니다. subscription을 테스트시 유의하세요.(기존 Subscription type은 Websocket)
> `wscat`으로도 테스트 가능합니다.
> ```shell
> npm install -g wscat
> $ wscat -c ws://localhost:8080/graphql -s graphql-transport-ws
> $ {"type":"connection_init","payload":{}}
> $ {"id":"1","type":"subscribe","payload":{"query":"subscription { newProduct(keyword:\"book\") { id name productType } }"}}
> ```






### 샘플 초기 데이터
- `DataInitializer`에서 Alice/Bob 유저, 전자제품(Laptop Pro), 의류(T-Shirt), 장바구니 아이템을 삽입합니다. 기존 데이터가 있으면 건너뜁니다.

### 예시 쿼리/뮤테이션/서브스크립션
**유저 생성**
```graphql
mutation {
  createUser(input: { name: "Charlie", email: "charlie@example.com" }) {
    id
    name
    email
    createdAt
  }
}
```

**제품 추가 (전자제품)**
```graphql
mutation {
  addProduct(input: {
    name: "Camera",
    price: 800,
    productType: ELECTRONICS,
    warrantyPeriod: 12
  }) {
    id
    name
    productType
    ... on Electronics { warrantyPeriod }
  }
}
```

**장바구니 조회**
```graphql
query {
  cart(userId: 1) {
    id
    totalAmount
    items {
      id
      quantity
      product { id name price productType }
    }
  }
}
```

**신상품 서브스크립션 (키워드 선택)**
```graphql
subscription {
  newProduct(keyword: "pro") {
    id
    name
    productType
  }
}
```

### 테스트
```bash
./gradlew test
```
