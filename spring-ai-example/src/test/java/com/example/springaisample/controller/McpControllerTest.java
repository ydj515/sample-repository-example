package com.example.springaisample.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.springaisample.service.mcp.McpAccessService;
import com.example.springaisample.service.mcp.McpWeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(McpController.class)
class McpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private McpWeatherService mcpWeatherService;

    @MockitoBean
    private McpAccessService mcpAccessService;

    @Test
    void weatherEndpointReturnsServiceResponse() throws Exception {
        when(mcpWeatherService.chat("오늘 날씨 알려줘")).thenReturn("서울은 맑고 12도입니다.");

        mockMvc.perform(get("/test/mcp/weather").param("prompt", "오늘 날씨 알려줘"))
                .andExpect(status().isOk())
                .andExpect(content().string("서울은 맑고 12도입니다."));

        verify(mcpWeatherService).chat("오늘 날씨 알려줘");
    }

    @Test
    void accessEndpointReturnsServiceResponse() throws Exception {
        MockMultipartFile attach = new MockMultipartFile("attach", "badge.png", "image/png", "img".getBytes());
        when(mcpAccessService.analyzeAccessImage(any(), any())).thenReturn("출입문이 열립니다.");

        mockMvc.perform(multipart("/test/mcp/access").file(attach))
                .andExpect(status().isOk())
                .andExpect(content().string("출입문이 열립니다."));

        verify(mcpAccessService).analyzeAccessImage(any(), any());
    }

    @Test
    void accessEndpointRejectsMissingImage() throws Exception {
        mockMvc.perform(multipart("/test/mcp/access"))
                .andExpect(status().isOk())
                .andExpect(content().string("이미지를 올려주세요."));

        verifyNoInteractions(mcpAccessService);
    }
}
