SELECT
    post_id,
    title,
    view_count,
    created_at
FROM posts
WHERE status = 'PUBLISHED'
ORDER BY created_at DESC
LIMIT 10 OFFSET 0;

SELECT
    post_id,
    title,
    view_count,
    created_at
FROM posts
WHERE status = 'PUBLISHED'
ORDER BY created_at DESC
LIMIT 10 OFFSET 10;


SELECT post_id, title, created_at
FROM posts
WHERE status = 'PUBLISHED'
ORDER BY created_at DESC
LIMIT 10 OFFSET 10000;


-- 커서 기반의 페이징 첫번쨰 페이지
SELECT post_id, title, created_at
FROM posts
WHERE status = 'PUBLISHED'
ORDER BY created_at DESC, post_id DESC
LIMIT 10;

-- 커서 기반의 페이징 두번쨰 페이지
SELECT post_id, title, created_at
FROM posts
WHERE status = 'PUBLISHED' AND post_id < 12345
ORDER BY created_at DESC, post_id DESC
LIMIT 10;


SELECT
    기본컬럼들,
    윈도우함수() OVER (PARTITION BY 그룹컬럼 ORDER BY 정렬컬럼) as 결과컬럼
FROM 테이블;


SELECT
    post_id,
    title,
    view_count,

    -- 순차적인 순위 (1,2,3,4...)
    ROW_NUMBER() OVER (ORDER BY view_count DESC) as 전체순위,

    -- 동점자 고려한 순위 (1,2,2,4...)
    RANK() OVER (ORDER BY view_count DESC) as 랭킹

FROM posts
WHERE status = 'PUBLISHED'
ORDER BY view_count DESC;

SELECT
    user_id,
    post_id,
    title,
    view_count,

    -- 각 사용자 안에서의 순위
    ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY view_count DESC) as 사용자내순위

FROM posts
WHERE status = 'PUBLISHED'
ORDER BY user_id, 사용자내순위;


SELECT
    post_id,
    title,
    created_at,
    view_count,

    -- 이전 게시글의 조회수
    LAG(view_count, 1) OVER (ORDER BY created_at) as 이전글조회수,

    -- 다음 게시글의 조회수
    LEAD(view_count, 1) OVER (ORDER BY created_at) as 다음글조회수

FROM posts
WHERE status = 'PUBLISHED'
ORDER BY created_at;

WITH 임시테이블명 AS (
    SELECT ... -- 1단계 계산
    )
SELECT ...     -- 2단계에서 1단계 결과 사용
    FROM 임시테이블명;

WITH post_summary AS (
    -- 1단계: 사용자별 게시글 통계 계산
    SELECT
        user_id,
        COUNT(*) as 게시글수,
        SUM(view_count) as 총조회수,
        AVG(view_count) as 평균조회수
    FROM posts
    WHERE status = 'PUBLISHED'
    GROUP BY user_id
)
-- 2단계: 사용자 정보와 결합해서 최종 결과
SELECT
    u.username,
    ps.게시글수,
    ps.총조회수,
    ps.평균조회수
FROM post_summary ps
         INNER JOIN users u ON ps.user_id = u.user_id
ORDER BY ps.총조회수 DESC;

SELECT
    u.username,
    COUNT(*) as 게시글수,
    SUM(p.view_count) as 총조회수,
    AVG(p.view_count) as 평균조회수
FROM posts p
         INNER JOIN users u ON p.user_id = u.user_id
WHERE p.status = 'PUBLISHED'
GROUP BY u.user_id, u.username
ORDER BY SUM(p.view_count) DESC;




WITH 활성사용자 AS (
    -- 1단계: 활성 사용자만 추출
    SELECT user_id, username
    FROM users
    WHERE status = 'ACTIVE'
),
     최근게시글 AS (
         -- 2단계: 최근 30일 게시글 통계
         SELECT user_id, COUNT(*) as 최근글수
         FROM posts
         WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
           AND status = 'PUBLISHED'
         GROUP BY user_id
     )
-- 3단계: 두 결과를 조합
SELECT
    a.username,
    COALESCE(r.최근글수, 0) as 최근30일글수
FROM 활성사용자 a
         LEFT JOIN 최근게시글 r ON a.user_id = r.user_id
ORDER BY 최근30일글수 DESC;

SELECT
    u.username,
    COALESCE(r.최근글수, 0) as 최근30일글수
FROM users u
         LEFT JOIN (
    SELECT user_id, COUNT(*) as 최근글수
    FROM posts
    WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
      AND status = 'PUBLISHED'
    GROUP BY user_id
) r ON u.user_id = r.user_id
WHERE u.status = 'ACTIVE'
ORDER BY 최근30일글수 DESC;


SELECT
    user_id,
    username
FROM users u
WHERE status = 'ACTIVE'
  AND EXISTS (
    SELECT 1  -- 실제 값은 중요하지 않음, 존재만 확인
    FROM posts p
    WHERE p.user_id = u.user_id
      AND p.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
);

SELECT username
FROM users u
WHERE EXISTS (
    SELECT 1 FROM posts p WHERE p.user_id = u.user_id
);

SELECT username
FROM users u
WHERE u.user_id IN (
    SELECT user_id FROM posts
);