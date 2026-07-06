package com.example.timescaledbapistatsexample.presentation.filter

/**
 * 인증 필터와 캡처 필터가 공통으로 건너뛰는 요청 경로 판정.
 * 통계 조회 API를 캡처하면 통계가 다시 통계 이벤트를 만드는 순환이 생기므로 제외한다.
 */
object RequestFilterExclusions {
    fun isExcluded(path: String): Boolean {
        return path.startsWith("/actuator/") ||
            path.startsWith("/api/stats/") ||
            path == "/favicon.ico"
    }
}
