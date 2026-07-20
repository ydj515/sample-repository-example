package com.example.timescaledbapistatsexample.presentation

import com.example.timescaledbapistatsexample.presentation.filter.ApiCallCaptureFilter
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * 컨트롤러에서 올라온 예외를 JSON으로 변환하면서, 서버 오류일 때만 예외 타입을 request attribute에 남긴다.
 *
 * [ApiCallCaptureFilter]의 catch 블록만으로는 error_type을 채울 수 없다.
 * Spring MVC는 컨트롤러 예외를 DispatcherServlet에서 처리해 /error로 넘기기 때문에
 * 필터의 catch까지 전파되지 않는다.
 *
 * [ResponseEntityExceptionHandler]를 상속하는 이유:
 * `@ExceptionHandler(Exception::class)`만 두면 Spring MVC가 자체적으로 4xx로 매핑하는 예외
 * (타입 불일치, 미지원 메서드, 파라미터 누락, 잘못된 JSON, 없는 경로)까지 모두 잡아 500으로 바꿔 버린다.
 * 상태 코드가 틀리는 것도 문제지만, 클라이언트 실수가 서버 오류로 집계되어 실패율 통계까지 망가진다.
 * 부모 클래스가 그 예외들을 표준 상태 코드로 처리하게 두고, 여기서는 진짜 예상 못한 예외만 맡는다.
 */
@RestControllerAdvice
class ApiExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 부모가 처리하는 모든 예외가 이 지점을 지난다.
     * 5xx로 떨어지는 경우에만 통계용 error_type을 남긴다.
     */
    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        if (statusCode.is5xxServerError) {
            recordErrorType(request, ex)
            log.warn("Server error while serving request", ex)
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request)
    }

    /** 애플리케이션이 의도적으로 던진 오류. reason은 우리가 쓴 문구라 그대로 내보내도 안전하다. */
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<Map<String, Any?>> {
        return ResponseEntity.status(ex.statusCode).body(
            mapOf(
                "status" to ex.statusCode.value(),
                "message" to (ex.reason ?: "Request failed"),
            ),
        )
    }

    /**
     * 위에서 걸러지지 않은 예상 밖의 예외.
     *
     * 예외 메시지를 응답에 그대로 담지 않는다. SQL 오류나 내부 경로가 노출될 수 있어서다.
     * 상세 내용은 로그로만 남긴다.
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        request: HttpServletRequest,
        ex: Exception,
    ): ResponseEntity<Map<String, Any?>> {
        request.setAttribute(ApiCallCaptureFilter.ATTR_ERROR_TYPE, ex.javaClass.simpleName)
        log.error("Unhandled exception while serving {} {}", request.method, request.servletPath, ex)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            mapOf(
                "status" to HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "message" to "Internal server error",
            ),
        )
    }

    private fun recordErrorType(request: WebRequest, ex: Exception) {
        request.setAttribute(
            ApiCallCaptureFilter.ATTR_ERROR_TYPE,
            ex.javaClass.simpleName,
            RequestAttributes.SCOPE_REQUEST,
        )
    }
}
