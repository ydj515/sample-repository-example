package com.example.webfluxwithmongoexample.domain.user.service;

import com.example.webfluxwithmongoexample.application.facade.user.UserCriteria;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserCommand {
    private String name;
    private Integer age;

    public UserCommand(UserCriteria criteria) {
        this.name = criteria.getName();
        this.age = criteria.getAge();
    }
}
