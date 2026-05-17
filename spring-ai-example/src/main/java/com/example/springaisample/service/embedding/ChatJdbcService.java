package com.example.springaisample.service.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChatJdbcService {
    private final ChatClient chatClient;
    // SpringBoot 설정되어진 Database에 연동
    private final JdbcChatMemoryRepository jdbcChatMemoryRepository;

    // Constructor
    public ChatJdbcService(ChatClient.Builder chatClientBuilder,
                                   JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        this.jdbcChatMemoryRepository = jdbcChatMemoryRepository;
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(this.jdbcChatMemoryRepository)
                .maxMessages(30)
                .build();
        this.chatClient = chatClientBuilder
                //    MessageChatMemoryAdvisor: 메모리에서 대화 내역을 검색하여 메시지 모음으로 프롬프트에 포함합니다.
                //    PromptChatMemoryAdvisor: 메모리에서 대화 내역을 검색하여 시스템 프롬프트에 일반 텍스트로 추가합니다.
                //    VectorStoreChatMemoryAdvisor: 벡터 저장소에서 대화 내역을 검색하여 시스템 메시지에 일반 텍스트로 추가합니다.
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public String deleteChat(String conversationId){
        this.jdbcChatMemoryRepository.deleteByConversationId(conversationId);
        return "Delete Completed ";
    }

    public String deleteAllChat(){
        this.jdbcChatMemoryRepository.findConversationIds().forEach(this.jdbcChatMemoryRepository::deleteByConversationId);
        return "Delete All Completed ";
    }

    public String chat(String question, String conversationId) {
        return chatClient.prompt()
                .system("질문에 대한 답변을 한국어로 친절하게 답변해야 합니다.")
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(question)
                .call()
                .content();
    }
}
