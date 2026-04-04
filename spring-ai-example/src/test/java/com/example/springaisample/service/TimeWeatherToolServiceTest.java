package com.example.springaisample.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.springaisample.service.tool.TimeWeatherToolService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

class TimeWeatherToolServiceTest {

    @Test
    void chatUsesFriendlyPromptAndRegistersToolCallbacks() {
        CapturingChatModel chatModel = new CapturingChatModel("현재 시간입니다.");
        TimeWeatherToolService service = new TimeWeatherToolService(ChatClient.builder(chatModel));

        String result = service.chat("지금 시간 알려줘");

        Prompt prompt = chatModel.lastPrompt();

        assertThat(result).isEqualTo("현재 시간입니다.");
        assertThat(prompt.getSystemMessage().getText()).contains("한국어로 친절하게");
        assertThat(prompt.getUserMessage().getText()).isEqualTo("지금 시간 알려줘");
        assertThat(prompt.getOptions()).isInstanceOf(ToolCallingChatOptions.class);
        assertThat(((ToolCallingChatOptions) prompt.getOptions()).getToolCallbacks())
                .extracting(toolCallback -> toolCallback.getToolDefinition().name())
                .contains("getCurrentDateTime", "setAlarm", "getCurrentWeather", "getForecastWeather", "getYesterdayWeather");
    }
}
