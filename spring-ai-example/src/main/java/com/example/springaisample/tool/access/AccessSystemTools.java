package com.example.springaisample.tool.access;

import org.springframework.ai.tool.annotation.Tool;

public class AccessSystemTools {

    @Tool(description = "사번이 일치하면 출입문을 연다.")
    public String open() {
        return "출입문이 열립니다.";
    }

    @Tool(description = "사번이 일치하지 않으면 출문을 열수 없다.")
    public String close() {
        return "출입문을 열수 없습니다.";
    }
}
