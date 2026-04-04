package com.example.springaisample.service.mcp;

import com.example.springaisample.tool.access.AccessSystemTools;
import com.example.springaisample.tool.access.EmployeeIdTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

@Service
public class McpAccessService {

    private final ChatClient chatClient;

    // 시스템 메시지 생성
    private static final String SYSTEM_MESSAGE_TEXT = """
            너는 이미지 분석가 입니다.

            """;
    private static final String USER_MESSAGE_TEXT = """
            사용자가 전송한 이미지를 기반으로 사용자의 질문에 맞게 분석하고 답변을 한국어로 하세요.
            답변을 만들때는 숫자로만 알려줘
            숫자가 직원의 사번으로 사용되며 모든 직원의 사번과 일치하는지 검사 한다.
            사번이 일치하면 출입문을 연다.
            사번이 일치하지 않으면 출문을 열수 없다.
            """;

    @Autowired
    public McpAccessService(OpenAiChatModel openAiChatModel, ObjectProvider<ToolCallbackProvider> toolCallbackProvider) {
        this(ChatClient.builder(openAiChatModel), toolCallbackProvider.getIfAvailable(() -> ToolCallbackProvider.from()));
    }

    // Constructor
    public McpAccessService(ChatClient.Builder chatClientBuilder, ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    // 2. MCP Access
    public String analyzeAccessImage(String contentType, byte[] bytes) {
        Media media = Media.builder()
                .mimeType(MimeType.valueOf(contentType))
                .data(new ByteArrayResource(bytes))
                .build();

        UserMessage userMessage = UserMessage.builder()
                .text(USER_MESSAGE_TEXT)
                .media(media)
                .build();

        return chatClient.prompt()
                .system(SYSTEM_MESSAGE_TEXT)
                .messages(userMessage)
                .tools(new AccessSystemTools(), new EmployeeIdTools())
                .call()
                .content();
    }
}
