SELECT
    user_id,
    username,
    email,
    display_name,
    profile_image_url,
    status,
    created_at,
    last_login_at
FROM users
WHERE user_id = 12345 AND status = 'ACTIVE';

SELECT
    post_id,
    user_id,
    category_id,
    title,
    content,
    view_count,
    like_count,
    comment_count,
    is_featured,
    created_at
FROM posts
WHERE status = 'PUBLISHED'
ORDER BY created_at DESC
LIMIT 10;


-- 1. 전문 검색 인덱스 활용 (가장 효율적)
SELECT post_id, title, content
FROM posts
WHERE MATCH(title, content) AGAINST('MySQL 최적화' IN NATURAL LANGUAGE MODE)
  AND status = 'PUBLISHED';

-- 2. 접두사 검색 (인덱스 활용 가능)
SELECT user_id, username
FROM users
WHERE username LIKE 'john%' AND status = 'ACTIVE';

-- 3. 복합 검색 조건
SELECT post_id, title
FROM posts
WHERE status = 'PUBLISHED'
  AND (
    title LIKE '%데이터베이스%'
        OR content LIKE '%성능%'
    )
ORDER BY created_at DESC;

-- 기본 문법
SELECT 테이블1.컬럼, 테이블2.컬럼
FROM 테이블1
         INNER JOIN 테이블2 ON 테이블1.공통컬럼 = 테이블2.공통컬럼;


SELECT
    p.post_id,
    p.title,
    u.username,
    u.display_name
FROM posts p
         INNER JOIN users u ON p.user_id = u.user_id
WHERE p.status = 'PUBLISHED';

SELECT
    u.username,
    u.display_name,
    COUNT(p.post_id) as post_count
FROM users u
LEFT JOIN posts p ON u.user_id = p.user_id
    AND p.status = 'PUBLISHED'
WHERE u.status = 'ACTIVE'
GROUP BY u.user_id, u.username, u.display_name;

SELECT
    p.post_id,
    p.title,
    COALESCE(u.username, '[삭제된 사용자]') as author_name
FROM users u
RIGHT JOIN posts p ON u.user_id = p.user_id
WHERE p.status = 'PUBLISHED';

SELECT
    p.post_id,
    p.title,
    COALESCE(u.username, '[삭제된 사용자]') as author_name
FROM posts p
         LEFT JOIN users u ON p.user_id = u.user_id
WHERE p.status = 'PUBLISHED';