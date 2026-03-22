package com.example.springaisample.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatMemoryPlaygroundService {

    private final ChatClient chatClientMemory;

    public ChatMemoryPlaygroundService(
            ChatClient.Builder chatClientBuilder,
            @Value("${app.chat.memory.max-turns:3}") int maxTurns
    ) {
        ChatClient.Builder builder = chatClientBuilder.clone();

        if (maxTurns > 0) {
            ChatMemory chatMemory = MessageWindowChatMemory.builder()
                    .maxMessages(maxTurns * 2)
                    .build();

            // 사용 가능한 Chat Memory Advisor 예시
            // - MessageChatMemoryAdvisor:
            //   메모리에 저장된 대화 내역을 조회해서 메시지 목록 형태로 프롬프트에 포함합니다.
            // - PromptChatMemoryAdvisor:
            //   메모리에 저장된 대화 내역을 조회해서 시스템 프롬프트에 일반 텍스트 형태로 추가합니다.
            // - VectorStoreChatMemoryAdvisor:
            //   벡터 저장소에서 관련 대화 내역을 조회해서 시스템 프롬프트에 일반 텍스트 형태로 추가합니다.
            //
            // 현재 예제에서는 이전 대화를 시스템 프롬프트에 자연스럽게 녹여 넣기 위해
            // PromptChatMemoryAdvisor를 사용합니다.
            builder.defaultAdvisors(PromptChatMemoryAdvisor.builder(chatMemory).build());
        }

        this.chatClientMemory = builder.build();
    }

    public Flux<String> chatMemory(String question, String conversationId) {
        return this.chatClientMemory.prompt()
                .user(question)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}
