package com.example.springaisample.service.mcp;

import com.example.springaisample.tool.time.DateTimeTools;
import com.example.springaisample.tool.weather.CurrentWeatherTools;
import com.example.springaisample.tool.weather.ForecastWeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class McpWeatherService {

    private final ChatClient chatClient;

    // @param toolCallbackProvider LLM이 Function Calling을 수행할 때 참고할 수 있도록 도구(Tool)들을 묶어서 제공합니다.
    //
    // [MCP 환경에서 주입되는 경우 (Bean이 존재함)]
    // 프로젝트와 연결된 원격/외부 MCP 서버(stdio, sse 방식으로 연결된 서버들)가 제공하는 도구 목록들이 담깁니다.
    // Spring AI의 자동 구성(Auto-Configuration) 기동 시 연결된 외부 MCP 서버(application.yaml의 spring.ai.mcp.client에 정의) 통신을 통해 도구 목록을 동적으로 조회하고, 이를 Spring AI 스펙에 맞는 ToolCallback 객체들로 감싸 컨텍스트에 글로벌 Provider 빈으로 등록합니다.
    //
    // [Fallback 환경으로 들어가는 경우 (Bean이 없음)]
    // 외부 MCP 연동이 꺼져 있어 빈이 등록되지 않았다면, 코드의 toolCallbackProvider.getIfAvailable(McpWeatherService::fallbackToolCallbackProvider) 부분에 의해 로컬에 정의된 메서드가 동작합니다.
    // 이 경우 로컬 자바 클래스로 구현된 DateTimeTools, CurrentWeatherTools, ForecastWeatherTools 객체들이 Spring AI 도구 규격으로 변환되어 들어갑니다.
    @Autowired
    public McpWeatherService(OpenAiChatModel openAiChatModel, ObjectProvider<ToolCallbackProvider> toolCallbackProvider) {
        this(ChatClient.builder(openAiChatModel), toolCallbackProvider.getIfAvailable(McpWeatherService::fallbackToolCallbackProvider));
    }

    // Constructor
    public McpWeatherService(ChatClient.Builder chatClientBuilder, ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    // 1. MCP Chat
    public String chat(String question) {
        return chatClient.prompt()
                .system("질문에 대한 답변을 한국어로 친절하게 답변해야 합니다.")
                .user(question)
                .call()
                .content();
    }

    // MCP 서버가 없더라도 도구를 이용해 동일한 질문 흐름을 유지
    private static ToolCallbackProvider fallbackToolCallbackProvider() {
        return ToolCallbackProvider.from(ToolCallbacks.from(new DateTimeTools(), new CurrentWeatherTools(), new ForecastWeatherTools()));
    }
}
