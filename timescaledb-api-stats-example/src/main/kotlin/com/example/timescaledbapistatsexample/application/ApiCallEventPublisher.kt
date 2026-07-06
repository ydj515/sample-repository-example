package com.example.timescaledbapistatsexample.application

import com.example.timescaledbapistatsexample.domain.model.ApiCallEvent
import com.example.timescaledbapistatsexample.domain.model.toStreamFields
import com.example.timescaledbapistatsexample.domain.port.ApiCallEventStreamRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ApiCallEventPublisher(
    private val streamRepository: ApiCallEventStreamRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 요청 처리 스레드에서 동기로 호출되므로 스트림 append(XADD) 1회만 수행한다.
     * 스트림 trimming은 요청 경로가 아니라 consumer poll 주기에서 처리한다.
     */
    fun publish(event: ApiCallEvent) {
        runCatching {
            streamRepository.append(event.toStreamFields())
        }.onFailure { ex ->
            log.warn("Failed to publish API call event to Redis Stream", ex)
        }
    }
}
