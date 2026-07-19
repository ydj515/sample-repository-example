package com.example.timescaledbapistatsexample.presentation

import com.example.timescaledbapistatsexample.presentation.filter.ApiCallCaptureFilter
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

/**
 * 컨트롤러에서 올라온 예외를 JSON으로 변환하면서, 예외 타입을 request attribute에 남긴다.
 *
 * [ApiCallCaptureFilter]의 catch 블록만으로는 error_type을 채울 수 없다.
 * Spring MVC는 컨트롤러 예외를 DispatcherServlet에서 처리해 /error로 넘기기 때문에
 * 필터의 catch까지 전파되지 않고, 그래서 error_type 컬럼이 항상 비어 있었다.
 */
@RestControllerAdvice
class ApiExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(
        request: HttpServletRequest,
        ex: ResponseStatusException,
    ): ResponseEntity<Map<String, Any?>> {
        // 400 같은 의도된 클라이언트 오류는 장애가 아니므로 error_type으로 기록하지 않는다.
        return ResponseEntity.status(ex.statusCode).body(
            mapOf(
                "status" to ex.statusCode.value(),
                "message" to (ex.reason ?: "Request failed"),
            ),
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        request: HttpServletRequest,
        ex: Exception,
    ): ResponseEntity<Map<String, Any?>> {
        request.setAttribute(ApiCallCaptureFilter.ATTR_ERROR_TYPE, ex.javaClass.simpleName)
        log.warn("Unhandled exception while serving {} {}", request.method, request.servletPath, ex)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            mapOf(
                "status" to HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "message" to (ex.message ?: "Unexpected error"),
            ),
        )
    }
}
