package com.example.springaisample.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.springaisample.model.Question;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

class PromptTemplateResourceServiceTest {

    @Test
    void promptTemplate3RendersSystemAndUserMessagesFromResources() {
        CapturingChatModel chatModel = new CapturingChatModel("리소스 기반 응답");
        PromptTemplateResourceService service = new PromptTemplateResourceService(ChatClient.builder(chatModel));
        injectPromptResources(service);

        String result = service.promptTemplate3(new Question("서울 종로", "맛집", "영어"));

        Prompt prompt = chatModel.lastPrompt();
        assertThat(result).isEqualTo("리소스 기반 응답");
        assertThat(prompt.getSystemMessage().getText()).contains("영어");
        assertThat(prompt.getUserMessage().getText())
                .contains("서울 종로")
                .contains("맛집");
    }

    @Test
    void promptTemplate5BuildsSystemAndUserMessagesInOrder() {
        CapturingChatModel chatModel = new CapturingChatModel("메시지 조합 응답");
        PromptTemplateResourceService service = new PromptTemplateResourceService(ChatClient.builder(chatModel));
        injectPromptResources(service);

        String result = service.promptTemplate5(new Question("부산 해운대", "카페", "일본어"));

        Prompt prompt = chatModel.lastPrompt();
        assertThat(result).isEqualTo("메시지 조합 응답");
        assertThat(prompt.getInstructions()).hasSize(2);
        assertThat(prompt.getInstructions().get(0).getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(prompt.getInstructions().get(1).getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(prompt.getInstructions().get(0).getText()).contains("일본어");
        assertThat(prompt.getInstructions().get(1).getText())
                .contains("부산 해운대")
                .contains("카페");
    }

    private void injectPromptResources(PromptTemplateResourceService service) {
        ReflectionTestUtils.setField(
                service,
                "systemResource",
                new ClassPathResource("prompts/system-message-prompt-template.st")
        );
        ReflectionTestUtils.setField(
                service,
                "userResource",
                new ClassPathResource("prompts/user-message-prompt-template.st")
        );
    }
}
