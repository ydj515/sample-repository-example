-- 초기 : 스케줄러 & 스크립트의 조합

INSERT INTO daily_product_sales (product_id, total_sales_amount, total_sales_count, target_date)
SELECT
    product_id,
    SUM(sale_price * quantity),
    SUM(quantity),
    '2024-05-20'
FROM
    orders
WHERE
    created_at >= '2024-05-20 00:00:00' AND created_at < '2024-05-21 00:00:00'
GROUP BY
    product_id;

-- 초기에는 괜찮으나 데이터가 많아질수록
-- 1. 처리 시간의 증가
-- 2. DB 부하로 인한 서비스 영향도
-- 3. 장애 대응에 대한 문제

-- 맵 리듀스 : 거대한 데이터 처리 작업을 위해, 분할 정복 방식으로 해결하는 개념
-- 아파치 스파크 : 맵 리듀스를 구현하고, 인메모리 기술로 성능을 극대화 하는 시스템
-- 1. 계산 위임
-- 2. 병렬 데이터 로딩

-- 1. MySQL의 'orders' 테이블을 Spark에서는 병렬로 읽어와 임시 뷰로 정의
-- spark 문법
-- 아래 OPTIONS를 굉장히 잘 조절해야함.
CREATE OR REPLACE TEMPORARY VIEW orders_view
USING "jdbc"
OPTIONS (
  url              "jdbc:mysql://your-mysql-server:3306/service_db",
  dbtable          "orders",
  user             "spark_user",
  password         "spark_password",

  -- ▼ 병렬 읽기를 위한 핵심 옵션들 ▼
  partitionColumn  "order_id",         -- 데이터를 나눌 기준 컬럼 (숫자 타입, PK 권장)
  lowerBound       "1",                -- 기준 컬럼의 최소값
  upperBound       "100000000",        -- 기준 컬럼의 최대값
  numPartitions    "100"               -- 데이터를 몇 개로 나눌지 (파티션 개수). 병렬로 처리하는 단위(100개씩 한번에).
);

-- Executor 1이 실행하는 쿼리
SELECT * FROM orders WHERE order_id >= 1 AND order_id < 1000000;

-- Executor 2가 실행하는 쿼리
SELECT * FROM orders WHERE order_id >= 1000000 AND order_id < 2000000;

-- Executor 100이 실행하는 쿼리
SELECT * FROM orders WHERE order_id >= 99000000 AND order_id <= 100000000;

-- 2. 실제 계산은 Spark 클러스터에서 병렬로 수행
--    (이 쿼리는 더 이상 MySQL이 아닌 Spark 엔진이 실행)
CREATE OR REPLACE TEMPORARY VIEW daily_summary AS
SELECT
    product_id,
    SUM(sale_price * quantity) AS total_amount,
    COUNT(*) AS total_count
FROM
    orders_view
WHERE
    created_at >= '2024-05-20 00:00:00' AND created_at < '2024-05-21 00:00:00'
GROUP BY
    product_id;

-- 3. 집계된 결과를 다시 다른 DB 테이블에 저장
INSERT INTO analytics_db.daily_product_sales
SELECT * FROM daily_summary;

-- Predicate, PushDown
-- 데이터 셔플링