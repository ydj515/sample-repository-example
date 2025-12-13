-- mysql은 계층이 여러개임.
-- 1. 커넥션 핸들러 (Connection Handler)
-- 2. MySQL 엔진 (MySQL Engine)
-- 3. 스토리지 엔진 (Storage Engine)
-- 4. 파일 시스템 (File System)

-- 현재 누가 접속해 있는지 확인하기
SELECT
    ID as 프로세스번호,
    USER as 사용자,
    HOST as 접속위치,
    DB as 사용중인DB,
    COMMAND as 현재상태,
    TIME as 실행시간초,
    LEFT(INFO, 50) as 실행중쿼리
FROM information_schema.PROCESSLIST
WHERE COMMAND != 'Sleep'  -- 잠들어있지 않은 것만
ORDER BY TIME DESC;       -- 실행시간이 긴 순서로


-- MySQL 연결 설정 확인하기
SELECT
    '최대 연결 수' as 설정,
    @@max_connections as 현재값,
    '동시에 접속할 수 있는 최대 사용자 수' as 설명
UNION ALL
SELECT
    '현재 연결 수',
    (SELECT VARIABLE_VALUE FROM performance_schema.global_status
     WHERE VARIABLE_NAME = 'Threads_connected'),
    '지금 접속해 있는 사용자 수'
UNION ALL
SELECT
    '활성 연결 수',
    (SELECT VARIABLE_VALUE FROM performance_schema.global_status
     WHERE VARIABLE_NAME = 'Threads_running'),
    '실제로 뭔가 하고 있는 사용자 수';



-- 어떤 스토리지 엔진을 사용하고 있는지 확인
SELECT
    TABLE_SCHEMA as 데이터베이스,
    TABLE_NAME as 테이블명,
    ENGINE as 스토리지엔진,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) as 크기_MB
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;


-- MyISAM

-- 버퍼 풀 기본 정보 확인하기
SELECT
    POOL_SIZE as 전체페이지수,
    ROUND(POOL_SIZE * 16384 / 1024 / 1024, 0) as 전체크기_MB,
    FREE_BUFFERS as 빈페이지수,
    DATABASE_PAGES as 데이터페이지수,
    MODIFIED_DATABASE_PAGES as 수정된페이지수,
    ROUND(HIT_RATE, 2) as 히트율_퍼센트
FROM information_schema.INNODB_BUFFER_POOL_STATS;

-- 어떤 종류의 페이지들이 메모리에 있는지 확인
SELECT
    PAGE_TYPE as 페이지종류,
    COUNT(*) as 개수,
    ROUND(COUNT(*) * 16 / 1024, 1) as 크기_MB,
    ROUND(COUNT(*) / (SELECT COUNT(*) FROM information_schema.INNODB_BUFFER_PAGE) * 100, 1) as 비율_퍼센트
FROM information_schema.INNODB_BUFFER_PAGE
GROUP BY PAGE_TYPE
ORDER BY 개수 DESC
LIMIT 10;

-- 로그 버퍼 : WAL : Write-Ahead Logging
-- flush : 로그 버퍼 데이터를 디스크에 저장

-- 로그 버퍼 상태 확인
SELECT
    '로그 버퍼 크기(MB)' as 항목,
    @@innodb_log_buffer_size / 1024 / 1024 as 값
UNION ALL
SELECT
    '로그 대기 횟수',
    (SELECT VARIABLE_VALUE FROM performance_schema.global_status
     WHERE VARIABLE_NAME = 'Innodb_log_waits')
UNION ALL
SELECT
    '로그 쓰기 요청',
    (SELECT VARIABLE_VALUE FROM performance_schema.global_status
     WHERE VARIABLE_NAME = 'Innodb_log_write_requests')
UNION ALL
SELECT
    '실제 로그 쓰기',
    (SELECT VARIABLE_VALUE FROM performance_schema.global_status
     WHERE VARIABLE_NAME = 'Innodb_log_writes');