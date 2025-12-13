CREATE TABLE posts (
       post_id BIGINT AUTO_INCREMENT PRIMARY KEY,
       user_id BIGINT NOT NULL,
       title VARCHAR(200) NOT NULL,
       content TEXT NOT NULL,

       -- 성능을 위한 비정규화 필드들
       comment_count INT DEFAULT 0,
       like_count INT DEFAULT 0,
       view_count INT DEFAULT 0,

       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

       INDEX idx_user_id (user_id),
       INDEX idx_created_at (created_at)
);

CREATE TABLE comments (
      comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
      post_id BIGINT NOT NULL,
      user_id BIGINT NOT NULL,
      content TEXT NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      is_deleted BOOLEAN DEFAULT FALSE,

      INDEX idx_post_id (post_id),
      INDEX idx_user_id (user_id),
      INDEX idx_post_created (post_id, created_at)
);

INSERT INTO comments (post_id, user_id, content) VALUES (123, 456, 'Great post!');
UPDATE posts SET comment_count = comment_count + 1 WHERE post_id = 123;

-- 또는 트리거로 자동화
DELIMITER //
CREATE TRIGGER tr_comment_count_insert
    AFTER INSERT ON comments
    FOR EACH ROW
BEGIN
    UPDATE posts
    SET comment_count = comment_count + 1
    WHERE post_id = NEW.post_id;
END //
DELIMITER ;

-- -----------------------------------------------------



-- 사용자 활동 요약 테이블 (배치로 갱신)
CREATE TABLE user_activity_summary (
   summary_date DATE NOT NULL,
   user_id BIGINT NOT NULL,

    -- 일별 활동 통계
   login_count INT DEFAULT 0,
   post_count INT DEFAULT 0,
   comment_count INT DEFAULT 0,
   like_given_count INT DEFAULT 0,
   like_received_count INT DEFAULT 0,

    -- 시간 관련
   first_activity_at TIMESTAMP NULL,
   last_activity_at TIMESTAMP NULL,
   total_active_minutes INT DEFAULT 0,

   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

   PRIMARY KEY (summary_date, user_id),
   INDEX idx_user_id (user_id),
   INDEX idx_summary_date (summary_date)
);







-- 실시간 요약 갱신 프로시저
DELIMITER //
CREATE PROCEDURE sp_update_user_activity_summary(
    IN p_user_id BIGINT,
    IN p_activity_type VARCHAR(50)
)
BEGIN
    INSERT INTO user_activity_summary (summary_date, user_id)
    VALUES (CURDATE(), p_user_id)
    ON DUPLICATE KEY UPDATE
     login_count = CASE WHEN p_activity_type = 'LOGIN' THEN login_count + 1 ELSE login_count END,
     post_count = CASE WHEN p_activity_type = 'POST' THEN post_count + 1 ELSE post_count END,
     comment_count = CASE WHEN p_activity_type = 'COMMENT' THEN comment_count + 1 ELSE comment_count END,
     last_activity_at = NOW(),
     updated_at = NOW();
END //
DELIMITER ;

CALL sp_update_user_activity_summary(123, 'LOGIN');

CREATE TABLE financial_master_class.stock_trades (
         trade_id BIGINT NOT NULL AUTO_INCREMENT,
         trade_date DATE NOT NULL,
         PRIMARY KEY (trade_id, trade_date)
);

DELIMITER //

CREATE PROCEDURE sp_manage_partitions()
BEGIN
    DECLARE current_partition_key INT;
    DECLARE next_partition_key INT;
    DECLARE partition_name VARCHAR(20);
    DECLARE sql_stmt TEXT;

    -- 현재 월 기준 파티션 키 계산
    SET current_partition_key = YEAR(NOW()) * 100 + MONTH(NOW());
    SET next_partition_key =
            CASE
                WHEN MONTH(NOW()) = 12 THEN (YEAR(NOW()) + 1) * 100 + 1
                ELSE current_partition_key + 1
                END;

    -- 다음 달 파티션 이름
    SET partition_name = CONCAT('p', next_partition_key);

    -- 파티션 존재 여부 확인
    SELECT COUNT(*) INTO @partition_exists
    FROM information_schema.partitions
    WHERE table_schema = 'financial_master_class'
      AND table_name = 'stock_trades'
      AND partition_name = partition_name;

    -- 파티션이 없으면 생성
    IF @partition_exists = 0 THEN
        SET sql_stmt = CONCAT(
                'ALTER TABLE stock_trades ',
                'REORGANIZE PARTITION p_future INTO (',
                'PARTITION ', partition_name, ' VALUES LESS THAN (', next_partition_key + 1, '),',
                'PARTITION p_future VALUES LESS THAN MAXVALUE',
                ')'
                       );

        SET @sql = sql_stmt;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        SELECT CONCAT('Created partition: ', partition_name) as result;
    ELSE
        SELECT CONCAT('Partition already exists: ', partition_name) as result;
    END IF;

END //

DELIMITER ;

-- 매월 1일 자동 실행을 위한 이벤트 생성
CREATE EVENT ev_monthly_partition_maintenance
    ON SCHEDULE EVERY 1 MONTH
        STARTS '2024-01-01 00:00:00'
    DO
    CALL sp_manage_partitions();