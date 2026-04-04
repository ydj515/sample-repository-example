package com.example.springaisample.service.tool;

import com.example.springaisample.tool.customer.CustomerStringTools;
import com.example.springaisample.tool.customer.CustomerTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

@Service
public class CustomerInquiryToolService {

    private final ChatClient chatClient;

    @Autowired
    public CustomerInquiryToolService(OpenAiChatModel openAiChatModel) {
        this(ChatClient.builder(openAiChatModel));
    }

    // Constructor
    public CustomerInquiryToolService(ChatClient.Builder chatClientBuilder) {
        SimpleLoggerAdvisor customLogger = new SimpleLoggerAdvisor(
                request -> "[SimpleLoggerAdvisor] Custom request: " + request.prompt().getUserMessage(),
                response -> "[SimpleLoggerAdvisor] Custom response: " + response.getResult(),
                Ordered.HIGHEST_PRECEDENCE
        );
        this.chatClient = chatClientBuilder
                .defaultAdvisors(customLogger)
                .build();
    }

    // 2. Customer Inquiry - JSON
    public String getCustomer(String question) {
        return chatClient.prompt()
                .system("질문에 대한 답변을 한국어로 친절하게 답변해야 합니다.")
                .user(question)
                .tools(new CustomerTools())
                .call()
                .content();
    }

    // 2. Customer Inquiry - String
    public String getCustomerAsString(String question) {
        return chatClient.prompt()
                .system("질문에 대한 답변을 한국어로 친절하게 답변해야 합니다.")
                .user(question)
                .tools(new CustomerStringTools())
                .call()
                .content();
    }
}
