package com.example.timescaledbapistatsexample.domain.port

import com.example.timescaledbapistatsexample.domain.model.ApiCallEventRecord

/**
 * 저장소에 API 호출 이벤트를 영속화하는 포트.
 * 재처리 시 중복은 구현체의 멱등 저장(예: primary key 충돌 무시)으로 흡수한다.
 */
interface ApiCallEventStore {
    fun write(records: List<ApiCallEventRecord>)
}
