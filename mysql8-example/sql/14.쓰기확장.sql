-- 파티셔닝
-- > 거대한 파일을 논리적인 기준을 잡고 나눠서 저장하는 기법

-- 1. RANGE : 날짜, 연도, 월, 번호 연속적인 값을 기준을 나눌 떄 효과적이다.
-- 2. LIST : 국가코드, 카테고리, 이런 정해진 값을 목록에 따라 나눌떄
-- 3. Hash : 특정 기준 없이 균등하게 저장할 떄

CREATE TABLE access_logs (
     log_id BIGINT AUTO_INCREMENT,
     user_id INT NOT NULL,
     access_time DATETIME NOT NULL,
     message VARCHAR(255),
     PRIMARY KEY (log_id, access_time) -- 파티션 키는 PK에 포함되어야 함
)
    PARTITION BY RANGE (TO_DAYS(access_time)) (
        PARTITION p202401 VALUES LESS THAN (TO_DAYS('2024-02-01')),
        PARTITION p202402 VALUES LESS THAN (TO_DAYS('2024-03-01')),
        PARTITION p202403 VALUES LESS THAN (TO_DAYS('2024-04-01')),
        PARTITION p_future VALUES LESS THAN MAXVALUE
        );


-- 1. 샤드 키 선정
    -- > is_active [boolean], country_code

-- 2. Cross Shard

-- 3. 분산 트랜잭션
    -- > 2PC, 사가 패턴
