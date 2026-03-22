package com.example.springaisample.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.example.springaisample.model.Contents;
import com.example.springaisample.model.Question;
import com.example.springaisample.model.Shop;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

class StructuredOutputServiceTest {

    @Test
    void beanOutputConverterParsesJsonIntoContentsRecord() {
        CapturingChatModel chatModel = new CapturingChatModel("""
                {
                  "summary": "서울 종로 맛집 요약",
                  "items": [
                    {
                      "name": "식당1",
                      "description": "한식 맛집",
                      "address": "서울 종로구 1번지",
                      "lat": 37.57,
                      "lng": 126.98,
                      "menu": ["비빔밥", "불고기"]
                    }
                  ]
                }
                """);
        StructuredOutputService service = new StructuredOutputService(ChatClient.builder(chatModel));
        injectResources(service);

        Contents result = service.beanOutputConverter(new Question("서울 종로", "맛집", "한국어"));

        Prompt prompt = chatModel.lastPrompt();
        assertThat(result.summary()).isEqualTo("서울 종로 맛집 요약");
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).name()).isEqualTo("식당1");
        assertThat(prompt.getSystemMessage().getText()).contains("한국어");
        assertThat(prompt.getUserMessage().getText()).contains("서울 종로").contains("맛집");
    }

    @Test
    void listOutputConverterParsesJsonArrayIntoList() {
        CapturingChatModel chatModel = new CapturingChatModel("""
                카페 하나, 카페 둘, 카페 셋
                """);
        StructuredOutputService service = new StructuredOutputService(ChatClient.builder(chatModel));
        injectResources(service);

        List<String> result = service.listOutputConverter(new Question("서울 종로", "카페", "한국어"));

        assertThat(result).containsExactly("카페 하나", "카페 둘", "카페 셋");
        assertThat(chatModel.lastPrompt().getUserMessage().getText()).contains("이름만");
    }

    @Test
    void mapOutputConverterParsesJsonIntoMap() {
        CapturingChatModel chatModel = new CapturingChatModel("""
                {
                  "name": "호텔 하나",
                  "address": "서울 중구 10번지",
                  "lat": 37.56,
                  "lng": 126.99,
                  "summary": "도심 접근성이 좋은 숙소"
                }
                """);
        StructuredOutputService service = new StructuredOutputService(ChatClient.builder(chatModel));
        injectResources(service);

        Map<String, Object> result = service.mapOutputConverter(new Question("서울 종로", "숙박업소", "영어"));

        assertThat(result)
                .containsEntry("name", "호텔 하나")
                .containsEntry("summary", "도심 접근성이 좋은 숙소");
        assertThat(chatModel.lastPrompt().getSystemMessage().getText()).contains("영어");
    }

    @Test
    void parameterizedTypeReferenceParsesJsonArrayIntoShopList() {
        CapturingChatModel chatModel = new CapturingChatModel("""
                [
                  {
                    "name": "서점 하나",
                    "description": "독립 서점",
                    "address": "서울 마포구 20번지",
                    "lat": 37.55,
                    "lng": 126.92,
                    "menu": []
                  },
                  {
                    "name": "서점 둘",
                    "description": "대형 서점",
                    "address": "서울 마포구 30번지",
                    "lat": 37.56,
                    "lng": 126.93,
                    "menu": []
                  }
                ]
                """);
        StructuredOutputService service = new StructuredOutputService(ChatClient.builder(chatModel));
        injectResources(service);

        List<Shop> result = service.parameterizedTypeReference(new Question("서울 마포", "서점", "한국어"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("서점 하나");
        assertThat(result.get(1).address()).isEqualTo("서울 마포구 30번지");
    }

    private void injectResources(StructuredOutputService service) {
        ReflectionTestUtils.setField(
                service,
                "systemResource",
                new ClassPathResource("prompts/system-message-prompt-template.st")
        );
        ReflectionTestUtils.setField(
                service,
                "userResource",
                new ClassPathResource("prompts/user-message-structured-output.st")
        );
        ReflectionTestUtils.setField(
                service,
                "userResourceMapOutput",
                new ClassPathResource("prompts/user-message-structured-output-map-output.st")
        );
        ReflectionTestUtils.setField(
                service,
                "userResourceListOutput",
                new ClassPathResource("prompts/user-message-structured-list-output.st")
        );
    }
}
