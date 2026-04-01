package com.example.springaisample.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.content.Media;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

@Service
public class MultimodalService {

    // System Message 생성
    private static final String IMAGE_ANALYSIS_SYSTEM_MESSAGE = """
            너는 이미지 분석가 입니다.
            사용자가 전송한 이미지를 기반으로 사용자의 질문에 맞게 분석하고 답변을 한국어로 하세요.
            """;
    private final PromptTemplate systemPrompt = PromptTemplate.builder()
            .template(IMAGE_ANALYSIS_SYSTEM_MESSAGE)
            .build();

    private final ChatClient chatClient;
    private final ImageModel imageModel;

    public MultimodalService(
            @Qualifier("openAiChatClient") ChatClient openAiChatClient,
            ImageModel imageModel
    ) {
        this.chatClient = openAiChatClient.mutate().build();
        this.imageModel = imageModel;
    }

    // Text를 이미지 URL로 생성
    public String generateImageUrl(String prompt) {
        return generateImage(prompt, "url")
                .getResult()
                .getOutput()
                .getUrl();
    }

    // Text를 이미지 파일로 생성
    public String generateImageBase64(String prompt) {
        return generateImage(prompt, "b64_json")
                .getResult()
                .getOutput()
                .getB64Json();
    }

    // Image 파일과 질문을 이용해 Image 분석
    public Flux<String> analyzeImage(String question, String contentType, byte[] bytes) {
        Message systemMessage = new SystemMessage(systemPrompt.render());

        Media media = Media.builder()
                .mimeType(MimeType.valueOf(contentType))
                .data(new ByteArrayResource(bytes))
                .build();

        UserMessage userMessage = UserMessage.builder()
                .text(question)
                .media(media)
                .build();

        return this.chatClient.prompt()
                .messages(userMessage, systemMessage)
                .stream()
                .content();
    }

    // Text를 이미지로 생성 Format에 따라 URL 또는 Image 파일로 생성
    private ImageResponse generateImage(String prompt, String responseFormat) {
        ImageMessage imageMessage = new ImageMessage(prompt);

        OpenAiImageOptions imageOptions = OpenAiImageOptions.builder()
                .model("dall-e-3")
                .responseFormat(responseFormat)
                .width(1024)
                .height(1024)
                .N(1)
                .build();

        List<ImageMessage> imageMessageList = List.of(imageMessage);
        ImagePrompt imagePrompt = new ImagePrompt(imageMessageList, imageOptions);

        return this.imageModel.call(imagePrompt);
    }
}
