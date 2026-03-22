package com.example.springaisample.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;

class ChatPlaygroundServiceTest {

    @Test
    void chatUsesFriendlyKoreanSystemPrompt() {
        CapturingChatModel chatModel = new CapturingChatModel("기본 응답");
        ChatPlaygroundService service = new ChatPlaygroundService(ChatClient.builder(chatModel));

        String result = service.chat("안녕하세요");

        Prompt prompt = chatModel.lastPrompt();
        assertThat(result).isEqualTo("기본 응답");
        assertThat(prompt.getSystemMessage().getText()).contains("한국어로 친절하게 답변");
        assertThat(prompt.getUserMessage().getText()).isEqualTo("안녕하세요");
    }

    @Test
    void chatStreamUsesFriendlyKoreanSystemPrompt() {
        CapturingChatModel chatModel = new CapturingChatModel("스트림 응답");
        ChatPlaygroundService service = new ChatPlaygroundService(ChatClient.builder(chatModel));

        List<String> result = service.chatStream("스트림 테스트").collectList().block();

        Prompt prompt = chatModel.lastPrompt();
        assertThat(result).containsExactly("스트림 응답");
        assertThat(prompt.getSystemMessage().getText()).contains("한국어로 친절하게 답변");
        assertThat(prompt.getUserMessage().getText()).isEqualTo("스트림 테스트");
    }

    @Test
    void chatChainOfThoughtBuildsExampleDrivenPrompt() {
        CapturingChatModel chatModel = new CapturingChatModel("chain 응답");
        ChatPlaygroundService service = new ChatPlaygroundService(ChatClient.builder(chatModel));

        List<String> result = service.chatChainOfThought("정렬 알고리즘 추천해줘").collectList().block();

        Prompt prompt = chatModel.lastPrompt();
        assertThat(result).containsExactly("chain 응답");
        assertThat(prompt.getUserMessage().getText())
                .contains("정렬 알고리즘 추천해줘")
                .contains("위의 질문을 단계별로 해결해 봅시다")
                .contains("[예시]")
                .contains("1단계")
                .contains("총 비용");
    }

    @Test
    void chatFewShotBuildsJsonInstructionPrompt() {
        CapturingChatModel chatModel = new CapturingChatModel("few-shot 응답");
        ChatPlaygroundService service = new ChatPlaygroundService(ChatClient.builder(chatModel));

        String result = service.chatFewShot("서울 종로 맛집 알려줘");

        Prompt prompt = chatModel.lastPrompt();
        assertThat(result).isEqualTo("few-shot 응답");
        assertThat(prompt.getUserMessage().getText())
                .contains("JSON 형식")
                .contains("10개의 정보를 조회")
                .contains("예시1")
                .contains("예시2")
                .contains("고객 주문: 서울 종로 맛집 알려줘");
    }
}
