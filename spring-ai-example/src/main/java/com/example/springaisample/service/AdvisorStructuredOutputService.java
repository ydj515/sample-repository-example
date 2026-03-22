package com.example.springaisample.service;

import java.util.Map;

import com.example.springaisample.model.Contents;
import com.example.springaisample.model.Question;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AdvisorStructuredOutputService {

    private final ChatClient chatClient;

    // src/main/resources 폴더에 prompts 폴더 생성 후 아래 prompt template 파일을 생성
    @Value("classpath:prompts/system-message-prompt-template.st")
    private Resource systemResource;

    @Value("classpath:prompts/user-message-structured-output.st")
    private Resource userResource;

    // Constructor
    public AdvisorStructuredOutputService(ChatClient.Builder chatClientBuilder) {
        // 특정 조건이 충족될 때까지 LLM을 재귀적 또는 반복적으로 호출 3회 진행
        StructuredOutputValidationAdvisor validationAdvisor = StructuredOutputValidationAdvisor.builder()
                .outputType(Contents.class)
                .maxRepeatAttempts(3)
                .advisorOrder(Ordered.HIGHEST_PRECEDENCE + 1000)
                .build();
        this.chatClient = chatClientBuilder.clone()
                .defaultAdvisors(validationAdvisor)
                .build();
    }

    public Contents beanOutputConverter(Question question) {
        SystemPromptTemplate userQuestionTemplate = new SystemPromptTemplate(userResource);
        SystemPromptTemplate systemTemplate = new SystemPromptTemplate(systemResource);
        String userQuestion = userQuestionTemplate.render(Map.of(
                "location", question.location(),
                "content", question.content()
        ));
        String systemMessage = systemTemplate.render(Map.of("language", question.language()));

        return chatClient.prompt()
                .system(systemMessage)
                .user(userQuestion)
                .call()
                .entity(Contents.class);
    }
}
