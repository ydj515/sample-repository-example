package com.example.springaisample.service.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChatPgvectorService {

    private final ChatClient chatClient;

    private final PgVectorStore pgVectorStore;
    private final JdbcTemplate jdbcTemplate;

    // Constructor
    public ChatPgvectorService(ChatClient.Builder chatClientBuilder,
                                   JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        this.jdbcTemplate = jdbcTemplate;
        this.pgVectorStore = PgVectorStore.builder(this.jdbcTemplate, embeddingModel)
                .initializeSchema(false) // default
                .schemaName("public")
                // 반드시 Pgvector에 테이블 생성
                // /resources/schema 폴도에 있는 sql문이 서버 실행 시 기본적으로 테이블 생성
                .vectorTableName("chat_pgvector")
                .build();
        this.chatClient = chatClientBuilder
                .defaultAdvisors(VectorStoreChatMemoryAdvisor.builder(this.pgVectorStore).build())
                .build();
    }
    public String deleteChat(String conversationId){
        this.pgVectorStore.delete("conversationId == '%s'".formatted(conversationId));
        return "Delete Completed ";

    }
    public String deleteAllChat(){
        this.jdbcTemplate.execute("delete from chat_pgvector");
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
