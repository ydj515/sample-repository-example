package com.example.quality;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TodoIntegrationTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private EntityManager entityManager;

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"title\":null}", "{\"title\":\"\"}", "{\"title\":\"   \"}"})
  void rejectsInvalidTitleWithoutSaving(String body) throws Exception {
    mockMvc
        .perform(post("/todos").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Invalid todo title"))
        .andExpect(jsonPath("$.detail").value("title must not be blank"))
        .andExpect(jsonPath("$.code").value("INVALID_TODO_TITLE"));
    entityManager.clear();
    mockMvc
        .perform(get("/todos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void persistsAndReloadsTodoThroughHttp() throws Exception {
    mockMvc
        .perform(
            post("/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"quality gates\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber());
    entityManager.flush();
    entityManager.clear();
    mockMvc
        .perform(get("/todos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("quality gates"))
        .andExpect(jsonPath("$.length()").value(1));
  }
}
