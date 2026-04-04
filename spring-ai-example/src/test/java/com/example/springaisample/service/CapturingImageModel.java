package com.example.springaisample.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;

final class CapturingImageModel implements ImageModel {

    private final Queue<ImageResponse> scriptedResponses = new ArrayDeque<>();
    private final List<ImagePrompt> capturedPrompts = new ArrayList<>();

    CapturingImageModel(ImageResponse... scriptedResponses) {
        this.scriptedResponses.addAll(Arrays.asList(scriptedResponses));
    }

    @Override
    public ImageResponse call(ImagePrompt prompt) {
        this.capturedPrompts.add(prompt);
        return nextResponse();
    }

    ImagePrompt lastPrompt() {
        return this.capturedPrompts.get(this.capturedPrompts.size() - 1);
    }

    private ImageResponse nextResponse() {
        if (this.scriptedResponses.isEmpty()) {
            throw new IllegalStateException("scripted image response가 부족합니다.");
        }

        return this.scriptedResponses.remove();
    }
}
