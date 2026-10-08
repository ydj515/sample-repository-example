package com.example.quality;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
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
