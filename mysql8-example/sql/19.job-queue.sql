-- Anti-pattern: Synchronous Processing
public void signUp(User user) {
    -- 1. 사용자 정보를 DB에 저장 (빠름)
    userRepository.save(user);

-- 2. 환영 이메일 발송 (느릴 수 있음)
    emailService.sendWelcomeEmail(user.getEmail());

-- 3. 신규 가입 쿠폰 발급 (느릴 수 있음)
    couponService.issueSignUpCoupon(user.getId());

-- 4. 모든 작업이 끝나야 사용자에게 응답이 감
}

CREATE TABLE `jobs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `job_type` VARCHAR(50) NOT NULL COMMENT '작업 종류 (e.g., SEND_EMAIL, ISSUE_COUPON)',
    `payload` JSON NOT NULL COMMENT '작업에 필요한 데이터',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '작업 상태 (PENDING, RUNNING, DONE, FAILED)',
    `priority` INT NOT NULL DEFAULT 100 COMMENT '우선순위 (낮을수록 높음)',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    `last_error_message` TEXT NULL COMMENT '마지막 에러 메시지',
    `run_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '실행될 시간 (지연 작업용)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_status_priority_runat` (`status`, `priority`, `run_at`) COMMENT '워커가 작업을 가져가기 위한 복합 인덱스'
);

BEGIN; -- 트랜잭션 시작

-- 1. 핵심 비즈니스 로직
INSERT INTO users (name, email) VALUES ('John Doe', 'john.doe@example.com');
SET @user_id = LAST_INSERT_ID();

-- 2. 비동기 작업 등록
INSERT INTO jobs (job_type, payload, priority)
VALUES ('SEND_WELCOME_EMAIL', JSON_OBJECT('user_id', @user_id), 100);

INSERT INTO jobs (job_type, payload, priority)
VALUES ('ISSUE_SIGNUP_COUPON', JSON_OBJECT('user_id', @user_id), 200);

COMMIT; -- 트랜잭션 종료

--

BEGIN; -- 트랜잭션 시작

-- 1. 처리할 작업을 찾고, 해당 행에 배타적 락(Exclusive Lock)을 건다.
--    다른 워커는 이 행에 접근할 수 없게 된다.
SET @job_id = (
    SELECT id FROM jobs
    WHERE status = 'PENDING' AND run_at <= NOW()
    ORDER BY priority ASC, id ASC
    LIMIT 1
    FOR UPDATE SKIP LOCKED -- MySQL 8.0+ : 락이 걸린 행은 건너뛰고 다음 행을 찾음
);

-- 2. 만약 처리할 작업이 있다면 (job_id가 NULL이 아니라면)
IF @job_id IS NOT NULL THEN
  -- 상태를 'RUNNING'으로 변경하여 다른 워커가 가져가지 못하도록 명시한다.
UPDATE jobs SET status = 'RUNNING' WHERE id = @job_id;
END IF;

COMMIT; -- 트랜잭션 종료. 락이 해제된다.


-- 실패 처리 로직 (의사 코드)
SET @new_retry_count = retry_count + 1;
SET @delay = POWER(10, @new_retry_count); -- 10, 100, 1000... 초 지연 (예시)


-- 먼저 jobs 테이블에 작업 상태가 마지막으로 변경된 시간을 기록할 컬럼이 필요
-- 이제 특정 시간(예: 5분) 이상 running 상태에 머물러 있는 작업을 찾아 pending으로 바꾸는 쿼리를 작성
UPDATE jobs
SET
    status = 'PENDING', -- 재시도를 위해 PENDING으로 변경
    retry_count = @new_retry_count,
    last_error_message = '...',
    run_at = NOW() + INTERVAL @delay SECOND -- 다음 실행 시간을 뒤로 미룸
WHERE id = @job_id;