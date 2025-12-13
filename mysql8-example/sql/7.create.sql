-- INSERT INTO

use crud_patterns;

-- 단일 사용자 등록
INSERT INTO users (username, email, password_hash, display_name)
VALUES ('john_doe', 'john@example.com', SHA2('password123', 256), 'John Doe');

-- 게시글 작성
INSERT INTO posts (user_id, category_id, title, content, status)
VALUES (1, 1, '제목', '내용', 'PUBLISHED');

-- 댓글 작성
INSERT INTO comments (post_id, user_id, content)
VALUES (1, 1, '댓글 내용');



INSERT INTO posts (user_id, category_id, title, content, status) VALUES
     (1, 1, '제목1', '내용1', 'PUBLISHED'),
     (1, 2, '제목2', '내용2', 'PUBLISHED'),
     (2, 1, '제목3', '내용3', 'DRAFT');


-- Prepared Statement로 반복 INSERT 최적화
PREPARE stmt_insert_comment FROM
    'INSERT INTO comments (post_id, user_id, content) VALUES (?, ?, ?)';

-- 변수 설정 후 실행 (루프에서 반복)
SET @post_id = 1;
SET @user_id = 1;
SET @content = '첫 번째 댓글';

EXECUTE stmt_insert_comment USING @post_id, @user_id, @content;

-- 다른 데이터로 재실행
SET @post_id = 1;
SET @user_id = 2;
SET @content = '두 번째 댓글';
SET @status = 'APPROVED';

EXECUTE stmt_insert_comment USING @post_id, @user_id, @content;

-- 정리
DEALLOCATE PREPARE stmt_insert_comment;



-- CSV 파일에서 직접 로드 (가장 빠른 방법)
LOAD DATA INFILE '/tmp/users_data.csv'
    INTO TABLE users
    FIELDS TERMINATED BY ','
    LINES TERMINATED BY '\n'
    IGNORE 1 ROWS  -- 헤더 스킵
    (username, email, display_name, @password)
    SET password_hash = SHA2(@password, 256),
        status = 'ACTIVE',
        created_at = NOW();


-- 중복 시 에러 없이 건너뛰기
INSERT IGNORE INTO users (username, email, password_hash, display_name)
VALUES
    ('john_doe', 'john@example.com', SHA2('pass1', 256), 'John'),
    ('jane_doe', 'jane@example.com', SHA2('pass2', 256), 'Jane'),
    ('john_doe', 'john2@example.com', SHA2('pass3', 256), 'John2');


-- -> SELECT -> UPDATE or INSERT

-- 중복 시 특정 컬럼만 업데이트
INSERT INTO users (username, email, password_hash, display_name, login_count)
VALUES ('john_doe', 'john@example.com', SHA2('pass', 256), 'John Doe', 1)
ON DUPLICATE KEY UPDATE
         display_name = VALUES(display_name),
         login_count = login_count + 1,  -- 기존값 + 1
         updated_at = CURRENT_TIMESTAMP;

-- 복잡한 업데이트 로직
INSERT INTO posts (user_id, category_id, title, content, view_count)
VALUES (1, 1, '제목', '내용', 1)
ON DUPLICATE KEY UPDATE
         content = CONCAT(content, '\n--- 업데이트됨 ---\n', VALUES(content)),
         view_count = view_count + VALUES(view_count),
         updated_at = CURRENT_TIMESTAMP;

-- 중복 시 기존 데이터 삭제 후 새 데이터 삽입
REPLACE INTO users (user_id, username, email, password_hash, display_name)
VALUES (1, 'john_updated', 'john_new@example.com', SHA2('newpass', 256), 'John Updated');


-- EXISTS를 이용한 조건부 삽입
INSERT INTO comments (post_id, user_id, content)
SELECT 1, 2, '댓글 내용'
WHERE EXISTS (
    SELECT 1 FROM posts
    WHERE post_id = 1 AND status = 'PUBLISHED'
)
  AND NOT EXISTS (
    SELECT 1 FROM comments
    WHERE post_id = 1 AND user_id = 2
      AND created_at > DATE_SUB(NOW(), INTERVAL 1 MINUTE)
);

-- 다른 테이블에서 데이터 복사
INSERT INTO posts (user_id, category_id, title, content, status)
SELECT
    user_id,
    1 as category_id,
    CONCAT('임시 제목 - ', user_id) as title,
    '자동 생성된 내용' as content,
    'DRAFT' as status
FROM users
WHERE status = 'ACTIVE'
  AND login_count > 10;

-- @Transactional

-- 다중 테이블 INSERT
START TRANSACTION;

-- 1. 게시글 작성
INSERT INTO posts (user_id, category_id, title, content, status)
VALUES (1, 1, '새 게시글', '내용', 'PUBLISHED');

SET @new_post_id = LAST_INSERT_ID();

-- 2. 첫 댓글 자동 생성
INSERT INTO comments (post_id, user_id, content, status)
VALUES (@new_post_id, 1, '게시글을 작성했습니다.', 'APPROVED');

COMMIT;