-- 하나의 post에 댓글 만개 생성
-- 설정값 (이 부분만 수정하면 전체 동작 변경)
SET @TOTAL_COUNT := 10000;  -- 생성할 전체 댓글 수 (10,000)
SET @BLOCK_SIZE  := 5;      -- 블록당 댓글 수 (= 1 루트 + 4 자식)
SET @MAX_DEPTH   := 5;      -- 블록 내 최대 depth(루트 포함, 1~5 랜덤)

TRUNCATE TABLE post_comment;

-- 데이터 삽입
INSERT INTO post_comment (id, parent_id, review, created_on, score, post_id)
SELECT
    n AS id,

    /* 부모 ID 결정:
       - pos = 1         → 루트(NULL)
       - pos <= deepest  → 체인(직전 n-1을 부모로) → 깊이 증가
       - pos > deepest   → 루트에 바로 붙임(깊이 고정)
    */
    CASE
        WHEN pos = 1 THEN NULL
        WHEN pos <= deepest THEN n - 1
        ELSE n - (pos - 1)
        END AS parent_id,

    CONCAT('Comment ', n) AS review,
    TIMESTAMP(DATE_ADD('2025-10-21 10:00:00', INTERVAL n MINUTE)) AS created_on,
    FLOOR(1 + (RAND() * 5)) AS score,

    1 AS post_id -- post_id는 1로 고정

FROM (
         SELECT
             n,
             ((n - 1) % @BLOCK_SIZE) + 1 AS pos,  -- 블록 내 위치 1..@BLOCK_SIZE
             block,
             -- 블록 시작마다 1..@MAX_DEPTH에서 랜덤 뽑기
             @deepest := IF(@prevBlock = block, @deepest, FLOOR(RAND() * @MAX_DEPTH) + 1) AS deepest,
             @prevBlock := block AS _
         FROM (
                  /* 🔢 1..10000 시퀀스 & 블록 번호 (10 x 10 x 10 x 10 = 10000) */
                  SELECT
                      a.n + 10*b.n + 100*c.n + 1000*d.n + 1 AS n,
                      CEIL((a.n + 10*b.n + 100*c.n + 1000*d.n + 1) / @BLOCK_SIZE) AS block
                  FROM (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
                           CROSS JOIN
                       (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
                           CROSS JOIN
                       (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
                           CROSS JOIN
                       (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d
              ) base
                  CROSS JOIN (SELECT @prevBlock := 0, @deepest := 1) vars
         WHERE n <= @TOTAL_COUNT
         ORDER BY n  -- 사용자 변수 일관성 보장용
     ) t
ORDER BY n;