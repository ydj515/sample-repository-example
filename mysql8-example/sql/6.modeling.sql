-- 1. 개념적인 데이터 모델링
-- 2. 논리적인 데이터 모델링
-- 3. 물리적 데이터 모델링

-- 1. 슈퍼 키 (user_id, email)
-- 2. 후보 키 (user_id, email)  [user_id, username] X
-- 3. 기본 키
-- 4. 대체 키
-- 5. 외래 키, 복합 키

-- 유저 테이블
CREATE TABLE Users (
   user_id INT AUTO_INCREMENT PRIMARY KEY,
   username VARCHAR(50) NOT NULL,
   email VARCHAR(100) NOT NULL,
   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
   UNIQUE KEY uq_username (username), -- 유저명은 유일해야 함 (대체키)
   UNIQUE KEY uq_email (email)         -- 이메일은 유일해야 함 (대체키)
);

-- 태그 테이블
CREATE TABLE Tags (
  tag_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  UNIQUE KEY uq_name (name)
);


-- 게시글 테이블
CREATE TABLE Posts (
       post_id INT AUTO_INCREMENT PRIMARY KEY,
       user_id INT NOT NULL,
       title VARCHAR(200) NOT NULL,
       content TEXT NOT NULL,
       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
       KEY idx_user_id (user_id), -- 작성자별 게시글 조회를 위해 인덱스 추가
       FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

-- 댓글 테이블
CREATE TABLE Comments (
      comment_id INT AUTO_INCREMENT PRIMARY KEY,
      post_id INT NOT NULL,
      user_id INT NOT NULL,
      content TEXT NOT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      KEY idx_post_id (post_id), -- 특정 게시글의 댓글 목록 조회를 위해 인덱스 추가
      KEY idx_user_id (user_id),
      FOREIGN KEY (post_id) REFERENCES Posts(post_id),
      FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

-- 게시글-태그 연결 테이블 (N:M 관계 해소)
CREATE TABLE PostTags (
      post_id INT NOT NULL,
      tag_id INT NOT NULL,
      PRIMARY KEY (post_id, tag_id), -- 복합키(Composite Key) 사용
      FOREIGN KEY (post_id) REFERENCES Posts(post_id),
      FOREIGN KEY (tag_id) REFERENCES Tags(tag_id)
);

-- 1. 1NF
-- 2. 2NF (order_id, product_id) PK
-- 3. 3NF