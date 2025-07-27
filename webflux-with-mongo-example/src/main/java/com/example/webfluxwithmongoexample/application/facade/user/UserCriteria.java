package com.example.webfluxwithmongoexample.application.facade.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserCriteria {
    private String name;
    private Integer age;
}
