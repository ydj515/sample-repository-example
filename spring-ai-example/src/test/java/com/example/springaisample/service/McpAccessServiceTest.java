package com.example.springaisample.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import com.example.springaisample.service.mcp.McpAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;

class McpAccessServiceTest {

    @Test
    void analyzeAccessImageBuildsVisionPromptAndRegistersLocalTools() {
        byte[] imageBytes = "image".getBytes(StandardCharsets.UTF_8);
        CapturingChatModel chatModel = new CapturingChatModel("출입문이 열립니다.");
        McpAccessService service = new McpAccessService(ChatClient.builder(chatModel), ToolCallbackProvider.from());

        String result = service.analyzeAccessImage("image/png", imageBytes);

        Prompt prompt = chatModel.lastPrompt();
        ToolCallingChatOptions options = (ToolCallingChatOptions) prompt.getOptions();

        assertThat(result).isEqualTo("출입문이 열립니다.");
        assertThat(prompt.getSystemMessage().getText()).contains("이미지 분석가");
        assertThat(prompt.getUserMessage().getText()).contains("답변을 만들때는 숫자로만 알려줘");
        assertThat(prompt.getUserMessage().getMedia()).hasSize(1);
        assertThat(prompt.getUserMessage().getMedia().get(0).getDataAsByteArray()).isEqualTo(imageBytes);
        assertThat(options.getToolCallbacks())
                .extracting(toolCallback -> toolCallback.getToolDefinition().name())
                .containsExactlyInAnyOrder("open", "close", "getCardList");
    }
}
