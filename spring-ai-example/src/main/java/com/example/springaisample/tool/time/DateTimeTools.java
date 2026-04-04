package com.example.springaisample.tool.time;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.i18n.LocaleContextHolder;

@Slf4j
public class DateTimeTools {

    @Tool(description = "사용자가 운영하는 시스템의 timezone 을 기반으로 현재 날짜와 시간 정보를 알려줍니다. ")
    String getCurrentDateTime() {
        log.info("현재 시간: {}", LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()));
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

    @Tool(description = "사용자가 요청한 시간으로 알람 설정")
    void setAlarm(@ToolParam(description = "ISO-8601 형식으로 제공된 시간", required = true) String time) {
        LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
        log.info("다음 시간으로 알람이 설정 되었습니다. {}", alarmTime);
    }
}
