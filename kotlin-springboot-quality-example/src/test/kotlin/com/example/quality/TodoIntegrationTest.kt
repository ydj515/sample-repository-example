package com.example.quality

import com.example.quality.repository.todo.JpaTodoRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class TodoIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val repository: JpaTodoRepository,
    ) : FunSpec() {
        override fun extensions() = listOf(SpringExtension)

        init {
            beforeTest { repository.deleteAll() }

            test("GET returns an empty array before creation") {
                mockMvc.get("/todos").andExpect {
                    status { isOk() }
                    content { json("[]") }
                }
            }

            test("POST persists todos and GET returns them in id order") {
                listOf("first", "second").forEach { title ->
                    mockMvc
                        .post("/todos") {
                            contentType = MediaType.APPLICATION_JSON
                            content = """{"title":"$title"}"""
                        }.andExpect {
                            status { isCreated() }
                            jsonPath("$.id") { isNumber() }
                            jsonPath("$.title") { value(title) }
                        }
                }

                mockMvc.get("/todos").andExpect {
                    status { isOk() }
                    jsonPath("$.length()") { value(2) }
                    jsonPath("$[0].title") { value("first") }
                    jsonPath("$[1].title") { value("second") }
                }
                repository.findAllByOrderByIdAsc().map { it.title } shouldBe listOf("first", "second")
            }

            listOf(
                "{}",
                """{"title":null}""",
                """{"title":""}""",
                """{"title":"   "}""",
                """{"title":"\t\n"}""",
            ).forEachIndexed { index, body ->
                test("invalid title case $index returns a problem response without saving") {
                    mockMvc
                        .post("/todos") {
                            contentType = MediaType.APPLICATION_JSON
                            content = body
                        }.andExpect {
                            status { isBadRequest() }
                            content { contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) }
                            jsonPath("$.type") { value("about:blank") }
                            jsonPath("$.title") { value("Bad Request") }
                            jsonPath("$.status") { value(400) }
                            jsonPath("$.detail") { value("title must not be blank") }
                            jsonPath("$.instance") { value("/todos") }
                        }
                    repository.count() shouldBe 0L
                }
            }

            test("malformed JSON fails before persistence with a stable problem response") {
                mockMvc
                    .post("/todos") {
                        contentType = MediaType.APPLICATION_JSON
                        content = "{"
                    }.andExpect {
                        status { isBadRequest() }
                        content { contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) }
                        jsonPath("$.status") { value(400) }
                        jsonPath("$.detail") { value("request body must be valid JSON") }
                        jsonPath("$.instance") { value("/todos") }
                    }
                repository.count() shouldBe 0L
            }
        }
    }
