package com.example.springaisample.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.springaisample.service.mcp.McpWeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;

class McpWeatherServiceTest {

    @Test
    void chatUsesFriendlyPromptAndFallbackTools() {
        CapturingChatModel chatModel = new CapturingChatModel("서울은 맑고 12도입니다.");
        McpWeatherService service = new McpWeatherService(ChatClient.builder(chatModel), ToolCallbackProvider.from());

        String result = service.chat("오늘 날씨 알려줘");

        Prompt prompt = chatModel.lastPrompt();
        ToolCallingChatOptions options = (ToolCallingChatOptions) prompt.getOptions();

        assertThat(result).isEqualTo("서울은 맑고 12도입니다.");
        assertThat(prompt.getSystemMessage().getText()).contains("한국어로 친절하게");
        assertThat(prompt.getUserMessage().getText()).isEqualTo("오늘 날씨 알려줘");
        assertThat(options.getToolCallbacks()).isEmpty();
    }
}
