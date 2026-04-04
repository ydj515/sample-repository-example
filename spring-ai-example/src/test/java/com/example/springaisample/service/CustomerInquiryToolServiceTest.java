package com.example.springaisample.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.springaisample.service.tool.CustomerInquiryToolService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

class CustomerInquiryToolServiceTest {

    @Test
    void getCustomerRegistersCustomerTool() {
        CapturingChatModel chatModel = new CapturingChatModel("{\"name\":\"James\"}");
        CustomerInquiryToolService service = new CustomerInquiryToolService(ChatClient.builder(chatModel));

        String result = service.getCustomer("id01 고객 정보 알려줘");

        Prompt prompt = chatModel.lastPrompt();

        assertThat(result).isEqualTo("{\"name\":\"James\"}");
        assertThat(prompt.getUserMessage().getText()).isEqualTo("id01 고객 정보 알려줘");
        assertThat(prompt.getOptions()).isInstanceOf(ToolCallingChatOptions.class);
        assertThat(((ToolCallingChatOptions) prompt.getOptions()).getToolCallbacks())
                .extracting(toolCallback -> toolCallback.getToolDefinition().name())
                .containsExactlyInAnyOrder("getCustomer", "getAllCustomer");
    }

    @Test
    void getCustomerAsStringRegistersStringConverterTool() {
        CapturingChatModel chatModel = new CapturingChatModel("사용자 이름은 James, 나이는 30");
        CustomerInquiryToolService service = new CustomerInquiryToolService(ChatClient.builder(chatModel));

        String result = service.getCustomerAsString("id01 고객 정보 알려줘");

        Prompt prompt = chatModel.lastPrompt();

        assertThat(result).isEqualTo("사용자 이름은 James, 나이는 30");
        assertThat(prompt.getOptions()).isInstanceOf(ToolCallingChatOptions.class);
        assertThat(((ToolCallingChatOptions) prompt.getOptions()).getToolCallbacks())
                .extracting(toolCallback -> toolCallback.getToolDefinition().name())
                .containsExactlyInAnyOrder("getCustomer", "getAllCustomer");
    }
}
