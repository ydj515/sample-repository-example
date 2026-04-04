package com.example.springaisample.service.tool;

import com.example.springaisample.tool.time.DateTimeTools;
import com.example.springaisample.tool.weather.CurrentWeatherTools;
import com.example.springaisample.tool.weather.ForecastWeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TimeWeatherToolService {

    private final ChatClient chatClient;
    private final ChatOptions chatOptions;

    @Autowired
    public TimeWeatherToolService(OpenAiChatModel openAiChatModel) {
        this(ChatClient.builder(openAiChatModel));
    }

    // Constructor
    public TimeWeatherToolService(ChatClient.Builder chatClientBuilder) {
        ToolCallback[] toolCallbacks = ToolCallbacks.from(new DateTimeTools(), new CurrentWeatherTools(), new ForecastWeatherTools());

        this.chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbacks)
                .build();

        this.chatClient = chatClientBuilder
                .defaultOptions(chatOptions)
                .build();
    }

    // 1. Date Time
    public String chat(String question) {
        return chatClient.prompt()
                .system("질문에 대한 답변을 한국어로 친절하게 답변해야 합니다.")
                .user(question)
                .call()
                .content();
    }

    // 예시: Prompt 입력 가능
    public String chatWithOptions(String question) {
        return chatClient.prompt()
                .system("질문에 대한 답변을 한국어로 친절하게 답변해야 합니다.")
                .user(question)
                .options(this.chatOptions)
                .call()
                .content();
    }
}
