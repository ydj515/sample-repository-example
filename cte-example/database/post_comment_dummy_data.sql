-- 하나의 post에 댓글 15개 정도 생성
-- 설정값 (이 부분만 수정하면 전체 동작 변경)
SET @TOTAL_COUNT := 1000; -- 전체 댓글 수
SET @BLOCK_SIZE := 5; -- 블록당 댓글(= 1루트 + 자식들)
SET @MAX_DEPTH := 5; -- 최대 트리 깊이(루트 포함)
SET @ROOTS_PER_POST := 3; -- 한 post에 몇 개의 "루트 블록"을 넣을지

TRUNCATE TABLE post_comment;

INSERT INTO post_comment (id, parent_id, review, created_on, score, post_id)
SELECT n                                                             AS id,
       CASE
           WHEN pos = 1 THEN NULL -- 블록 첫 댓글은 루트
           WHEN pos <= deepest THEN n - 1 -- 체인으로 깊이 증가
           ELSE n - (pos - 1) -- 루트에 바로 붙임(깊이 고정)
           END                                                       AS parent_id,

       CONCAT('Comment ', n)                                         AS review,
       TIMESTAMP(DATE_ADD('2025-10-21 10:00:00', INTERVAL n MINUTE)) AS created_on,
       FLOOR(1 + (RAND() * 5))                                       AS score,

    /* 블록을 post에 묶는 방식 */
       CEIL(block / @ROOTS_PER_POST)                                 AS post_id -- 블록 @ROOTS_PER_POST개 = post 1개

FROM (SELECT n,
             ((n - 1) % @BLOCK_SIZE) + 1                                                  AS pos,
             block,
          /* 블록 시작마다 깊이 랜덤 1..@MAX_DEPTH */
             @deepest := IF(@prevBlock = block, @deepest, FLOOR(RAND() * @MAX_DEPTH) + 1) AS deepest,
             @prevBlock := block                                                          AS _
      FROM (
               -- 1..1000 시퀀스 & 블록 번호
               SELECT a.n + 10 * b.n + 100 * c.n + 1                       AS n,
                      CEIL((a.n + 10 * b.n + 100 * c.n + 1) / @BLOCK_SIZE) AS block
               FROM (SELECT 0 n
                     UNION ALL
                     SELECT 1
                     UNION ALL
                     SELECT 2
                     UNION ALL
                     SELECT 3
                     UNION ALL
                     SELECT 4
                     UNION ALL
                     SELECT 5
                     UNION ALL
                     SELECT 6
                     UNION ALL
                     SELECT 7
                     UNION ALL
                     SELECT 8
                     UNION ALL
                     SELECT 9) a
                        CROSS JOIN
                    (SELECT 0 n
                     UNION ALL
                     SELECT 1
                     UNION ALL
                     SELECT 2
                     UNION ALL
                     SELECT 3
                     UNION ALL
                     SELECT 4
                     UNION ALL
                     SELECT 5
                     UNION ALL
                     SELECT 6
                     UNION ALL
                     SELECT 7
                     UNION ALL
                     SELECT 8
                     UNION ALL
                     SELECT 9) b
                        CROSS JOIN
                    (SELECT 0 n
                     UNION ALL
                     SELECT 1
                     UNION ALL
                     SELECT 2
                     UNION ALL
                     SELECT 3
                     UNION ALL
                     SELECT 4
                     UNION ALL
                     SELECT 5
                     UNION ALL
                     SELECT 6
                     UNION ALL
                     SELECT 7
                     UNION ALL
                     SELECT 8
                     UNION ALL
                     SELECT 9) c) base
               CROSS JOIN (SELECT @prevBlock := 0, @deepest := 1) vars
      WHERE n <= @TOTAL_COUNT
      ORDER BY n) t
ORDER BY n;

-- post마다 루트 수 확인
SELECT post_id, COUNT(*) AS roots
FROM post_comment
WHERE parent_id IS NULL
GROUP BY post_id
ORDER BY post_id;

-- 실제 최대 depth 확인
WITH RECURSIVE tree AS (SELECT id, parent_id, post_id, 1 AS depth
                        FROM post_comment
                        WHERE parent_id IS NULL

                        UNION ALL
                        SELECT c.id, c.parent_id, c.post_id, t.depth + 1
                        FROM post_comment c
                                 JOIN tree t ON c.parent_id = t.id)
SELECT post_id, MAX(depth) AS max_depth, COUNT(*) AS cnt
FROM tree
GROUP BY post_id
ORDER BY post_id;

-- 단순히 테스트용
INSERT INTO post_comment (id, parent_id, review, created_on, score, post_id)
VALUES (1, NULL, 'Comment 1', '2019-10-13 12:23:05', 1, 1),
       (2, 1, 'Comment 1.1', '2019-10-14 13:23:10', 2, 1),
       (3, 1, 'Comment 1.2', '2019-10-14 15:45:15', 2, 1),
       (4, 3, 'Comment 1.2.1', '2019-10-15 10:15:20', 1, 1),

       (5, NULL, 'Comment 2', '2019-10-13 15:23:25', 1, 1),
       (6, 5, 'Comment 2.1', '2019-10-14 11:23:30', 1, 1),
       (7, 5, 'Comment 2.2', '2019-10-14 14:45:35', 1, 1),

       (8, NULL, 'Comment 3', '2019-10-15 10:15:40', 1, 1),
       (9, 8, 'Comment 3.1', '2019-10-16 11:15:45', 10, 1),
       (10, 8, 'Comment 3.2', '2019-10-17 18:30:50', -2, 1),

       (11, NULL, 'Comment 4', '2019-10-19 21:43:55', -5, 1),
