package com.example.springaisample.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;

class MultimodalServiceTest {

    @Test
    void generateImageUrlUsesDallE3AndReturnsUrl() {
        CapturingImageModel imageModel = new CapturingImageModel(imageResponse("https://example.com/image.png", null));
        MultimodalService service = new MultimodalService(
                ChatClient.builder(new CapturingChatModel()).build(),
                imageModel
        );

        String result = service.generateImageUrl("노을지는 바다를 그려줘");

        ImagePrompt prompt = imageModel.lastPrompt();
        OpenAiImageOptions options = (OpenAiImageOptions) prompt.getOptions();

        assertThat(result).isEqualTo("https://example.com/image.png");
        assertThat(prompt.getInstructions()).singleElement().satisfies(message ->
                assertThat(message.getText()).isEqualTo("노을지는 바다를 그려줘"));
        assertThat(options.getModel()).isEqualTo("dall-e-3");
        assertThat(options.getResponseFormat()).isEqualTo("url");
        assertThat(options.getWidth()).isEqualTo(1024);
        assertThat(options.getHeight()).isEqualTo(1024);
        assertThat(options.getN()).isEqualTo(1);
    }

    @Test
    void generateImageBase64ReturnsBase64Payload() {
        CapturingImageModel imageModel = new CapturingImageModel(imageResponse(null, "base64-image"));
        MultimodalService service = new MultimodalService(
                ChatClient.builder(new CapturingChatModel()).build(),
                imageModel
        );

        String result = service.generateImageBase64("우주 정거장을 그려줘");

        ImagePrompt prompt = imageModel.lastPrompt();
        OpenAiImageOptions options = (OpenAiImageOptions) prompt.getOptions();

        assertThat(result).isEqualTo("base64-image");
        assertThat(options.getResponseFormat()).isEqualTo("b64_json");
    }

    @Test
    void analyzeImageBuildsVisionPromptWithImageAttachment() {
        byte[] imageBytes = "image-bytes".getBytes(StandardCharsets.UTF_8);
        CapturingChatModel chatModel = new CapturingChatModel("분석 결과");
        MultimodalService service = new MultimodalService(
                ChatClient.builder(chatModel).build(),
                new CapturingImageModel(imageResponse("https://example.com/image.png", null))
        );

        List<String> result = service.analyzeImage("무엇이 보이나요?", "image/png", imageBytes)
                .collectList()
                .block();

        Prompt prompt = chatModel.lastPrompt();

        assertThat(result).containsExactly("분석 결과");
        assertThat(prompt.getSystemMessage().getText())
                .contains("이미지 분석가")
                .contains("한국어");
        assertThat(prompt.getUserMessage().getText()).isEqualTo("무엇이 보이나요?");
        assertThat(prompt.getUserMessage().getMedia()).hasSize(1);
        assertThat(prompt.getUserMessage().getMedia().get(0).getMimeType().toString()).isEqualTo("image/png");
        assertThat(prompt.getUserMessage().getMedia().get(0).getDataAsByteArray()).isEqualTo(imageBytes);
    }

    private static ImageResponse imageResponse(String url, String base64) {
        return new ImageResponse(List.of(new ImageGeneration(new Image(url, base64))));
    }
}
