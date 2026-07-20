package com.example.timescaledbapistatsexample.presentation

import com.example.timescaledbapistatsexample.presentation.filter.ApiCallCaptureFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * 예외 처리는 Spring MVC를 거쳐야만 실제 동작이 드러난다.
 *
 * 컨트롤러 메서드를 직접 호출하는 단위 테스트는 디스패처를 거치지 않아,
 * `@ExceptionHandler(Exception::class)`가 프레임워크 예외까지 삼켜 4xx를 500으로 바꾸는 문제를
 * 잡지 못한다. 그래서 MockMvc로 검증한다.
 */
class ApiExceptionHandlerTest {
    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(ProbeController())
        .setControllerAdvice(ApiExceptionHandler())
        .build()

    @Test
    fun `경로 변수 타입이 맞지 않으면 400을 준다`() {
        val result = mockMvc.perform(get("/probe/echo/not-a-number")).andReturn()

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.response.status)
        assertNull(
            result.request.getAttribute(ApiCallCaptureFilter.ATTR_ERROR_TYPE),
            "클라이언트 실수는 error_type으로 기록하지 않아야 합니다",
        )
    }

    @Test
    fun `지원하지 않는 메서드는 405를 준다`() {
        val result = mockMvc.perform(post("/probe/echo/1")).andReturn()

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), result.response.status)
        assertNull(result.request.getAttribute(ApiCallCaptureFilter.ATTR_ERROR_TYPE))
    }

    @Test
    fun `필수 파라미터가 없으면 400을 준다`() {
        val result = mockMvc.perform(get("/probe/need-param")).andReturn()

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.response.status)
        assertNull(result.request.getAttribute(ApiCallCaptureFilter.ATTR_ERROR_TYPE))
    }

    @Test
    fun `애플리케이션이 던진 ResponseStatusException은 상태와 안내 문구를 유지한다`() {
        val result = mockMvc.perform(get("/probe/bad-request")).andReturn()

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.response.status)
        assertTrue(
            result.response.contentAsString.contains("period must be day, month, or year"),
            "우리가 쓴 안내 문구는 그대로 전달해야 합니다: ${result.response.contentAsString}",
        )
    }

    @Test
    fun `예상 못한 예외는 500이고 내부 메시지를 노출하지 않는다`() {
        val result = mockMvc.perform(get("/probe/boom")).andReturn()

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.response.status)
        assertTrue(
            !result.response.contentAsString.contains(SECRET_DETAIL),
            "예외 메시지가 응답에 노출됐습니다: ${result.response.contentAsString}",
        )
        assertEquals(
            "IllegalStateException",
            result.request.getAttribute(ApiCallCaptureFilter.ATTR_ERROR_TYPE),
            "서버 오류는 통계용 error_type을 남겨야 합니다",
        )
    }

    private fun get(path: String) =
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)

    private fun post(path: String) =
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)

    @RestController
    private class ProbeController {
        @GetMapping("/probe/echo/{id}")
        fun echo(@PathVariable id: Long): Map<String, Long> = mapOf("id" to id)

        @GetMapping("/probe/need-param")
        fun needParam(@RequestParam value: String): Map<String, String> = mapOf("value" to value)

        @GetMapping("/probe/bad-request")
        fun badRequest(): Nothing =
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "period must be day, month, or year")

        @GetMapping("/probe/boom")
        fun boom(): Nothing = throw IllegalStateException(SECRET_DETAIL)
    }

    companion object {
        private const val SECRET_DETAIL = "jdbc:postgresql://internal-host:5432 connection failed"
    }
}
