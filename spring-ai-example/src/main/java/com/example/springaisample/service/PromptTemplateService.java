package com.example.springaisample.service;

import com.example.springaisample.model.Question;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PromptTemplateService {

    private final ChatClient chatClient;

    // json을 template으로 써야한다면 "<", ">" 형태로 넣어도 동작한다.
    private final String userQuestionTemplateText1 = """
            {location} 지역에 {content} 정보를 5개 이상 알려주세요.
            검색 후 시스템에 설정된 언어로 번역된 내용만 출력해 주세요.
            """;
    private final String userQuestionTemplateText2 = """
            <location> 지역에 <content> 정보를 5개 이상 알려주세요.
            검색 후 시스템에 설정된 언어로 번역된 내용만 출력해 주세요.
            """;
    private final String systemTemplateText1 = """
            사용자의 검색 결과를 {language}로 번역해주세요.
            """;
    private final String systemTemplateText2 = """
            사용자의 검색 결과를 <language>로 번역해주세요.
            """;

    private final PromptTemplate userQuestionTemplate1 = PromptTemplate.builder()
            .template(userQuestionTemplateText1)
            .build();
    private final PromptTemplate userQuestionTemplate2 = PromptTemplate.builder()
            .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
            .template(userQuestionTemplateText2)
            .build();
    private final PromptTemplate systemTemplate1 = PromptTemplate.builder()
            .template(systemTemplateText1)
            .build();
    private final PromptTemplate systemTemplate2 = PromptTemplate.builder()
            .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
            .template(systemTemplateText2)
            .build();

    public PromptTemplateService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.clone().build();
    }

    public String promptTemplate1(Question question) {
        Prompt prompt = userQuestionTemplate1.create(Map.of(
                "location", question.location(),
                "content", question.content()
        ));

        return this.chatClient.prompt(prompt)
                .call()
                .content();
    }

    public String promptTemplate2(Question question) {
        String userQuestion = userQuestionTemplate1.render(Map.of(
                "location", question.location(),
                "content", question.content()
        ));

        return this.chatClient.prompt()
                .user(userQuestion)
                .call()
                .content();
    }

    public String promptTemplate3(Question question) {
        String userQuestion = userQuestionTemplate1.render(Map.of(
                "location", question.location(),
                "content", question.content()
        ));
        String systemMessage = systemTemplate1.render(Map.of("language", question.language()));

        return this.chatClient.prompt()
                .user(userQuestion)
                .system(systemMessage)
                .call()
                .content();
    }

    public String promptTemplate4(Question question) {
        Message userQuestionMessage = userQuestionTemplate1.createMessage(Map.of(
                "location", question.location(),
                "content", question.content()
        ));
        Message systemMessage = systemTemplate1.createMessage(Map.of("language", question.language()));
        Prompt prompt = new Prompt(List.of(userQuestionMessage, systemMessage));

        return this.chatClient.prompt(prompt)
                .call()
                .content();
    }

    public String promptTemplate5(Question question) {
        Message userQuestionMessage = userQuestionTemplate2.createMessage(Map.of(
                "location", question.location(),
                "content", question.content()
        ));
        Message systemMessage = systemTemplate2.createMessage(Map.of("language", question.language()));

        return this.chatClient.prompt()
                .messages(userQuestionMessage, systemMessage)
                .call()
                .content();
    }
}
