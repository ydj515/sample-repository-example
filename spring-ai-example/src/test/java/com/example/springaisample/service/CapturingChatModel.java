package com.example.springaisample.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

final class CapturingChatModel implements ChatModel {

    private final Queue<String> scriptedResponses = new ArrayDeque<>();
    private final List<Prompt> capturedPrompts = new ArrayList<>();

    CapturingChatModel(String... scriptedResponses) {
        this.scriptedResponses.addAll(Arrays.asList(scriptedResponses));
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        this.capturedPrompts.add(prompt.copy());
        return createResponse(nextResponse());
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        this.capturedPrompts.add(prompt.copy());
        return Flux.just(createResponse(nextResponse()));
    }

    Prompt lastPrompt() {
        return this.capturedPrompts.get(this.capturedPrompts.size() - 1);
    }

    List<Prompt> prompts() {
        return this.capturedPrompts;
    }

    private String nextResponse() {
        if (this.scriptedResponses.isEmpty()) {
            throw new IllegalStateException("scripted response가 부족합니다.");
        }

        return this.scriptedResponses.remove();
    }

    private ChatResponse createResponse(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }
}
