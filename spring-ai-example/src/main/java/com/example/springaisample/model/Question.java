package com.example.springaisample.model;

import org.springframework.util.StringUtils;

public record Question(String location, String content, String language) {

    public Question {
        location = StringUtils.hasText(location) ? location : "서울 종로";
        content = StringUtils.hasText(content) ? content : "맛집";
        language = StringUtils.hasText(language) ? language : "한국어";
    }
}
