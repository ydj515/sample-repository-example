-- 웹 사이트 클릭 로그가 스트림으로 실시간으로 들어온다.

{ "user_id": 101, "page": "/products/1", "timestamp": "2024-04-25 10:00:01" }
{ "user_id": 204, "page": "/main", "timestamp": "2024-04-25 10:00:03" }
{ "user_id": 101, "page": "/products/1", "timestamp": "2024-04-25 10:00:05" }


SELECT
    page,
    COUNT(*) AS click_count
FROM clicks
GROUP BY
    -- 10초 간격의 텀블링 윈도우를 정의
    TUMBLE(timestamp, INTERVAL '10' SECOND),
    page;

-- 0 ~ 10초 사이 윈도우1
-- 10 ~ 20초사이 윈도우2

-- Apache Flink SQL, Kafa SQL(ksqlDB)

-- 출력 예시
-- 10:00:10 시점에 출력
{ "window_end": "10:00:10", "page": "/products/1", "click_count": 15 }
{ "window_end": "10:00:10", "page": "/main", "click_count": 8 }

-- 10:00:20 시점에 출력
{ "window_end": "10:00:20", "page": "/cart", "click_count": 12 }-- 웹 사이트 클릭 로그가 스트림으로 실시간으로 들어온다.


-------

{ "user_id": 101, "page": "/products/1", "timestamp": "2024-04-25 10:00:01" }
{ "user_id": 204, "page": "/main", "timestamp": "2024-04-25 10:00:03" }
{ "user_id": 101, "page": "/products/1", "timestamp": "2024-04-25 10:00:05" }


SELECT
    page,
    COUNT(*) AS click_count
FROM clicks
GROUP BY
    -- 10초 간격의 텀블링 윈도우를 정의
    TUMBLE(timestamp, INTERVAL '10' SECOND),
    page;

-- 0 ~ 10분 사이 윈도우1
-- 10 ~ 20분 사이 윈도우2

-- Apache Flink SQL, Kafa SQL(ksqlDB)

-- 10:00:10 시점에 출력
{ "window_end": "10:00:10", "page": "/products/1", "click_count": 15 }
{ "window_end": "10:00:10", "page": "/main", "click_count": 8 }

-- 10:00:20 시점에 출력
{ "window_end": "10:00:20", "page": "/cart", "click_count": 12 }