package com.example.springaisample.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.example.springaisample.advisor.PromptTooShortException;
import com.example.springaisample.exception.GlobalExceptionHandler;
import com.example.springaisample.model.Contents;
import com.example.springaisample.model.Question;
import com.example.springaisample.model.Shop;
import com.example.springaisample.service.AdvisorService;
import com.example.springaisample.service.AdvisorStructuredOutputService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

@WebMvcTest(AdvisorController.class)
@Import(GlobalExceptionHandler.class)
class AdvisorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdvisorService advisorService;

    @MockitoBean
    private AdvisorStructuredOutputService advisorStructuredOutputService;

    @Test
    void completionEndpointReturnsAdvisorResponse() throws Exception {
        when(advisorService.chat("테스트 질문")).thenReturn("advisor 응답");

        mockMvc.perform(get("/test/advisor/completion").param("prompt", "테스트 질문"))
                .andExpect(status().isOk())
                .andExpect(content().string("advisor 응답"));

        verify(advisorService).chat("테스트 질문");
    }

    @Test
    void completionEndpointReturnsBadRequestForShortPrompt() throws Exception {
        when(advisorService.chat("가")).thenThrow(new PromptTooShortException("Char size too short"));

        mockMvc.perform(get("/test/advisor/completion").param("prompt", "가"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Char size too short"));
    }

    @Test
    void streamEndpointReturnsFluxContent() throws Exception {
        when(advisorService.chatStream("스트림 질문")).thenReturn(Flux.just("첫", "응답"));

        mockMvc.perform(get("/test/advisor/stream").param("prompt", "스트림 질문"))
                .andExpect(status().isOk());

        verify(advisorService).chatStream("스트림 질문");
    }

    @Test
    void beanOutputEndpointReturnsStructuredContents() throws Exception {
        when(advisorStructuredOutputService.beanOutputConverter(new Question("서울 종로", "맛집", "한국어")))
                .thenReturn(new Contents(
                        "요약",
                        List.of(new Shop("식당 하나", "한식", "서울 종로구", 37.57, 126.98, List.of("비빔밥")))
                ));

        mockMvc.perform(get("/test/advisor/bean-output")
                        .param("location", "서울 종로")
                        .param("content", "맛집")
                        .param("language", "한국어"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("요약"))
                .andExpect(jsonPath("$.items[0].name").value("식당 하나"));
    }
}
