package com.example.timescaledbapistatsexample.application

import com.example.timescaledbapistatsexample.domain.model.ApiCallEventRecord
import com.example.timescaledbapistatsexample.domain.model.StreamEntry
import org.slf4j.LoggerFactory

/**
 * 파싱 결과. 정상 record와 그 stream id, 변환 불가로 skip한 stream id를 구분한다.
 */
data class ParsedEntries(
    val records: List<ApiCallEventRecord>,
    val validIds: List<String>,
    val invalidIds: List<String>,
)

/**
 * Redis Stream 엔트리를 저장 record로 변환한다.
 * 한 건이 깨져도 배치 전체를 버리지 않도록 개별 변환하고, 실패 건은 skip 대상으로 격리한다.
 */
object StreamEntryParser {
    private val log = LoggerFactory.getLogger(javaClass)

    fun parse(entries: List<StreamEntry>): ParsedEntries {
        val records = mutableListOf<ApiCallEventRecord>()
        val validIds = mutableListOf<String>()
        val invalidIds = mutableListOf<String>()

        entries.forEach { entry ->
            runCatching { ApiCallEventRecord.from(entry.id, entry.fields) }
                .onSuccess {
                    records += it
                    validIds += entry.id
                }
                .onFailure { ex ->
                    log.warn("Skipping malformed Redis Stream record {}: {}", entry.id, ex.message)
                    invalidIds += entry.id
                }
        }

        return ParsedEntries(records = records, validIds = validIds, invalidIds = invalidIds)
    }
}
