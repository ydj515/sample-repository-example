package com.example.springaisample.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AiChatController {

    private final ChatClient openAiChatClient;
    private final ChatClient anthropicChatClient;
    private final String openAiModel;
    private final String anthropicModel;

    public AiChatController(
            @Qualifier("openAiChatClient") ChatClient openAiChatClient,
            @Qualifier("anthropicChatClient") ChatClient anthropicChatClient,
            @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String openAiModel,
            @Value("${spring.ai.anthropic.chat.options.model:claude-sonnet-4-5}") String anthropicModel
    ) {
        this.openAiChatClient = openAiChatClient;
        this.anthropicChatClient = anthropicChatClient;
        this.openAiModel = openAiModel;
        this.anthropicModel = anthropicModel;
    }

    @GetMapping("/ai/chat")
    public AiChatResponse chat(
            @RequestParam(defaultValue = "anthropic") String provider,
            @RequestParam(defaultValue = "Spring AI에서 현재 설정된 모델을 설명해줘") String message
    ) {
        String normalizedProvider = provider.trim().toLowerCase();

        if ("openai".equals(normalizedProvider)) {
            String answer = this.openAiChatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            return new AiChatResponse("openai", this.openAiModel, message, answer);
        }

        if (!"anthropic".equals(normalizedProvider) && !"claude".equals(normalizedProvider)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "provider는 openai, anthropic, claude 중 하나여야 합니다."
            );
        }

        String answer = this.anthropicChatClient.prompt()
                .user(message)
                .call()
                .content();

        return new AiChatResponse("anthropic", this.anthropicModel, message, answer);
    }

    public record AiChatResponse(String provider, String model, String message, String answer) {
    }
}
