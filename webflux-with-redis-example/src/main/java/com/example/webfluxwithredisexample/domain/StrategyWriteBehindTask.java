package com.example.webfluxwithredisexample.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StrategyWriteBehindTask {
    private String type;
    private Long userId;
    private StrategyUser data;
}
