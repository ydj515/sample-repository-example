package com.example.springaisample.controller;

import java.util.List;
import java.util.Map;

import com.example.springaisample.model.Contents;
import com.example.springaisample.model.Question;
import com.example.springaisample.model.Shop;
import com.example.springaisample.service.PromptTemplateResourceService;
import com.example.springaisample.service.StructuredOutputService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/test/prompt")
public class PromptTemplateController {

    private final PromptTemplateResourceService promptTemplateResourceService;
    private final StructuredOutputService structuredOutputService;

    @GetMapping("/template")
    public String promptTemplate(@ModelAttribute Question question) {
        log.info("prompt template question={}", question);
        return promptTemplateResourceService.promptTemplate3(question);
    }

    @GetMapping("/list")
    public List<String> listOutput(@ModelAttribute Question question) {
        log.info("list output question={}", question);
        return structuredOutputService.listOutputConverter(question);
    }

    @GetMapping("/map")
    public Map<String, Object> mapOutput(@ModelAttribute Question question) {
        log.info("map output question={}", question);
        return structuredOutputService.mapOutputConverter(question);
    }

    @GetMapping("/bean")
    public Contents beanOutput(@ModelAttribute Question question) {
        log.info("bean output question={}", question);
        return structuredOutputService.beanOutputConverter(question);
    }

    @GetMapping("/shops")
    public List<Shop> shops(@ModelAttribute Question question) {
        log.info("shops question={}", question);
        return structuredOutputService.parameterizedTypeReference(question);
    }
}
