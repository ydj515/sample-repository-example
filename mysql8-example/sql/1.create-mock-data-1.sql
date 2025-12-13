CREATE DATABASE IF NOT EXISTS mysql_fundamentals;
USE mysql_fundamentals;

-- 사용자 테이블
CREATE TABLE users (
                      user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      email VARCHAR(255) NOT NULL UNIQUE,
                      password_hash VARCHAR(255) NOT NULL,
                      name VARCHAR(100) NOT NULL,
                      phone VARCHAR(20),
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      is_active BOOLEAN DEFAULT TRUE,

                      INDEX idx_email (email),
                      INDEX idx_created_at (created_at),
                      INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 계좌 테이블
CREATE TABLE accounts (
                         account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         user_id BIGINT NOT NULL,
                         account_number VARCHAR(20) NOT NULL UNIQUE,
                         account_type ENUM('CHECKING', 'SAVINGS', 'INVESTMENT') NOT NULL,
                         balance DECIMAL(15,2) DEFAULT 0.00,
                         currency VARCHAR(3) DEFAULT 'KRW',
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         is_active BOOLEAN DEFAULT TRUE,

                         FOREIGN KEY (user_id) REFERENCES users(user_id),
                         INDEX idx_user_id (user_id),
                         INDEX idx_account_number (account_number),
                         INDEX idx_account_type (account_type),
                         INDEX idx_balance (balance)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 대용량 더미 데이터 생성
DELIMITER //

CREATE PROCEDURE GenerateUsers()
BEGIN
   DECLARE i INT DEFAULT 1;
   DECLARE max_users INT DEFAULT 10000;

   WHILE i <= max_users DO
           INSERT INTO users (email, password_hash, name, phone, is_active)
           VALUES
               (
                   CONCAT('user', i, '@example.com'),
                   SHA2(CONCAT('password', i), 256),
                   CONCAT('User ', i),
                   CONCAT('010-', LPAD(FLOOR(RAND() * 10000), 4, '0'), '-', LPAD(FLOOR(RAND() * 10000), 4, '0')),
                   CASE WHEN RAND() > 0.1 THEN TRUE ELSE FALSE END
               );

           SET i = i + 1;

           -- 매 1000건마다 커밋
           IF i % 1000 = 0 THEN
               COMMIT;
           END IF;
       END WHILE;
END //

CREATE PROCEDURE GenerateAccounts()
BEGIN
   DECLARE i INT DEFAULT 1;
   DECLARE max_accounts INT DEFAULT 25000;
   DECLARE user_count INT;

   SELECT COUNT(*) INTO user_count FROM users;

   WHILE i <= max_accounts DO
           INSERT INTO accounts (user_id, account_number, account_type, balance)
           VALUES
               (
                   FLOOR(RAND() * user_count) + 1,
                   CONCAT('ACC', LPAD(i, 10, '0')),
                   ELT(FLOOR(RAND() * 3) + 1, 'CHECKING', 'SAVINGS', 'INVESTMENT'),
                   ROUND(RAND() * 1000000, 2)
               );

           SET i = i + 1;

           IF i % 1000 = 0 THEN
               COMMIT;
           END IF;
       END WHILE;
END //

DELIMITER ;

-- 더미 데이터 생성 실행
CALL GenerateUsers();

-- 프로시저 정리
DROP PROCEDURE GenerateUsers;

-- check
SELECT
   (SELECT COUNT(*) FROM users) as users_count,
   (SELECT COUNT(*) FROM accounts) as accounts_count
