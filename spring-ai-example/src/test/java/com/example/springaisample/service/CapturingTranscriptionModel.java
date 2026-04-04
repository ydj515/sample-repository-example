package com.example.springaisample.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;

final class CapturingTranscriptionModel implements TranscriptionModel {

    private final Queue<AudioTranscriptionResponse> scriptedResponses = new ArrayDeque<>();
    private final List<AudioTranscriptionPrompt> capturedPrompts = new ArrayList<>();

    CapturingTranscriptionModel(AudioTranscriptionResponse... scriptedResponses) {
        this.scriptedResponses.addAll(Arrays.asList(scriptedResponses));
    }

    @Override
    public AudioTranscriptionResponse call(AudioTranscriptionPrompt prompt) {
        this.capturedPrompts.add(prompt);
        return nextResponse();
    }

    AudioTranscriptionPrompt lastPrompt() {
        return this.capturedPrompts.get(this.capturedPrompts.size() - 1);
    }

    private AudioTranscriptionResponse nextResponse() {
        if (this.scriptedResponses.isEmpty()) {
            throw new IllegalStateException("scripted transcription response가 부족합니다.");
        }

        return this.scriptedResponses.remove();
    }
}
