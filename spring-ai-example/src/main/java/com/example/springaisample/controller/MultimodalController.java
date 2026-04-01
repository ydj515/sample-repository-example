package com.example.springaisample.controller;

import java.io.IOException;

import com.example.springaisample.service.MultimodalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/test/multimodal")
// Multimodality API – Images & Vision Controller
public class MultimodalController {

    private final MultimodalService multimodalService;

    // 1. Generate Image for URL
    @GetMapping("/generate-image-url")
    public String generateImageUrl(@RequestParam("prompt") String prompt) {
        log.info("multimodal generate-image-url prompt={}", prompt);
        String response = multimodalService.generateImageUrl(prompt);
        log.info("multimodal generate-image-url response={}", response);
        return response;
    }

    // 2. Generate Image
    @GetMapping("/generate-image")
    public String generateImage(@RequestParam("prompt") String prompt) {
        log.info("multimodal generate-image prompt={}", prompt);
        String response = multimodalService.generateImageBase64(prompt);
        log.info("multimodal generate-image response-length={}", response.length());
        return response;
    }

    // 3. Image Analysis
    @PostMapping(value = "/image-analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> imageAnalysis(
            @RequestParam("question") String question,
            @RequestParam(value = "attach", required = false) MultipartFile attach
    ) throws IOException {
        if (attach == null || attach.isEmpty() || attach.getContentType() == null || !attach.getContentType().startsWith("image/")) {
            log.info("multimodal image-analysis invalid attach question={}", question);
            return Flux.just("이미지를 올려주세요.");
        }

        String contentType = attach.getContentType();
        byte[] bytes = attach.getBytes();
        log.info("multimodal image-analysis question={}, contentType={}, size={}", question, contentType, bytes.length);

        return Flux.defer(() -> {
            StringBuilder responseBuilder = new StringBuilder();

            return multimodalService.analyzeImage(question, contentType, bytes)
                    .doOnNext(chunk -> {
                        responseBuilder.append(chunk);
                        log.info("multimodal image-analysis response chunk={}", chunk);
                    })
                    .doOnComplete(() -> log.info("multimodal image-analysis response question={}, response={}", question, responseBuilder))
                    .doOnError(error -> log.error("multimodal image-analysis response error question={}", question, error));
        });
    }
}
