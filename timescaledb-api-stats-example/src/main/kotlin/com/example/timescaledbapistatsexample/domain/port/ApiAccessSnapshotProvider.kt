package com.example.timescaledbapistatsexample.domain.port

import com.example.timescaledbapistatsexample.domain.model.ApiAccessSnapshot

/**
 * 인증/인가에 필요한 client, route, 권한 스냅샷을 제공하는 포트.
 */
fun interface ApiAccessSnapshotProvider {
    fun loadSnapshot(): ApiAccessSnapshot
}
