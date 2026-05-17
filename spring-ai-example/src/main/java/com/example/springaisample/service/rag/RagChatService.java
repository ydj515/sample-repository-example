package com.example.springaisample.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class RagChatService {

    private final ChatClient chatClient;

    //Constructor
    public RagChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        SimpleLoggerAdvisor customLogger = new SimpleLoggerAdvisor(
                request -> "[SimpleLoggerAdvisor] Custom request: " + request.prompt().getUserMessage(),
                response -> "[SimpleLoggerAdvisor] Custom response: " + response.getResult(),
                0);
        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(
                        SearchRequest.builder()
                                .topK(3)
                                .similarityThreshold(0.6)
                                .build())
                .order(Ordered.HIGHEST_PRECEDENCE)
                .build();
        this.chatClient = chatClientBuilder
                        .defaultAdvisors(questionAnswerAdvisor, customLogger)
                        .build();
    }

    public Flux<String> ragChat(String question, String type) {

        log.info("ragChat question={} type={}", question, type);
        return this.chatClient.prompt()
                .user(question)
                .advisors(a -> a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION, "type == '%s'".formatted(type)))
                .stream()
                .content();
    }
}
