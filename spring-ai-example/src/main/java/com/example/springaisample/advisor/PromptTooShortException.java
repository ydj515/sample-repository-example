package com.example.springaisample.advisor;

public class PromptTooShortException extends RuntimeException {

    public PromptTooShortException(String message) {
        super(message);
    }
}
