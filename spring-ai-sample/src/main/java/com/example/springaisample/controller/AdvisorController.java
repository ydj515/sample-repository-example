package com.example.springaisample.controller;

import com.example.springaisample.model.Contents;
import com.example.springaisample.model.Question;
import com.example.springaisample.service.AdvisorService;
import com.example.springaisample.service.AdvisorStructuredOutputService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/test/advisor")
@Slf4j
@RequiredArgsConstructor
// Chapter 3. Advisors Controller
public class AdvisorController {

    // 1. Advisor
    // 2. Advisor: Stream
    private final AdvisorService advisorService;
    // 3. Recursive Advisors
    private final AdvisorStructuredOutputService advisorStructuredOutputService;

    // 1. Advisor
    @GetMapping("/completion")
    public String chatCompletion(@RequestParam("prompt") String userPrompt) {
        log.info("advisor completion prompt={}", userPrompt);
        String response = advisorService.chat(userPrompt);
        log.info("advisor completion response={}", response);
        return response;
    }

    // 2. Advisor: Stream
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam("prompt") String userPrompt) {
        log.info("advisor stream prompt={}", userPrompt);
        return Flux.defer(() -> {
            StringBuilder responseBuilder = new StringBuilder();

            return advisorService.chatStream(userPrompt)
                    .doOnNext(chunk -> {
                        responseBuilder.append(chunk);
                        log.info("advisor stream response chunk={}", chunk);
                    })
                    .doOnComplete(() -> log.info("advisor stream response prompt={}, response={}", userPrompt, responseBuilder))
                    .doOnError(error -> log.error("advisor stream response error prompt={}", userPrompt, error));
        });
    }

    // 3. Recursive Advisors
    @GetMapping("/bean-output")
    public Contents beanOutput(@ModelAttribute Question question) {
        log.info("advisor bean-output question={}", question);
        Contents response = advisorStructuredOutputService.beanOutputConverter(question);
        log.info("advisor bean-output response={}", response);
        return response;
    }
}
