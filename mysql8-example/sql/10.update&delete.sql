UPDATE users
SET last_login_at = CURRENT_TIMESTAMP
WHERE user_id = 1;

UPDATE users
SET
    display_name = '새로운 표시명',
    profile_image_url = 'https://example.com/new-image.jpg',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 123 AND status = 'ACTIVE';


UPDATE posts
SET like_count = CASE
        WHEN like_count > 0 THEN like_count - 1
        ELSE like_count + 1
    END
WHERE post_id = 1;

UPDATE posts
SET status = CASE
         WHEN view_count < 10 THEN 'DRAFT'        -- 조회수 낮으면 임시저장
         WHEN view_count >= 1000 THEN 'FEATURED'  -- 조회수 높으면 추천글
         ELSE 'PUBLISHED'                         -- 나머지는 일반 발행
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);

UPDATE posts p
    INNER JOIN users u ON p.user_id = u.user_id
SET p.author_name = u.display_name
WHERE u.updated_at > p.updated_at;

UPDATE users
SET status = 'INACTIVE'
    WHERE last_login_at < DATE_SUB(NOW(), INTERVAL 90 DAY)
    AND status = 'ACTIVE'
LIMIT 1000;


DELIMITER //

CREATE PROCEDURE BatchUpdateInactiveUsers()
BEGIN
    DECLARE affected_rows INT DEFAULT 1;

    WHILE affected_rows > 0 DO
            UPDATE users
            SET status = 'INACTIVE',
                updated_at = CURRENT_TIMESTAMP
            WHERE last_login_at < DATE_SUB(NOW(), INTERVAL 90 DAY)
              AND status = 'ACTIVE'
            LIMIT 1000;

            SET affected_rows = ROW_COUNT();  -- 실제 수정된 행 수

            SELECT SLEEP(0.1);  -- 0.1초 대기 (시스템 부하 방지)
        END WHILE;
END //

DELIMITER ;

CALL BatchUpdateInactiveUsers();


DELIMITER $$

CREATE PROCEDURE safe_deduct_points()
BEGIN
    DECLARE current_points INT DEFAULT 0;

    -- 트랜잭션 시작
    START TRANSACTION;

    -- 1. 현재 포인트 확인 (락 걸기)
    SELECT COALESCE(points, 0) INTO current_points
    FROM users
    WHERE user_id = 1 FOR UPDATE;

    -- 2. 포인트가 충분한지 검증
    IF current_points >= 100 THEN
        -- 3. 포인트 차감
        UPDATE users
        SET points = points - 100,
            updated_at = CURRENT_TIMESTAMP
        WHERE user_id = 1;

        -- 4. 사용 내역 기록
        INSERT INTO point_history (user_id, amount, type, description)
        VALUES (1, -100, 'USE', '상품 구매');

        COMMIT;  -- 성공
    ELSE
        ROLLBACK;  -- 포인트 부족으로 실패
    END IF;
END$$

DELIMITER ;


-- ---------------------------------------------------------------------------------------------------

DELETE FROM comments
WHERE comment_id = 456
  AND user_id = 123;

UPDATE posts
SET status = 'DELETED',
    updated_at = CURRENT_TIMESTAMP
WHERE post_id = 1 AND user_id = 1;

DELIMITER //

CREATE PROCEDURE BatchDeleteOldData()
BEGIN
    DECLARE affected_rows INT DEFAULT 1;

    WHILE affected_rows > 0 DO
            -- 30일 이상 삭제된 댓글 실제 삭제
            DELETE FROM comments
            WHERE status = 'DELETED'
              AND updated_at < DATE_SUB(NOW(), INTERVAL 30 DAY)
            LIMIT 1000;

            SET affected_rows = ROW_COUNT();

            -- 진행상황 출력
            SELECT CONCAT('Deleted ', affected_rows, ' comments') as progress;

            SELECT SLEEP(0.5);  -- 0.5초 대기
        END WHILE;
END //

DELIMITER ;

CALL BatchDeleteOldData();


ALTER TABLE logs DROP PARTITION p_202301;