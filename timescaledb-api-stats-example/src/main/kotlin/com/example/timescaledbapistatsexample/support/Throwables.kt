package com.example.timescaledbapistatsexample.support

/**
 * 예외 cause 체인 어딘가에 [fragment] 문자열을 가진 메시지가 있는지 검사한다.
 * Redis 드라이버가 원인 예외를 여러 겹으로 감싸므로 BUSYGROUP 같은 신호를 찾을 때 사용한다.
 */
fun Throwable.hasMessageInChain(fragment: String): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current.message?.contains(fragment) == true) {
            return true
        }
        current = current.cause
    }
    return false
}
