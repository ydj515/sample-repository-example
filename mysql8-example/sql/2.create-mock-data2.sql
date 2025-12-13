CREATE DATABASE IF NOT EXISTS crud_patterns;
USE crud_patterns;

-- 사용자 테이블
CREATE TABLE users (
                      user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      username VARCHAR(50) NOT NULL UNIQUE,
                      email VARCHAR(255) NOT NULL UNIQUE,
                      password_hash VARCHAR(255) NOT NULL,
                      display_name VARCHAR(100),
                      profile_image_url VARCHAR(500),
                      status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') DEFAULT 'ACTIVE',
                      email_verified BOOLEAN DEFAULT FALSE,
                      last_login_at TIMESTAMP NULL,
                      login_count INT DEFAULT 0,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                      INDEX idx_username (username),
                      INDEX idx_email (email),
                      INDEX idx_status (status),
                      INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 게시글 테이블 (비정규화 적용)
CREATE TABLE posts (
                      post_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      user_id BIGINT NOT NULL,
                      category_id INT NOT NULL,
                      title VARCHAR(200) NOT NULL,
                      content TEXT NOT NULL,

   -- 성능을 위한 비정규화 필드들
                      comment_count INT DEFAULT 0,
                      like_count INT DEFAULT 0,
                      view_count INT DEFAULT 0,

   -- 상태 관리
                      status ENUM('DRAFT', 'PUBLISHED', 'DELETED') DEFAULT 'DRAFT',
                      is_featured BOOLEAN DEFAULT FALSE,

                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                      FOREIGN KEY (user_id) REFERENCES users(user_id),

   -- 자주 사용되는 쿼리 패턴에 맞춘 인덱스
                      INDEX idx_user_status_created (user_id, status, created_at),
                      INDEX idx_category_status_featured (category_id, status, is_featured, created_at),
                      INDEX idx_status_created (status, created_at),
                      INDEX idx_featured_created (is_featured, created_at),

   -- 전문 검색 인덱스
                      FULLTEXT idx_search (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 댓글 테이블 (대댓글 지원)
CREATE TABLE comments (
                         comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         post_id BIGINT NOT NULL,
                         user_id BIGINT NOT NULL,
                         parent_comment_id BIGINT NULL,
                         content TEXT NOT NULL,
                         like_count INT DEFAULT 0,
                         status ENUM('APPROVED', 'PENDING', 'DELETED') DEFAULT 'APPROVED',
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                         FOREIGN KEY (post_id) REFERENCES posts(post_id),
                         FOREIGN KEY (user_id) REFERENCES users(user_id),
                         FOREIGN KEY (parent_comment_id) REFERENCES comments(comment_id),

                         INDEX idx_post_created (post_id, created_at),
                         INDEX idx_user_created (user_id, created_at),
                         INDEX idx_parent (parent_comment_id),
                         INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER //


-- 대용량 더미 데이터 생성 프로시저
CREATE PROCEDURE GenerateCrudData()
BEGIN
   DECLARE i INT DEFAULT 1;
   DECLARE max_users INT DEFAULT 20000;
   DECLARE max_posts INT DEFAULT 100000;
   DECLARE max_comments INT DEFAULT 300000;
   DECLARE max_likes INT DEFAULT 500000;

   -- 사용자 데이터 생성
   WHILE i <= max_users DO
           INSERT INTO users (username, email, password_hash, display_name, status, email_verified, login_count)
           VALUES (
                      CONCAT('user_', i),
                      CONCAT('user', i, '@example.com'),
                      SHA2(CONCAT('password', i), 256),
                      CONCAT('User Display ', i),
                      ELT(FLOOR(RAND() * 10) + 1, 'ACTIVE', 'ACTIVE', 'ACTIVE', 'ACTIVE', 'ACTIVE',
                          'ACTIVE', 'ACTIVE', 'ACTIVE', 'INACTIVE', 'SUSPENDED'),
                      RAND() > 0.2,
                      FLOOR(RAND() * 1000)
                  );

           SET i = i + 1;

           IF i % 1000 = 0 THEN
               COMMIT;
           END IF;
       END WHILE;


   -- 게시글 데이터 생성
   SET i = 1;
   WHILE i <= max_posts DO
           INSERT INTO posts (user_id, category_id, title, content, status, is_featured, view_count, created_at)
           VALUES (
                      FLOOR(RAND() * max_users) + 1,
                      FLOOR(RAND() * 10) + 1,
                      CONCAT('게시글 제목 ', i, ' - ', ELT(FLOOR(RAND() * 5) + 1, '중요', '공지', '질문', '정보', '후기')),
                      CONCAT('게시글 내용입니다. 이것은 ', i, '번째 게시글로 다양한 내용을 담고 있습니다. ',
                             'MySQL, 데이터베이스, 성능, 최적화, 인덱스, 쿼리 등의 키워드를 포함합니다. ',
                             '실무에서 유용한 정보를 제공하고 있으며, 많은 사용자들에게 도움이 될 것입니다.'),
                      ELT(FLOOR(RAND() * 10) + 1, 'PUBLISHED', 'PUBLISHED', 'PUBLISHED', 'PUBLISHED', 'PUBLISHED',
                          'PUBLISHED', 'PUBLISHED', 'PUBLISHED', 'DRAFT', 'DELETED'),
                      RAND() < 0.1,  -- 10% 확률로 featured
                      FLOOR(RAND() * 10000),
                      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY)
                  );

           SET i = i + 1;

           IF i % 1000 = 0 THEN
               COMMIT;
           END IF;
       END WHILE;

   -- 댓글 데이터 생성
   SET i = 1;
   WHILE i <= max_comments DO
           SET @post_id = FLOOR(RAND() * max_posts) + 1;
           SET @user_id = FLOOR(RAND() * max_users) + 1;
           SET @parent_id = NULL;

           -- 20% 확률로 대댓글 생성
           IF RAND() < 0.2 THEN
               SELECT comment_id INTO @parent_id
               FROM comments
               WHERE post_id = @post_id AND parent_comment_id IS NULL
               ORDER BY RAND()
               LIMIT 1;
           END IF;

           INSERT INTO comments (post_id, user_id, parent_comment_id, content, like_count, created_at)
           VALUES (
                      @post_id,
                      @user_id,
                      @parent_id,
                      CONCAT('댓글 내용입니다. ', i, '번째 댓글로 유익한 정보를 제공합니다.'),
                      FLOOR(RAND() * 50),
                      DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 300) DAY)
                  );

           SET i = i + 1;

           IF i % 1000 = 0 THEN
               COMMIT;
           END IF;
       END WHILE;
END //

DELIMITER ;

-- 더미 데이터 생성 실행
CALL GenerateCrudData();

-- 프로시저 정리
DROP PROCEDURE GenerateCrudData;