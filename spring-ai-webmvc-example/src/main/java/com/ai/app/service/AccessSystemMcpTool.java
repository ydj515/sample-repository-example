package com.ai.app.service;

import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AccessSystemMcpTool {

    @McpTool(description = "사번이 일치하면 출입문을 연다.")
    public String mcpOpen() {
        log.info("출입문이 열립니다...");
        return "출입문이 열립니다...";
    }
    @McpTool(description = "사번이 일치하지 않으면 출문을 열수 없다.")
    public String mcpClose() {
        log.info("출입문을 열수 없습니다...");
        return "출입문을 열수 없습니다...";
    }

}