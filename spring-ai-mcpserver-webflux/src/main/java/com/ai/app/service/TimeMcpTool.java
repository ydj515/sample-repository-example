package com.ai.app.service;

import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
public class TimeMcpTool {

    @McpTool(description = "사용자가 운영하는 시스템의 timezone 을 기반으로 현재 날짜와 시간 정보를 알려줍니다. ")
    Mono<String> getCurrentDateTime() {
        log.info("현재 시간: {}",LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString());
        return Mono.just("결과: " + LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString());
    }

}
