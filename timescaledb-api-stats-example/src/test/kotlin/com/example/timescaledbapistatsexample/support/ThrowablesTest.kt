package com.example.timescaledbapistatsexample.support

import kotlin.test.Test
import kotlin.test.assertTrue

class ThrowablesTest {
    @Test
    fun `중첩 예외 메시지에서 BUSYGROUP을 찾는다`() {
        val exception = RuntimeException("Error in execution", IllegalStateException("BUSYGROUP Consumer Group name already exists"))

        assertTrue(exception.hasMessageInChain("BUSYGROUP"))
    }
}
