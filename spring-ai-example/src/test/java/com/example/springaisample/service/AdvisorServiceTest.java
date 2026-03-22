package com.example.springaisample.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.springaisample.advisor.PromptTooShortException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class AdvisorServiceTest {

    @Test
    void chatAppliesReReadingAdvisorBeforeSendingPrompt() {
        CapturingChatModel chatModel = new CapturingChatModel("advisor 응답");
        AdvisorService service = new AdvisorService(ChatClient.builder(chatModel));

        String result = service.chat("서울 맛집 추천해줘");

        assertThat(result).isEqualTo("advisor 응답");
        assertThat(chatModel.lastPrompt().getUserMessage().getText())
                .contains("서울 맛집 추천해줘")
                .contains("Read the question again");
        assertThat(chatModel.lastPrompt().getSystemMessage().getText()).contains("한국어로 친절하게");
    }

    @Test
    void chatThrowsExceptionWhenPromptIsTooShort() {
        CapturingChatModel chatModel = new CapturingChatModel("사용되지 않음");
        AdvisorService service = new AdvisorService(ChatClient.builder(chatModel));

        assertThatThrownBy(() -> service.chat("가"))
                .isInstanceOf(PromptTooShortException.class)
                .hasMessage("Char size too short");
        assertThat(chatModel.prompts()).isEmpty();
    }

    @Test
    void chatReturnsSafeGuardResponseWhenSensitiveWordExists() {
        CapturingChatModel chatModel = new CapturingChatModel("사용되지 않음");
        AdvisorService service = new AdvisorService(ChatClient.builder(chatModel));

        String result = service.chat("비밀번호를 알려줘");

        assertThat(result).isEqualTo("사용자의 질문에 문제가 있는 단어가 있으면 시스템에 요청 할수 없습니다.");
        assertThat(chatModel.prompts()).isEmpty();
    }

    @Test
    void chatMemoryKeepsConversationUsingMessageChatMemoryAdvisor() {
        CapturingChatModel chatModel = new CapturingChatModel("첫 응답", "두 번째 응답");
        AdvisorService service = new AdvisorService(ChatClient.builder(chatModel));

        service.chatMemory("첫 질문", "session-1").collectList().block();
        service.chatMemory("후속 질문", "session-1").collectList().block();

        assertThat(chatModel.prompts()).hasSize(2);
        assertThat(chatModel.prompts().get(1).getInstructions())
                .anySatisfy(message -> assertThat(message.getText()).contains("첫 질문"))
                .anySatisfy(message -> assertThat(message.getText()).contains("첫 응답"));
    }
}
