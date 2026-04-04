package com.example.springaisample.controller;

import java.io.IOException;

import com.example.springaisample.service.tool.AccessControlToolService;
import com.example.springaisample.service.tool.CustomerInquiryToolService;
import com.example.springaisample.service.tool.ShoppingRecommendationToolService;
import com.example.springaisample.service.tool.TimeWeatherToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/test/tools")
@Slf4j
@RequiredArgsConstructor
// Chapter 6. Tool Calling Controller
public class ToolCallingController {

    // 1. Date Time
    private final TimeWeatherToolService timeWeatherToolService;
    // 2. Customer Inquiry - JSON
    // 2. Customer Inquiry - String
    private final CustomerInquiryToolService customerInquiryToolService;
    // 3. Recommendation
    private final ShoppingRecommendationToolService shoppingRecommendationToolService;
    // 4. Access System
    private final AccessControlToolService accessControlToolService;

    // 1. Date Time
    @GetMapping({"/date-time", "/data-time"})
    public String chatTimeWeather(@RequestParam("prompt") String userPrompt) {
        log.info("tools date-time prompt={}", userPrompt);
        return timeWeatherToolService.chat(userPrompt);
    }

    // 2. Customer Inquiry - JSON
    @GetMapping("/customer-inquiry-json")
    public String getCustomer(@RequestParam("prompt") String userPrompt) {
        log.info("tools customer-inquiry-json prompt={}", userPrompt);
        return customerInquiryToolService.getCustomer(userPrompt);
    }

    // 2. Customer Inquiry - String
    @GetMapping("/customer-inquiry-string")
    public String getCustomerString(@RequestParam("prompt") String userPrompt) {
        log.info("tools customer-inquiry-string prompt={}", userPrompt);
        return customerInquiryToolService.getCustomerAsString(userPrompt);
    }

    // 3. Recommendation
    @GetMapping("/recommendation")
    public String getOrderedByCustomer(
            @RequestParam("prompt") String userPrompt,
            @RequestParam("user_id") String userId
    ) {
        log.info("tools recommendation prompt={}, userId={}", userPrompt, userId);
        return shoppingRecommendationToolService.getRecommendations(userPrompt, userId);
    }

    // 4. Access System
    @PostMapping(value = "/access-system", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String accessSystem(@RequestParam(value = "attach", required = false) MultipartFile attach) throws IOException {
        if (attach == null || attach.isEmpty() || attach.getContentType() == null || !attach.getContentType().startsWith("image/")) {
            log.info("tools access-system invalid attach");
            return "이미지를 올려주세요.";
        }

        return accessControlToolService.analyzeAccessImage(attach.getContentType(), attach.getBytes());
    }
}
