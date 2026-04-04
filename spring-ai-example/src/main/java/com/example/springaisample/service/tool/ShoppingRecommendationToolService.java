package com.example.springaisample.service.tool;

import java.util.Map;

import com.example.springaisample.tool.shopping.ShoppingTools;
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
public class ShoppingRecommendationToolService {

    private final ChatClient chatClient;

    @Autowired
    public ShoppingRecommendationToolService(OpenAiChatModel openAiChatModel) {
        this(ChatClient.builder(openAiChatModel));
    }

    // Constructor
    public ShoppingRecommendationToolService(ChatClient.Builder chatClientBuilder) {
        ToolCallback[] toolCallbacks = ToolCallbacks.from(
                new DateTimeTools(),
                new CurrentWeatherTools(),
                new ForecastWeatherTools(),
                new ShoppingTools()
        );

        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbacks)
                .build();

        this.chatClient = chatClientBuilder
                .defaultOptions(chatOptions)
                .build();
    }

    // 3. Recommendation
    public String getRecommendations(String question, String userId) {
        return chatClient.prompt()
                .system("""
                        userId를 기반으로 구매목록을 이용하여 질문에 대해서 답변해줘.
                        주로 구매한 카테고리를 질문 하면 카테고리 정보만 문자로 답변해줘.
                        고객이 주로 주문한 카테고리와 고객이 주문한 총 주문금액의 평균가격보다 낮은 제품을 고객에게 추천해줘.
                        고객이 선호 하거나 좋아 할 만한 제품은 다음과 같은 형식으로만 출력해줘
                        예시)
                        1. 제품명: 가격, 카테고리, 색상
                        ...
                        """)
                .user(question)
                .toolContext(Map.of("userId", userId))
                .call()
                .content();
    }
}
