package com.example.oidcsimpleexample

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.Session
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@ActiveProfiles("test")
class OidcSimpleExampleApplicationTests {

    @MockitoBean
    lateinit var sessionRepository: FindByIndexNameSessionRepository<Session>

    @Test
    fun contextLoads() {
    }

}
