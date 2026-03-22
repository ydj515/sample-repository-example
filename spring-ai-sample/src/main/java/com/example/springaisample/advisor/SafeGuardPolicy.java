package com.example.springaisample.advisor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.springframework.core.io.Resource;

public record SafeGuardPolicy(List<String> sensitiveWords, String blockedMessage) {

    public static final String DEFAULT_BLOCKED_MESSAGE = "사용자의 질문에 문제가 있는 단어가 있으면 시스템에 요청 할수 없습니다.";
    private static final List<String> DEFAULT_SENSITIVE_WORDS = List.of("스미싱", "무기", "비밀번호");

    public SafeGuardPolicy {
        sensitiveWords = List.copyOf(Objects.requireNonNull(sensitiveWords, "sensitiveWords must not be null"));
        blockedMessage = Objects.requireNonNullElse(blockedMessage, DEFAULT_BLOCKED_MESSAGE);
    }

    public static SafeGuardPolicy defaultPolicy() {
        return new SafeGuardPolicy(DEFAULT_SENSITIVE_WORDS, DEFAULT_BLOCKED_MESSAGE);
    }

    public static SafeGuardPolicy fromResource(Resource resource) {
        try {
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            List<String> words = content.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.startsWith("#"))
                    .toList();

            if (words.isEmpty()) {
                throw new IllegalStateException("SafeGuard 민감 단어 목록이 비어 있습니다.");
            }

            return new SafeGuardPolicy(words, DEFAULT_BLOCKED_MESSAGE);
        }
        catch (IOException exception) {
            throw new UncheckedIOException("SafeGuard 민감 단어 목록을 읽을 수 없습니다.", exception);
        }
    }
}
