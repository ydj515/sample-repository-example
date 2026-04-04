package com.example.springaisample.controller;

import java.io.IOException;

import com.example.springaisample.service.mcp.McpAccessService;
import com.example.springaisample.service.mcp.McpWeatherService;
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
@RequestMapping("/test/mcp")
@Slf4j
@RequiredArgsConstructor
// 9. MCP(Model Context Protocol) Controller
public class McpController {

    private final McpWeatherService mcpWeatherService;
    private final McpAccessService mcpAccessService;

    // 1. MCP Chat
    @GetMapping("/weather")
    public String chatTimeWeather(@RequestParam("prompt") String userPrompt) {
        log.info("mcp weather prompt={}", userPrompt);
        return mcpWeatherService.chat(userPrompt);
    }

    // 2. MCP Access
    @PostMapping(value = "/access", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String access(@RequestParam(value = "attach", required = false) MultipartFile attach) throws IOException {
        if (attach == null || attach.isEmpty() || attach.getContentType() == null || !attach.getContentType().startsWith("image/")) {
            return "이미지를 올려주세요.";
        }

        return mcpAccessService.analyzeAccessImage(attach.getContentType(), attach.getBytes());
    }
}
