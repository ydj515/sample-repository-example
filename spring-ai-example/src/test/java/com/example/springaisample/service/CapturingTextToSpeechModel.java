package com.example.springaisample.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import reactor.core.publisher.Flux;

final class CapturingTextToSpeechModel implements TextToSpeechModel {

    private final Queue<TextToSpeechResponse> scriptedCallResponses = new ArrayDeque<>();
    private final Queue<List<TextToSpeechResponse>> scriptedStreamResponses = new ArrayDeque<>();
    private final List<TextToSpeechPrompt> capturedCallPrompts = new ArrayList<>();
    private final List<TextToSpeechPrompt> capturedStreamPrompts = new ArrayList<>();

    CapturingTextToSpeechModel(TextToSpeechResponse... scriptedCallResponses) {
        this.scriptedCallResponses.addAll(Arrays.asList(scriptedCallResponses));
    }

    void enqueueStreamResponses(TextToSpeechResponse... responses) {
        this.scriptedStreamResponses.add(List.of(responses));
    }

    @Override
    public TextToSpeechResponse call(TextToSpeechPrompt prompt) {
        this.capturedCallPrompts.add(prompt);
        return nextCallResponse();
    }

    @Override
    public Flux<TextToSpeechResponse> stream(TextToSpeechPrompt prompt) {
        this.capturedStreamPrompts.add(prompt);
        return Flux.fromIterable(nextStreamResponse());
    }

    TextToSpeechPrompt lastCallPrompt() {
        return this.capturedCallPrompts.get(this.capturedCallPrompts.size() - 1);
    }

    TextToSpeechPrompt lastStreamPrompt() {
        return this.capturedStreamPrompts.get(this.capturedStreamPrompts.size() - 1);
    }

    private TextToSpeechResponse nextCallResponse() {
        if (this.scriptedCallResponses.isEmpty()) {
            throw new IllegalStateException("scripted speech response가 부족합니다.");
        }

        return this.scriptedCallResponses.remove();
    }

    private List<TextToSpeechResponse> nextStreamResponse() {
        if (this.scriptedStreamResponses.isEmpty()) {
            throw new IllegalStateException("scripted speech stream response가 부족합니다.");
        }

        return this.scriptedStreamResponses.remove();
    }
}
