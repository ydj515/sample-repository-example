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
    //    - SimpleLoggerAdvisor: 개발자 console에 log를출력
    //    - SimpleLoggerAdvisorLow: 특정 파일에 질의 내용과 응답 내용만 log로 저장
    //    - SimpleLoggerAdvisorHigh: 특정 파일에 LLM과 연동 될때 사용되는 메시지를 log로 저장
    //    - SafeGuardAdvisor: 특정 문자가 입력 되면 진행을 중지
    //    - CheckCharSizeAdvisor: 입렫 되는 단어의 크기에 따라 진행을 중지
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
