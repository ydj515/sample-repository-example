package com.example.springaisample.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class RagMultiQueryExpanderService {

    private final Advisor retrievalAugmentationAdvisor;
    private final ChatClient chatClient;

    public RagMultiQueryExpanderService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        // Logger Advisor - yml파일에서 반드시 debug로 셋팅 해야 출력 됨
        SimpleLoggerAdvisor customLogger = new SimpleLoggerAdvisor(
                request -> "[SimpleLoggerAdvisor] Custom request: " + request.prompt().getUserMessage(),
                response -> "[SimpleLoggerAdvisor] Custom response: " + response.getResult(),
                Ordered.HIGHEST_PRECEDENCE);

        retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryExpander(MultiQueryExpander.builder()
                        .chatClientBuilder(chatClientBuilder)
                        // 사용자가 입력한 Question을 같이 사용할지 결정
                        //.includeOriginal(false)
                        .numberOfQueries(3)
                        .build())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .topK(5)
                        .similarityThreshold(0.6)
                        .vectorStore(vectorStore)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
        this.chatClient = chatClientBuilder
                .defaultAdvisors(customLogger)
                .build();
    }

    public Flux<String> ragChat(String question, String type, String conversationId) {
        return this.chatClient.prompt()
                .system("친절하게 한국어로 답변해줘.")
                .user(question)
                .advisors(retrievalAugmentationAdvisor)
                .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "type == '%s'".formatted(type)))
                .stream()
                .content();
    }
}
