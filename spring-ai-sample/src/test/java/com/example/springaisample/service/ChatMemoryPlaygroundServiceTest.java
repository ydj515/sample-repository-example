package com.example.springaisample.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;

class ChatMemoryPlaygroundServiceTest {

    @Test
    void chatMemoryStoresConversationAndAddsItToLaterPrompt() {
        CapturingChatModel chatModel = new CapturingChatModel("첫 응답", "두 번째 응답");
        ChatMemoryPlaygroundService service = new ChatMemoryPlaygroundService(
                ChatClient.builder(chatModel),
                3
        );

        List<String> firstResponse = service.chatMemory("첫 질문", "session-1").collectList().block();
        List<String> secondResponse = service.chatMemory("후속 질문", "session-1").collectList().block();

        Prompt secondPrompt = chatModel.prompts().get(1);
        assertThat(firstResponse).containsExactly("첫 응답");
        assertThat(secondResponse).containsExactly("두 번째 응답");
        assertThat(secondPrompt.getUserMessage().getText()).isEqualTo("후속 질문");
        assertThat(secondPrompt.getSystemMessage().getText())
                .contains("첫 질문")
                .contains("첫 응답");
    }

    @Test
    void chatMemoryKeepsRecentConversationPairsBasedOnMaxTurns() {
        CapturingChatModel chatModel = new CapturingChatModel("답변1", "답변2", "답변3", "답변4");
        ChatMemoryPlaygroundService service = new ChatMemoryPlaygroundService(
                ChatClient.builder(chatModel),
                2
        );

        service.chatMemory("질문1", "session-2").collectList().block();
        service.chatMemory("질문2", "session-2").collectList().block();
        service.chatMemory("질문3", "session-2").collectList().block();
        service.chatMemory("질문4", "session-2").collectList().block();

        Prompt fourthPrompt = chatModel.prompts().get(3);
        assertThat(fourthPrompt.getUserMessage().getText()).isEqualTo("질문4");
        assertThat(fourthPrompt.getSystemMessage().getText())
                .contains("질문2")
                .contains("답변2")
                .contains("질문3")
                .contains("답변3")
                .doesNotContain("질문1")
                .doesNotContain("답변1");
    }

    @Test
    void chatMemoryDoesNotAttachHistoryWhenMaxTurnsIsZero() {
        CapturingChatModel chatModel = new CapturingChatModel("첫 응답", "두 번째 응답");
        ChatMemoryPlaygroundService service = new ChatMemoryPlaygroundService(
                ChatClient.builder(chatModel),
                0
        );

        service.chatMemory("질문1", "session-3").collectList().block();
        service.chatMemory("질문2", "session-3").collectList().block();

        Prompt secondPrompt = chatModel.prompts().get(1);
        assertThat(secondPrompt.getUserMessage().getText()).isEqualTo("질문2");
        assertThat(secondPrompt.getSystemMessages()).isEmpty();
    }
}
