package com.example.springaisample.config;

import com.example.springaisample.advisor.SafeGuardPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class AdvisorConfig {

    @Bean
    SafeGuardPolicy safeGuardPolicy(@Value("classpath:advisors/sensitive-words.txt") Resource sensitiveWordsResource) {
        return SafeGuardPolicy.fromResource(sensitiveWordsResource);
    }
}
