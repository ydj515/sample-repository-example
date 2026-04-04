package com.example.springaisample.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.springaisample.service.tool.ShoppingRecommendationToolService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

class ShoppingRecommendationToolServiceTest {

    @Test
    void getRecommendationsAddsToolContextAndSystemPrompt() {
        CapturingChatModel chatModel = new CapturingChatModel("1. 청반바지: 50000, 바지, 빨강");
        ShoppingRecommendationToolService service = new ShoppingRecommendationToolService(ChatClient.builder(chatModel));

        String result = service.getRecommendations("추천해줘", "id01");

        Prompt prompt = chatModel.lastPrompt();
        ToolCallingChatOptions options = (ToolCallingChatOptions) prompt.getOptions();

        assertThat(result).isEqualTo("1. 청반바지: 50000, 바지, 빨강");
        assertThat(prompt.getSystemMessage().getText()).contains("userId를 기반으로 구매목록");
        assertThat(prompt.getUserMessage().getText()).isEqualTo("추천해줘");
        assertThat(options.getToolCallbacks())
                .extracting(toolCallback -> toolCallback.getToolDefinition().name())
                .contains("getCurrentDateTime", "setAlarm", "getCurrentWeather", "getForecastWeather", "getYesterdayWeather", "getOrderedByCustomer", "getContents");
        assertThat(options.getToolContext()).containsEntry("userId", "id01");
    }
}
