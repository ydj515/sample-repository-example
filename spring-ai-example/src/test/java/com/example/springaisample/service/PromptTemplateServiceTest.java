package com.example.springaisample.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.springaisample.model.Question;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.Prompt;

class PromptTemplateServiceTest {

    @Test
    void promptTemplate3RendersTemplateVariablesIntoUserAndSystemMessages() {
        CapturingChatModel chatModel = new CapturingChatModel("문자열 템플릿 응답");
        PromptTemplateService service = new PromptTemplateService(ChatClient.builder(chatModel));

        String result = service.promptTemplate3(new Question("제주", "관광지", "영어"));

        Prompt prompt = chatModel.lastPrompt();
        assertThat(result).isEqualTo("문자열 템플릿 응답");
        assertThat(prompt.getUserMessage().getText())
                .contains("제주")
                .contains("관광지");
        assertThat(prompt.getSystemMessage().getText()).contains("영어");
    }

    @Test
    void promptTemplate5SupportsAngleBracketTemplateRenderer() {
        CapturingChatModel chatModel = new CapturingChatModel("대체 구분자 응답");
        PromptTemplateService service = new PromptTemplateService(ChatClient.builder(chatModel));

        String result = service.promptTemplate5(new Question("서울 성수", "베이커리", "한국어"));

        Prompt prompt = chatModel.lastPrompt();
        assertThat(result).isEqualTo("대체 구분자 응답");
        assertThat(prompt.getInstructions()).hasSize(2);
        assertThat(prompt.getInstructions().get(0).getMessageType()).isEqualTo(MessageType.USER);
        assertThat(prompt.getInstructions().get(1).getMessageType()).isEqualTo(MessageType.USER);
        assertThat(prompt.getInstructions().get(0).getText())
                .contains("서울 성수")
                .contains("베이커리")
                .doesNotContain("<location>")
                .doesNotContain("<content>");
        assertThat(prompt.getInstructions().get(1).getText())
                .contains("한국어")
                .doesNotContain("<language>");
    }
}
