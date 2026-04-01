package com.example.springaisample.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import com.example.springaisample.service.MultimodalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

@WebMvcTest(MultimodalController.class)
class MultimodalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MultimodalService multimodalService;

    @Test
    void generateImageUrlEndpointReturnsServiceResponse() throws Exception {
        when(multimodalService.generateImageUrl("고양이 캐릭터 그려줘"))
                .thenReturn("https://example.com/cat.png");

        mockMvc.perform(get("/test/multimodal/generate-image-url").param("prompt", "고양이 캐릭터 그려줘"))
                .andExpect(status().isOk())
                .andExpect(content().string("https://example.com/cat.png"));

        verify(multimodalService).generateImageUrl("고양이 캐릭터 그려줘");
    }

    @Test
    void generateImageEndpointReturnsBase64Response() throws Exception {
        when(multimodalService.generateImageBase64("산책하는 강아지 그려줘"))
                .thenReturn("base64-payload");

        mockMvc.perform(get("/test/multimodal/generate-image").param("prompt", "산책하는 강아지 그려줘"))
                .andExpect(status().isOk())
                .andExpect(content().string("base64-payload"));

        verify(multimodalService).generateImageBase64("산책하는 강아지 그려줘");
    }

    @Test
    void imageAnalysisEndpointReturnsServiceStreamForImageUpload() throws Exception {
        MockMultipartFile attach = new MockMultipartFile("attach", "sample.png", "image/png", "img".getBytes());
        when(multimodalService.analyzeImage(eq("무엇이 보이나요?"), eq("image/png"), any(byte[].class)))
                .thenReturn(Flux.just("분석", " 결과"));

        mockMvc.perform(multipart("/test/multimodal/image-analysis")
                        .file(attach)
                        .param("question", "무엇이 보이나요?"))
                .andExpect(status().isOk());

        verify(multimodalService).analyzeImage(eq("무엇이 보이나요?"), eq("image/png"), any(byte[].class));
    }

    @Test
    void imageAnalysisEndpointRejectsNonImageUpload() throws Exception {
        MockMultipartFile attach = new MockMultipartFile("attach", "sample.txt", "text/plain", "text".getBytes());

        mockMvc.perform(multipart("/test/multimodal/image-analysis")
                        .file(attach)
                        .param("question", "무엇이 보이나요?"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("data:이미지를 올려주세요.\n\n".getBytes(StandardCharsets.UTF_8)));

        verifyNoInteractions(multimodalService);
    }
}
