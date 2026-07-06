package com.example.timescaledbapistatsexample.domain.port

import com.example.timescaledbapistatsexample.domain.model.StreamEntry
import java.time.Duration

/**
 * API 호출 이벤트 스트림에 대한 도메인 포트.
 * 상위 계층(publisher, consumer)은 이 인터페이스에만 의존하고,
 * Redis 접근 방식은 infrastructure 구현체에 캡슐화한다.
 */
interface ApiCallEventStreamRepository {
    /** 여러 이벤트 필드를 한 번의 파이프라인으로 스트림에 추가한다(batch XADD). */
    fun appendAll(fieldsList: List<Map<String, String>>)

    /** consumer group을 생성한다. 이미 존재하면 무시한다(XGROUP CREATE MKSTREAM). */
    fun ensureConsumerGroup()

    /** 아직 전달되지 않은 새 메시지를 batch로 읽는다(XREADGROUP `>`). */
    fun readNew(batchSize: Long, block: Duration): List<StreamEntry>

    /**
     * idle 시간이 [minIdle]을 넘긴 pending 메시지를 이 consumer로 회수한다(XPENDING + XCLAIM).
     * 다른 consumer가 전달받고 ACK하지 못한 채 죽은 메시지를 복구하는 용도다.
     */
    fun claimStale(minIdle: Duration, batchSize: Long): List<StreamEntry>

    /** 처리 완료된 메시지를 ACK한다(XACK). */
    fun acknowledge(ids: List<String>)

    /** 스트림을 근사 트리밍해 크기를 제한한다(XTRIM MAXLEN ~). */
    fun trim(maxLen: Long)
}
