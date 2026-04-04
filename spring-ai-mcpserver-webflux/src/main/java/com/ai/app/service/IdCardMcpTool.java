package com.ai.app.service;

import com.ai.app.service.idCard.IdCardService;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
//@RequiredArgsConstructor
public class IdCardMcpTool {

    IdCardService idCardService;

    public IdCardMcpTool(){
        this.idCardService = new IdCardService();
    }

    // @ToolParam을 이용해 사진에서 분석한 사번 정보를 Argument로 입력
    @McpTool(description = "직원의 사번과 모든 직원 사번과 비교한다.")
    Mono<Boolean> getMcpCardList(@McpToolParam(description = "사번") String idCardNumber) {
        log.info("직원의 사번: {}", idCardNumber);

        return  Mono.just(idCardService.checkIdCardNumber(idCardNumber));
    }
}