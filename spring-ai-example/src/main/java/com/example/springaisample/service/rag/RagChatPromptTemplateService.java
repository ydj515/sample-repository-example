package com.example.springaisample.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class RagChatPromptTemplateService {

    private final ChatClient chatClient;

    //Constructor
    public RagChatPromptTemplateService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        // Logger Advisor - yml파일에서 반드시 debug로 셋팅 해야 출력 됨
        SimpleLoggerAdvisor customLogger = new SimpleLoggerAdvisor(
                request -> "[SimpleLoggerAdvisor] Custom request: " + request.prompt().getUserMessage(),
                response -> "[SimpleLoggerAdvisor] Custom response: " + response.getResult(),
                0);
        PromptTemplate customPromptTemplate = PromptTemplate.builder()
                .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
                .template("""
                        <query>
                        답변 정보는 아래와 같습니다.
	                     --------------------
				            <question_answer_context>
				            ---------------------
            
                        답변 정보가 없는 경우, 질문에 답하세요.                
                        1. 답변 정보가 없는 경우  "죄송하지만 모릅니다!!"러고 말하세요.
                        전체적인 답변은 다음 규칙에 따라 답변해줘
                        1. "맥락에 따라..." 또는 "제공된 정보..." 또는 "주어진 정보..."와 같은 진술은 피하세요.
                        """)

                .build();

        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .promptTemplate(customPromptTemplate)
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
        log.info("ragChat Service: {} {}", question, type);
        return this.chatClient.prompt()
                .system("친절하게 한국어로 답변해줘.")
                .user(question)
                .advisors(a -> a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION, "type == '%s'".formatted(type)))
                .stream()
                .content();
    }
}
