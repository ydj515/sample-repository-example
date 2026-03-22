package com.example.springaisample.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

// Question을 LLM에 전달하기 이전에 동작, Stream 또는 일반 Text 처리 시 각각 호출
// 전송 되어지는 내용과 LLM에서 전달 하는 답변 내용에 대한 Log 처리
// 질문과 답변 내용에 대한 전체 내용을 Log로 출력
// logback.xml에 선언된 내용에 따라 화면에도 출력 되며 Log 파일로도 생성
@Slf4j
public class SimpleLoggerAdvisorHigh implements CallAdvisor, StreamAdvisor {

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        logRequest(chatClientRequest);
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        logResponse(chatClientResponse);
        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
            ChatClientRequest chatClientRequest,
            StreamAdvisorChain streamAdvisorChain
    ) {
        logRequest(chatClientRequest);
        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);
        return new ChatClientMessageAggregator().aggregateChatClientResponse(chatClientResponses, this::logResponse);
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private void logRequest(ChatClientRequest request) {
        log.info("SimpleLoggerAdvisorHigh request: {}", request);
    }

    private void logResponse(ChatClientResponse chatClientResponse) {
        log.info("SimpleLoggerAdvisorHigh response: {}", chatClientResponse);
    }
}
