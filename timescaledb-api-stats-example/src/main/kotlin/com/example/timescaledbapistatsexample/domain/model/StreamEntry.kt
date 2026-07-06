package com.example.timescaledbapistatsexample.domain.model

/**
 * Redis Stream에서 읽어온 단일 엔트리의 도메인 표현.
 * 구현 기술(Redis MapRecord)을 상위 계층에 노출하지 않기 위한 최소 모델이다.
 */
data class StreamEntry(
    val id: String,
    val fields: Map<String, String>,
)
