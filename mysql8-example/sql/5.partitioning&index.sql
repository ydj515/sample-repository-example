-- 현재 파티션 상태 확인
SELECT
    PARTITION_NAME,
    PARTITION_EXPRESSION,
    PARTITION_DESCRIPTION,
    TABLE_ROWS,
    ROUND(DATA_LENGTH / 1024 / 1024, 2) as data_size_mb,
    ROUND(INDEX_LENGTH / 1024 / 1024, 2) as index_size_mb
FROM information_schema.partitions
WHERE table_schema = 'financial_master_class'
  AND table_name = 'stock_trades'
  AND PARTITION_NAME IS NOT NULL
ORDER BY PARTITION_ORDINAL_POSITION;


-- 파티션별 성능 분석
SELECT
    PARTITION_NAME,
    ROUND(DATA_LENGTH / (1024 * 1024 * 1024), 2) as data_size_gb,
    TABLE_ROWS,
    ROUND(TABLE_ROWS / (DATA_LENGTH / 1024), 2) as rows_per_kb,
    CASE
        WHEN TABLE_ROWS > 50000000 THEN 'VERY_LARGE'
        WHEN TABLE_ROWS > 10000000 THEN 'LARGE'
        WHEN TABLE_ROWS > 1000000 THEN 'MEDIUM'
        ELSE 'SMALL'
        END as partition_size_category
FROM information_schema.partitions
WHERE table_schema = 'financial_master_class'
  AND table_name = 'stock_trades'
  AND PARTITION_NAME IS NOT NULL;



-- 계좌별 거래 이력 테이블 (Hash 파티셔닝)
CREATE TABLE account_trade_history (
       history_id BIGINT UNSIGNED AUTO_INCREMENT,
       account_id BIGINT UNSIGNED NOT NULL,
       trade_id BIGINT UNSIGNED NOT NULL,
       trade_date DATE NOT NULL,
       symbol VARCHAR(10) NOT NULL,
       trade_type ENUM('BUY', 'SELL') NOT NULL,
       price DECIMAL(12,4) NOT NULL,
       volume INT UNSIGNED NOT NULL,
       trade_value DECIMAL(18,4) NOT NULL,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

       PRIMARY KEY (history_id, account_id)
) ENGINE=InnoDB PARTITION BY HASH(account_id) PARTITIONS 16;  -- 16개 파티션으로 분산


SELECT * FROM account_trade_history WHERE trade_date = '2025-08-04';


-- 시장별 거래 데이터 (List 파티셔닝)
CREATE TABLE market_specific_trades (
    trade_id BIGINT UNSIGNED AUTO_INCREMENT,
    symbol VARCHAR(10) NOT NULL,
    market_code VARCHAR(10) NOT NULL,
    trade_date DATE NOT NULL,
    trade_timestamp TIMESTAMP(6) NOT NULL,
    price DECIMAL(12,4) NOT NULL,
    volume INT UNSIGNED NOT NULL,
    trade_type ENUM('BUY', 'SELL') NOT NULL,

    PRIMARY KEY (trade_id, market_code),
    INDEX idx_symbol_timestamp (symbol, trade_timestamp),
    INDEX idx_date_volume (trade_date, volume)

) ENGINE=InnoDB
    PARTITION BY LIST COLUMNS(market_code) (
        PARTITION p_kospi VALUES IN ('KOSPI'),
        PARTITION p_kosdaq VALUES IN ('KOSDAQ'),
        PARTITION p_nasdaq VALUES IN ('NASDAQ'),
        PARTITION p_nyse VALUES IN ('NYSE'),
        PARTITION p_other VALUES IN ('OTHER', 'CRYPTO', 'FOREX')
        );



-- WHERE - ORDER

-- 자주 실행되는 쿼리
SELECT * FROM users WHERE email = 'user@example.com';

-- 인덱스 생성
CREATE INDEX idx_users_email ON users(email);


-- 좋은 예: 선택도가 높은 user_id를 앞에
CREATE INDEX idx_orders_user_status ON orders(user_id, status, created_at);

-- 이 인덱스로 다음 쿼리들이 모두 빨라집니다
SELECT * FROM orders WHERE user_id = 123;
SELECT * FROM orders WHERE user_id = 123 AND status = 'pending';
SELECT * FROM orders WHERE user_id = 123 AND status = 'pending' AND created_at > '2024-01-01';

-- 문제
SELECT * FROM orders WHERE status = 'pending' AND user_id = 123

-- 카디널리티 확인
SELECT
    COUNT(DISTINCT email) / COUNT(*) as email_selectivity,
    COUNT(DISTINCT status) / COUNT(*) as status_selectivity
FROM users;

-- email(높은 카디널리티) > status(낮은 카디널리티)
CREATE INDEX idx_users_email_status ON users(email, status);

-- 접두사 인덱스 생성 (20자가 적절하다면)
CREATE INDEX idx_articles_title_prefix ON articles(title(20));

WHERE symbol LIKE 'AAPL%'

WHERE symbol LIKE '%APL'