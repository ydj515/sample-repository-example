-- 1. 이중 쓰기 작업

-- Anti-pattern: Dual Writes => 아래는 예시고, 요즘은 트랜잭셔널 아웃박스 (Transactional Outbox) 패턴 권장
public void registerProduct(Product product) {
    --  1. MySQL에 저장
    mysqlRepository.save(product);

-- 2. Elasticsearch에 인덱싱
    try {
        elasticsearchClient.index(product);
} catch (Exception e) {
        -- ??? 어떻게 처리해야 할까? 롤백? 재시도? 무시?
    }

--  3. 캐시 업데이트
    redisClient.update(product);
}

-- 2. 배치 폴링

-- 5분마다 실행되는 스케줄러
SELECT * FROM products WHERE updated_at > '5분 전 시간';

-- Debezium

{
  "schema": { ... }, // 메시지 구조를 정의하는 스키마 정보
  "payload": {
    "before": { // 변경 전 데이터
      "id": 123,
      "name": "Old Product Name",
      "price": 10000.00
    },
    "after": { // 변경 후 데이터
      "id": 123,
      "name": "New Product Name",
      "price": 12000.00
    },
    "source": { // 변경이 발생한 소스 정보
      "version": "1.9.7.Final",
      "connector": "mysql",
      "name": "mysql.prod-db.main",
      "ts_ms": 1678886400000,
      "db": "mydatabase",
      "table": "products",
      "server_id": 1,
      "gtid": null,
      "file": "mysql-bin.000123", // Binlog 파일명
      "pos": 4567,               // Binlog 위치
      "row": 0
    },
    "op": "u", // Operation 타입: 'c'(create), 'u'(update), 'd'(delete)
    "ts_ms": 1678886400500, // Debezium이 이벤트를 처리한 시간
    "transaction": null
  }
}

-- 1. 멱등성 보장
    -> 헤더에 idempotency-key 추가
    -> db, redis를 통해 구현 가능.
        -> 메시지 ID 확인: 메시지를 받으면 먼저 Redis에 해당 message_id가 키로 존재하는지 확인합니다.
        -> 존재하면: 이미 처리된 메시지이므로 무시하고 종료합니다.
        -> 존재하지 않으면: 아직 처리되지 않은 메시지이므로
        -> 실제 비즈니스 로직을 실행합니다. 로직이 성공적으로 끝나면, message_id를 키로 하여 Redis에 기록합니다. 이때 적절한 만료 시간(TTL)을 설정하여 데이터가 무한정 쌓이는 것을 방지합니다.

-- 2. 메시지 처리 순서 보장(파티션별로만 순서 보장)
