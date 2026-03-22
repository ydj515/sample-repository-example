package com.example.springaisample.service;

import java.util.List;
import java.util.Map;

import com.example.springaisample.model.Question;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class PromptTemplateResourceService {

    private final ChatClient chatClient;

    @Value("classpath:prompts/system-message-prompt-template.st")
    private Resource systemResource;

    @Value("classpath:prompts/user-message-prompt-template.st")
    private Resource userResource;

    public PromptTemplateResourceService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.clone().build();
    }

    public String promptTemplate1(Question question) {
        SystemPromptTemplate userQuestionTemplate = new SystemPromptTemplate(userResource);
        Prompt prompt = userQuestionTemplate.create(Map.of(
                "location", question.location(),
                "content", question.content()
        ));

        return this.chatClient.prompt(prompt)
                .call()
                .content();
    }

    public String promptTemplate2(Question question) {
        SystemPromptTemplate userQuestionTemplate = new SystemPromptTemplate(userResource);
        String userQuestion = userQuestionTemplate.render(Map.of(
                "location", question.location(),
                "content", question.content()
        ));

        return this.chatClient.prompt()
                .user(userQuestion)
                .call()
                .content();
    }

    public String promptTemplate3(Question question) {
        SystemPromptTemplate userQuestionTemplate = new SystemPromptTemplate(userResource);
        SystemPromptTemplate systemTemplate = new SystemPromptTemplate(systemResource);
        String userQuestion = userQuestionTemplate.render(Map.of(
                "location", question.location(),
                "content", question.content()
        ));
        String systemMessage = systemTemplate.render(Map.of("language", question.language()));

        return this.chatClient.prompt()
                .system(systemMessage)
                .user(userQuestion)
                .call()
                .content();
    }

    public String promptTemplate4(Question question) {
        SystemPromptTemplate userQuestionTemplate = new SystemPromptTemplate(userResource);
        SystemPromptTemplate systemTemplate = new SystemPromptTemplate(systemResource);
        Message userQuestionMessage = userQuestionTemplate.createMessage(Map.of(
                "location", question.location(),
                "content", question.content()
        ));
        Message systemMessage = systemTemplate.createMessage(Map.of("language", question.language()));
        Prompt prompt = new Prompt(List.of(userQuestionMessage, systemMessage));

        return this.chatClient.prompt(prompt)
                .call()
                .content();
    }

    public String promptTemplate5(Question question) {
        SystemPromptTemplate userQuestionTemplate = new SystemPromptTemplate(userResource);
        SystemPromptTemplate systemTemplate = new SystemPromptTemplate(systemResource);
        Message userQuestionMessage = userQuestionTemplate.createMessage(Map.of(
                "location", question.location(),
                "content", question.content()
        ));
        Message systemMessage = systemTemplate.createMessage(Map.of("language", question.language()));

        return this.chatClient.prompt()
                .messages(systemMessage, userQuestionMessage)
                .call()
                .content();
    }
}
