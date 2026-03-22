package com.example.springaisample.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import com.example.springaisample.model.Contents;
import com.example.springaisample.model.Question;
import com.example.springaisample.model.Shop;
import com.example.springaisample.service.PromptTemplateResourceService;
import com.example.springaisample.service.StructuredOutputService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PromptTemplateController.class)
class PromptTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PromptTemplateResourceService promptTemplateResourceService;

    @MockBean
    private StructuredOutputService structuredOutputService;

    @Test
    void templateEndpointBindsQueryParameters() throws Exception {
        when(promptTemplateResourceService.promptTemplate3(new Question("서울 종로", "맛집", "영어")))
                .thenReturn("템플릿 응답");

        mockMvc.perform(get("/test/prompt/template")
                        .param("location", "서울 종로")
                        .param("content", "맛집")
                        .param("language", "영어"))
                .andExpect(status().isOk())
                .andExpect(content().string("템플릿 응답"));

        verify(promptTemplateResourceService).promptTemplate3(new Question("서울 종로", "맛집", "영어"));
    }

    @Test
    void templateEndpointUsesQuestionDefaultsWhenQueryParametersAreMissing() throws Exception {
        when(promptTemplateResourceService.promptTemplate3(new Question(null, null, null)))
                .thenReturn("기본값 응답");

        mockMvc.perform(get("/test/prompt/template"))
                .andExpect(status().isOk())
                .andExpect(content().string("기본값 응답"));

        verify(promptTemplateResourceService).promptTemplate3(new Question(null, null, null));
    }

    @Test
    void listEndpointReturnsJsonArray() throws Exception {
        when(structuredOutputService.listOutputConverter(new Question("서울 종로", "카페", "한국어")))
                .thenReturn(List.of("카페 하나", "카페 둘"));

        mockMvc.perform(get("/test/prompt/list")
                        .param("location", "서울 종로")
                        .param("content", "카페")
                        .param("language", "한국어"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("카페 하나"))
                .andExpect(jsonPath("$[1]").value("카페 둘"));
    }

    @Test
    void mapEndpointReturnsJsonObject() throws Exception {
        when(structuredOutputService.mapOutputConverter(new Question("서울 종로", "숙박업소", "영어")))
                .thenReturn(Map.of("name", "호텔 하나", "summary", "도심 접근성이 좋음"));

        mockMvc.perform(get("/test/prompt/map")
                        .param("location", "서울 종로")
                        .param("content", "숙박업소")
                        .param("language", "영어"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("호텔 하나"))
                .andExpect(jsonPath("$.summary").value("도심 접근성이 좋음"));
    }

    @Test
    void beanEndpointReturnsJsonObject() throws Exception {
        when(structuredOutputService.beanOutputConverter(new Question("서울 종로", "맛집", "한국어")))
                .thenReturn(new Contents(
                        "요약",
                        List.of(new Shop("식당 하나", "한식", "서울 종로구", 37.57, 126.98, List.of("비빔밥")))
                ));

        mockMvc.perform(get("/test/prompt/bean")
                        .param("location", "서울 종로")
                        .param("content", "맛집")
                        .param("language", "한국어"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("요약"))
                .andExpect(jsonPath("$.items[0].name").value("식당 하나"));
    }

    @Test
    void shopsEndpointReturnsJsonArray() throws Exception {
        when(structuredOutputService.parameterizedTypeReference(new Question("서울 종로", "숙박업소", "한국어")))
                .thenReturn(List.of(
                        new Shop("호텔 하나", "비즈니스 호텔", "서울 종로구 1번지", 37.57, 126.98, List.of()),
                        new Shop("호텔 둘", "부티크 호텔", "서울 종로구 2번지", 37.58, 126.99, List.of())
                ));

        mockMvc.perform(get("/test/prompt/shops")
                        .param("location", "서울 종로")
                        .param("content", "숙박업소")
                        .param("language", "한국어"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("호텔 하나"))
                .andExpect(jsonPath("$[1].address").value("서울 종로구 2번지"));
    }
}
