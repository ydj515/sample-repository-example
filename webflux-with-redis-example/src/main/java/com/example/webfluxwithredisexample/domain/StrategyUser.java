package com.example.webfluxwithredisexample.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StrategyUser {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private String updatedAt;
}
