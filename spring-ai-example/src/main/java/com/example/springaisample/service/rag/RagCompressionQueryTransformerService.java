package com.example.springaisample.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class RagCompressionQueryTransformerService {

    private final Advisor retrievalAugmentationAdvisor;
    private final ChatClient chatClientMemory;

    // Constructor
    public RagCompressionQueryTransformerService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        // Logger Advisor - yml파일에서 반드시 debug로 셋팅 해야 출력 됨
        SimpleLoggerAdvisor customLogger = new SimpleLoggerAdvisor(
                request -> "[SimpleLoggerAdvisor] Custom request: " + request.prompt().getUserMessage(),
                response -> "[SimpleLoggerAdvisor] Custom response: " + response.getResult(),
                Ordered.HIGHEST_PRECEDENCE);

        retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(CompressionQueryTransformer.builder().chatClientBuilder(chatClientBuilder).build())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .topK(5)
                        .similarityThreshold(0.6)
                        .vectorStore(vectorStore)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();
        this.chatClientMemory = chatClientBuilder
                .defaultAdvisors(
                        //    MessageChatMemoryAdvisor: 메모리에서 대화 내역을 검색하여 메시지 모음으로 프롬프트에 포함합니다.
                        //    PromptChatMemoryAdvisor: 메모리에서 대화 내역을 검색하여 시스템 프롬프트에 일반 텍스트로 추가합니다.
                        //    VectorStoreChatMemoryAdvisor: 벡터 저장소에서 대화 내역을 검색하여 시스템 메시지에 일반 텍스트로 추가합니다.
                        //MessageChatMemoryAdvisor.builder(chatMemory).build())
                        PromptChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public Flux<String> ragChat(String question, String type, String conversationId) {

        return this.chatClientMemory.prompt()
                .system("친절하게 한국어로 답변해줘.")
                .user(question)
                .advisors(advisorSpec -> advisorSpec.param(
                        ChatMemory.CONVERSATION_ID, conversationId
                ))
                .advisors(retrievalAugmentationAdvisor)
                .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "type == '%s'".formatted(type)))
                .stream()
                .content();
    }
}
