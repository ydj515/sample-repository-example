package com.example.springaisample.controller;

import com.example.springaisample.service.ChatMemoryPlaygroundService;
import com.example.springaisample.service.ChatPlaygroundService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/test/chat")
public class ChatPlaygroundController {

    private final ChatPlaygroundService chatPlaygroundService;
    private final ChatMemoryPlaygroundService chatMemoryPlaygroundService;

    @GetMapping("/completion")
    public String chatCompletion(@RequestParam("prompt") String userPrompt) {
        log.info("completion prompt={}", userPrompt);
        String response = chatPlaygroundService.chat(userPrompt);
        log.info("completion response={}", response);
        return response;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam("prompt") String userPrompt) {
        log.info("stream prompt={}", userPrompt);
        return logStreamResponse("stream", userPrompt, chatPlaygroundService.chatStream(userPrompt));
    }

    @GetMapping(value = "/chain", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatChainOfThought(@RequestParam("prompt") String userPrompt) {
        log.info("chain prompt={}", userPrompt);
        return logStreamResponse("chain", userPrompt, chatPlaygroundService.chatChainOfThought(userPrompt));
    }

    @GetMapping("/few")
    public String chatFewShot(@RequestParam("prompt") String userPrompt) {
        log.info("few prompt={}", userPrompt);
        String response = chatPlaygroundService.chatFewShot(userPrompt);
        log.info("few response={}", response);
        return response;
    }

    @GetMapping(value = "/memory", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatMemory(@RequestParam("prompt") String userPrompt, HttpSession session) {
        log.info("memory prompt={}, sessionId={}", userPrompt, session.getId());
        return logStreamResponse("memory", userPrompt, chatMemoryPlaygroundService.chatMemory(userPrompt, session.getId()));
    }

    private Flux<String> logStreamResponse(String endpoint, String userPrompt, Flux<String> responseFlux) {
        return Flux.defer(() -> {
            StringBuilder responseBuilder = new StringBuilder();

            return responseFlux
                    .doOnNext(chunk -> {
                        responseBuilder.append(chunk);
                        log.info("{} response chunk={}", endpoint, chunk);
                    })
                    .doOnComplete(() -> log.info("{} response prompt={}, response={}", endpoint, userPrompt, responseBuilder))
                    .doOnError(error -> log.error("{} response error prompt={}", endpoint, userPrompt, error));
        });
    }
}
