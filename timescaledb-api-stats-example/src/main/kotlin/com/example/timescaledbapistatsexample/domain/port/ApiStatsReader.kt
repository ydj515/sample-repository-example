package com.example.timescaledbapistatsexample.domain.port

import com.example.timescaledbapistatsexample.domain.model.AuthFailure
import com.example.timescaledbapistatsexample.domain.model.ApiKeyCallStat
import com.example.timescaledbapistatsexample.domain.model.BucketCount
import com.example.timescaledbapistatsexample.domain.model.BucketFailureRate
import com.example.timescaledbapistatsexample.domain.model.BucketLatency
import com.example.timescaledbapistatsexample.domain.model.ClientCall
import com.example.timescaledbapistatsexample.domain.model.StatsPeriod
import com.example.timescaledbapistatsexample.domain.model.StatsSource
import com.example.timescaledbapistatsexample.domain.model.TopEndpoint
import java.time.Instant

/**
 * TimescaleDB 통계 조회 포트. 도메인 읽기모델을 반환하고, presentation이 응답 DTO로 매핑한다.
 */
interface ApiStatsReader {
    companion object {
        /**
         * 한 응답이 담을 수 있는 최대 행 수.
         *
         * limit은 버킷별 상한이라 총 행 수는 "버킷 수 x limit"까지 늘어난다.
         * period=day로 넓은 구간을 limit=500으로 조회하면 수만 행이 한 응답에 담긴다.
         * DB 용량이 아니라 직렬화 메모리, 전송량, 클라이언트 렌더링이 먼저 무너진다.
         */
        const val MAX_TOTAL_ROWS = 5000
    }

    fun calls(bucket: String, from: Instant, to: Instant): List<BucketCount>

    fun latency(bucket: String, from: Instant, to: Instant): List<BucketLatency>

    fun failureRate(bucket: String, from: Instant, to: Instant): List<BucketFailureRate>

    fun apiKeyCalls(
        period: StatsPeriod,
        source: StatsSource,
        from: Instant,
        to: Instant,
        apiClientId: Long?,
        method: String?,
        pathPattern: String?,
        limit: Int,
    ): List<ApiKeyCallStat>

    fun topEndpoints(from: Instant, to: Instant, limit: Int): List<TopEndpoint>

    fun clients(from: Instant, to: Instant): List<ClientCall>

    fun authFailures(from: Instant, to: Instant): List<AuthFailure>
}
