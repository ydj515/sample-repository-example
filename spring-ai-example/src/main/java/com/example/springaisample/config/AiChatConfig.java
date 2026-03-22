package com.example.springaisample.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiChatConfig {

    @Bean
    ChatClient.Builder chatClientBuilder(
            OpenAiChatModel openAiChatModel,
            AnthropicChatModel anthropicChatModel,
            @Value("${app.chat.default-provider:openai}") String defaultProvider
    ) {
        return ChatClient.builder(selectChatModel(defaultProvider, openAiChatModel, anthropicChatModel));
    }

    @Bean
    ChatClient openAiChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("당신은 Spring AI 샘플 앱에 연결된 OpenAI 어시스턴트입니다. 답변은 간결하고 정확하게 작성하세요.")
                .build();
    }

    @Bean
    ChatClient anthropicChatClient(AnthropicChatModel anthropicChatModel) {
        return ChatClient.builder(anthropicChatModel)
                .defaultSystem("당신은 Spring AI 샘플 앱에 연결된 Claude 어시스턴트입니다. 답변은 간결하고 정확하게 작성하세요.")
                .build();
    }

    private ChatModel selectChatModel(
            String defaultProvider,
            OpenAiChatModel openAiChatModel,
            AnthropicChatModel anthropicChatModel
    ) {
        String normalizedProvider = defaultProvider.trim().toLowerCase();

        if ("anthropic".equals(normalizedProvider) || "claude".equals(normalizedProvider)) {
            return anthropicChatModel;
        }

        return openAiChatModel;
    }
}
