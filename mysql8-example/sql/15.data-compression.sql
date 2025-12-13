-- 1. 데이터 저장에 대한 비용
-- 2. 성능 저하

-- 압축 기능을 활성화하여 테이블 생성
CREATE TABLE logs_compressed (
     log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
     user_id INT,
     message TEXT,
     created_at TIMESTAMP
)
    ROW_FORMAT=COMPRESSED
    KEY_BLOCK_SIZE=8; -- 페이지 압축 후 목표 크기 (KB). 4, 8, 16 등.

-- 데이터 아카이빙
1. 데이터를 어디로 어떻게 옮길것인가
2. 옮긴데이터를 어떻게 접근할것인가