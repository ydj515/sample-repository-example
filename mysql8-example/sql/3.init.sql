-- 초기 단순한 사용자 테이블
CREATE TABLE users (
       id INT AUTO_INCREMENT PRIMARY KEY,
       username VARCHAR(50),
       email VARCHAR(100),
       password VARCHAR(255),
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- 서비스 성장 후 필요해진 확장된 사용자 테이블
CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,

    -- 프로필 정보
                       display_name VARCHAR(100),
                       bio TEXT,
                       profile_image_url VARCHAR(500),

    -- 상태 관리
                       status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') DEFAULT 'ACTIVE',
                       email_verified BOOLEAN DEFAULT FALSE,

    -- 로그인 관리
                       last_login_at TIMESTAMP NULL,
                       login_count INT DEFAULT 0,

    -- 시간 정보
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 인덱스
                       INDEX idx_username (username),
                       INDEX idx_email (email),
                       INDEX idx_status (status),
                       INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




-- 완전 정규화된 버전 (조인이 많이 필요)
CREATE TABLE orders (
        order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        user_id BIGINT NOT NULL,
        status ENUM('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED') NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_items (
         item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
         order_id BIGINT NOT NULL,
         product_id BIGINT NOT NULL,
         quantity INT NOT NULL,
         unit_price DECIMAL(10,2) NOT NULL,

         FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- 실무에서 자주 사용하는 비정규화된 버전 (성능 우선)
CREATE TABLE orders (
                        order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,

    -- 주문 정보 (변경 불가능한 스냅샷)
                        total_amount DECIMAL(12,2) NOT NULL,
                        item_count INT NOT NULL,

    -- 사용자 정보 복제 (배송용)
                        customer_name VARCHAR(100) NOT NULL,
                        customer_email VARCHAR(255) NOT NULL,
                        shipping_address TEXT NOT NULL,

    -- 상태 관리
                        status ENUM('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED') NOT NULL,

    -- 시간 정보
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 성능 최적화 인덱스
                        INDEX idx_user_status (user_id, status),
                        INDEX idx_created_at (created_at),
                        INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 잘못된 설계 (비정규화 과도)
CREATE TABLE user_posts_bad (
        post_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        user_id BIGINT NOT NULL,
        user_name VARCHAR(100) NOT NULL,      -- 중복!
        user_email VARCHAR(255) NOT NULL,     -- 중복!
        user_profile_image VARCHAR(500),      -- 중복!

        title VARCHAR(200) NOT NULL,
        content TEXT NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



-- 올바른 정규화된 설계
CREATE TABLE users (
       user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
       user_name VARCHAR(100) NOT NULL,
       email VARCHAR(255) NOT NULL UNIQUE,
       profile_image_url VARCHAR(500),
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

       INDEX idx_email (email)
);

CREATE TABLE posts (
       post_id BIGINT AUTO_INCREMENT PRIMARY KEY,
       user_id BIGINT NOT NULL,
       title VARCHAR(200) NOT NULL,
       content TEXT NOT NULL,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

       FOREIGN KEY (user_id) REFERENCES users(user_id),
       INDEX idx_user_created (user_id, created_at)
);


CREATE TABLE order_snapshots (
     order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
     user_id BIGINT NOT NULL,

    -- 주문 시점의 사용자 정보 (스냅샷)
     customer_name VARCHAR(100) NOT NULL,
     customer_email VARCHAR(255) NOT NULL,
     shipping_address TEXT NOT NULL,

    -- 주문 정보
     total_amount DECIMAL(12,2) NOT NULL,
     order_status ENUM('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED'),

    -- 주문 상품들 (JSON으로 저장)
     order_items JSON NOT NULL,

     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

     INDEX idx_user_id (user_id),
     INDEX idx_status (order_status),
     INDEX idx_created_at (created_at)
);