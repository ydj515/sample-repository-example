package com.example.springaisample.service;

import com.example.springaisample.advisor.CheckCharSizeAdvisor;
import com.example.springaisample.advisor.ReReadingAdvisor;
import com.example.springaisample.advisor.SafeGuardPolicy;
import com.example.springaisample.advisor.SimpleLoggerAdvisorHigh;
import com.example.springaisample.advisor.SimpleLoggerAdvisorLow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class AdvisorService {

    private final ChatClient chatClient;
    private final ChatClient chatClientMemory;

    AdvisorService(ChatClient.Builder chatClientBuilder) {
        this(chatClientBuilder, SafeGuardPolicy.defaultPolicy());
    }

    // Constructor
    @Autowired
    public AdvisorService(ChatClient.Builder chatClientBuilder, SafeGuardPolicy safeGuardPolicy) {
        // Logger Advisor - yml파일에서 반드시 debug로 셋팅 해야 출력 됨
        SimpleLoggerAdvisor customLogger = new SimpleLoggerAdvisor(
                request -> "[SimpleLoggerAdvisor] Custom request: " + request.prompt().getUserMessage(),
                response -> "[SimpleLoggerAdvisor] Custom response: " + response.getResult(),
                Ordered.HIGHEST_PRECEDENCE
        );
        // 입력되는 내용에 이상 문자 감지 Adviser
        SafeGuardAdvisor safeGuardAdvisor = new SafeGuardAdvisor(
                safeGuardPolicy.sensitiveWords(),
                safeGuardPolicy.blockedMessage(),
                Ordered.HIGHEST_PRECEDENCE
        );

        this.chatClient = chatClientBuilder.clone()
                // Add Advisor
                .defaultAdvisors(customLogger, new CheckCharSizeAdvisor(), safeGuardAdvisor)
                .build();

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(6)
                .build();
        this.chatClientMemory = chatClientBuilder.clone()
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public String chat(String question) {
        return chatClient.prompt()
                //.advisors(new SimpleLoggerAdvisorLow(), new SimpleLoggerAdvisorHigh())
                .advisors(new ReReadingAdvisor())
                .system("질문에 대한 답변을 한국어로 친절하게 답변해야 합니다.")
                .user(question)
                .call()
                .content();
    }

    public Flux<String> chatStream(String question) {
        return chatClient.prompt()
                .advisors(new SimpleLoggerAdvisorLow(), new SimpleLoggerAdvisorHigh())
                .system("질문에 대한 답변을 한국어로 친절하게 답변해야 합니다.")
                .user(question)
                .stream()
                .content();
    }

    public Flux<String> chatMemory(String question, String conversationId) {
        return chatClientMemory.prompt()
                .system("질문에 대한 답변을 한국어로 친절하게 답변해야 합니다.")
                .user(question)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}
