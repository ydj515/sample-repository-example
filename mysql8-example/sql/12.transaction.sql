-- LRU : 가장 최근에 사용되지 않은 데이터를 우선적으로 제거하면서 메모리 공간을 확보
-- 1. 풀 테이블 스캔
-- 2. 예측 읽기

-- LRU를 일부 개선
-- Minpoint : 새 페이지를 LRU 리스트의 중간 지점에 삽입 , Old 영역과 New 영역


-- 어댑티브 해시 인덱스 : 자주 사용되는 데이터 접근을 빠르게 처리

-- Change Buffer :

-- 예측 읽기 : 데이터를 미리 읽어온다. (Linear Read-Ahead) (Random Read-Ahead)


-- 버퍼 풀 히트율 확인하기 (95% 이상이면 좋음)
SELECT
    '버퍼 풀 성능 분석' as 분석항목,
    bp_read_requests.VARIABLE_VALUE as 전체요청수,
    bp_reads.VARIABLE_VALUE as 디스크읽기수,
    ROUND(
            (1 - bp_reads.VARIABLE_VALUE / bp_read_requests.VARIABLE_VALUE) * 100, 2
    ) as 히트율_퍼센트,
    CASE
        WHEN (1 - bp_reads.VARIABLE_VALUE / bp_read_requests.VARIABLE_VALUE) * 100 >= 95
            THEN '👍 매우 좋음'
        WHEN (1 - bp_reads.VARIABLE_VALUE / bp_read_requests.VARIABLE_VALUE) * 100 >= 90
            THEN '😊 양호'
        WHEN (1 - bp_reads.VARIABLE_VALUE / bp_read_requests.VARIABLE_VALUE) * 100 >= 80
            THEN '😐 보통'
        ELSE '😱 개선 필요'
        END as 상태
FROM
    (SELECT VARIABLE_VALUE FROM performance_schema.global_status
     WHERE VARIABLE_NAME = 'Innodb_buffer_pool_reads') bp_reads,
    (SELECT VARIABLE_VALUE FROM performance_schema.global_status
     WHERE VARIABLE_NAME = 'Innodb_buffer_pool_read_requests') bp_read_requests;


-- 테이블 락 : 테이블 전체에 락을 건다.
-- 갭락 : 인덱스에 범위 락을 건다.
-- Exclusive Lock : 데이터 수정 락
-- 읽기 락 :