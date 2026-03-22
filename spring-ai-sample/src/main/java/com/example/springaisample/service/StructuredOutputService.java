package com.example.springaisample.service;

import java.util.Map;
import java.util.List;

import com.example.springaisample.model.Contents;
import com.example.springaisample.model.Question;
import com.example.springaisample.model.Shop;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.ai.template.st.StTemplateRenderer;

@Service
public class StructuredOutputService {

    private final ChatClient chatClient;

    @Value("classpath:prompts/system-message-prompt-template.st")
    private Resource systemResource;

    @Value("classpath:prompts/user-message-structured-output.st")
    private Resource userResource;

    @Value("classpath:prompts/user-message-structured-output-map-output.st")
    private Resource userResourceMapOutput;

    @Value("classpath:prompts/user-message-structured-list-output.st")
    private Resource userResourceListOutput;

    public StructuredOutputService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.clone().build();
    }

    public Contents beanOutputConverter(Question question) {
        SystemPromptTemplate systemTemplate = new SystemPromptTemplate(systemResource);
        SystemPromptTemplate userQuestionTemplate = new SystemPromptTemplate(userResource);
        String userQuestion = userQuestionTemplate.render(Map.of(
                "location", question.location(),
                "content", question.content()
        ));
        String systemMessage = systemTemplate.render(Map.of("language", question.language()));

        return this.chatClient.prompt()
                .system(systemMessage)
                .user(userQuestion)
                .call()
                .entity(Contents.class);
    }

    public List<String> listOutputConverter(Question question) {
        SystemPromptTemplate systemTemplate = new SystemPromptTemplate(systemResource);
        SystemPromptTemplate userQuestionTemplate = new SystemPromptTemplate(userResourceListOutput);
        String userQuestion = userQuestionTemplate.render(Map.of(
                "location", question.location(),
                "content", question.content()
        ));
        String systemMessage = systemTemplate.render(Map.of("language", question.language()));

        return this.chatClient.prompt()
                .system(systemMessage)
                .user(userQuestion)
                .call()
                .entity(new ListOutputConverter(new DefaultConversionService()));
    }

    public List<Shop> parameterizedTypeReference(Question question) {
        SystemPromptTemplate systemTemplate = new SystemPromptTemplate(systemResource);
        SystemPromptTemplate userQuestionTemplate = new SystemPromptTemplate(userResource);
        String userQuestion = userQuestionTemplate.render(Map.of(
                "location", question.location(),
                "content", question.content()
        ));
        String systemMessage = systemTemplate.render(Map.of("language", question.language()));

        return this.chatClient.prompt()
                .system(systemMessage)
                .user(userQuestion)
                .call()
                .entity(new ParameterizedTypeReference<List<Shop>>() {
                });
    }

    public Map<String, Object> mapOutputConverter(Question question) {
        SystemPromptTemplate systemTemplate = new SystemPromptTemplate(systemResource);
        SystemPromptTemplate userQuestionTemplate = SystemPromptTemplate.builder()
                .resource(userResourceMapOutput)
                .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
                .build();
        String userQuestion = userQuestionTemplate.render(Map.of(
                "location", question.location(),
                "content", question.content()
        ));
        String systemMessage = systemTemplate.render(Map.of("language", question.language()));

        return this.chatClient.prompt()
                .system(systemMessage)
                .user(userQuestion)
                .call()
                .entity(new MapOutputConverter());
    }
}
