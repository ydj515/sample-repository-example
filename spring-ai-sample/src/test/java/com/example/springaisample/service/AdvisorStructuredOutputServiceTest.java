package com.example.springaisample.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.springaisample.model.Contents;
import com.example.springaisample.model.Question;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

class AdvisorStructuredOutputServiceTest {

    @Test
    void beanOutputConverterRetriesWhenStructuredOutputIsInvalid() {
        CapturingChatModel chatModel = new CapturingChatModel(
                """
                {"summary":"형식은 맞지만 items가 문자열입니다","items":"잘못된 값"}
                """,
                """
                {
                  "summary": "검증을 통과한 응답",
                  "items": [
                    {
                      "name": "식당 하나",
                      "description": "한식",
                      "address": "서울 종로구 1번지",
                      "lat": 37.57,
                      "lng": 126.98,
                      "menu": ["비빔밥"]
                    }
                  ]
                }
                """
        );
        AdvisorStructuredOutputService service = new AdvisorStructuredOutputService(ChatClient.builder(chatModel));
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

        Contents result = service.beanOutputConverter(new Question("서울 종로", "맛집", "한국어"));

        assertThat(result.summary()).isEqualTo("검증을 통과한 응답");
        assertThat(result.items()).hasSize(1);
        assertThat(chatModel.prompts()).hasSize(2);
    }
}
