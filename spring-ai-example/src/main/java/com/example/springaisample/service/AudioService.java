package com.example.springaisample.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

@Service
public class AudioService {

    private static final String FRIENDLY_KOREAN_SYSTEM_MESSAGE = "질문에 대한 답변을 한국어로 친절하게 답변해야 합니다.";

    private final ChatClient chatClient;
    // 음성 파일을 Text로 변환
    private final TranscriptionModel transcriptionModel;

    private final OpenAiAudioTranscriptionOptions transcriptionOptions;
    // Text 파일을 음성 파일로 변환
    private final TextToSpeechModel textToSpeechModel;
    // Text 파일을 음성 파일로 변환 할때 어떤 Model을 사용할지 설정
    private final OpenAiAudioSpeechOptions speechOptions;

    // Constructor
    public AudioService(
            @Qualifier("openAiChatClient") ChatClient openAiChatClient,
            TranscriptionModel transcriptionModel,
            TextToSpeechModel textToSpeechModel
    ) {
        this.chatClient = openAiChatClient.mutate()
                .defaultSystem(FRIENDLY_KOREAN_SYSTEM_MESSAGE)
                .build();

        // 음성 파일을 Text로 변환 하기 위한 Model 설정 및 Option 설정
        this.transcriptionModel = transcriptionModel;
        this.transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                .model("whisper-1")
                .language("ko")
                .build();

        // Text 파일을 음성 파일로 변환하기 위한 Model 설정 및 Option 설정
        this.textToSpeechModel = textToSpeechModel;
        this.speechOptions = OpenAiAudioSpeechOptions.builder()
                .model("gpt-4o-mini-tts")
                .voice(OpenAiAudioApi.SpeechRequest.Voice.NOVA)
                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                .speed(1.0)
                .build();
    }

    // 음성 데이터를 입력 받아 Text로 변환
    public String speechToText(MultipartFile multipartFile) throws IOException {
        Path tempFile = createTempAudioFile(multipartFile);

        try {
            multipartFile.transferTo(tempFile);
            Resource audioResource = new FileSystemResource(tempFile);
            // Prompt 생성
            AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, transcriptionOptions);
            // Model 호출, 호출 시 음성 파일 전송 후 Text로 답변
            AudioTranscriptionResponse response = transcriptionModel.call(prompt);
            return response.getResult().getOutput();
        }
        finally {
            Files.deleteIfExists(tempFile);
        }
    }

    // Text 파일을 음성으로 변환
    public Map<String, String> textToSpeech(String text) {
        // Prompt 생성
        TextToSpeechPrompt speechPrompt = new TextToSpeechPrompt(text, speechOptions);
        // Model 호출, 호출 시 Text 파일 전송 후 음성 파일을 byte[]로 답변
        TextToSpeechResponse response = textToSpeechModel.call(speechPrompt);
        byte[] bytes = response.getResult().getOutput();
        // byte []를 base64 형식의 String 으로 변환 후 전송
        return audioOnlyResponse(bytes);
    }

    // Text를 음성으로 변환, 단 음성으로 변환 시 Stream을 통해 데이터를 받아 Flux로 전달
    public Flux<byte[]> textToSpeechChatStream(String question) {
        String answerText = generateChatAnswer(question);
        TextToSpeechPrompt speechPrompt = new TextToSpeechPrompt(answerText, speechOptions);

        return textToSpeechModel.stream(speechPrompt)
                .map(response -> response.getResult().getOutput());
    }

    // Question을 LLM에 전달 하여 답변을 받고, 받은 답변을 음성으로 변환 다시 답변을 요청, 이후 답변에 대한 내용을 음성과 Text로 전달
    public Map<String, String> textToSpeechChat(String question) {
        // LLM에 요청 후 답변을 Text로 받음
        String answerText = generateChatAnswer(question);

        // 받은 답변을 음성으로 변환
        TextToSpeechPrompt speechPrompt = new TextToSpeechPrompt(answerText, speechOptions);
        TextToSpeechResponse response = textToSpeechModel.call(speechPrompt);

        // 답변 Text와 답변을 음성으로 변환하여 Map으로 전송
        Map<String, String> result = audioOnlyResponse(response.getResult().getOutput());
        result.put("answer", answerText);
        return result;
    }

    // 음성 데이터를 Text로 변환 한 후 다시 LLM에 전달 하여 응답 하녀 Flux로 전달
    public Flux<String> speechToTextChat(MultipartFile multipartFile) throws IOException {
        String text = speechToText(multipartFile);

        return chatClient.prompt()
                .user(text)
                .stream()
                .content();
    }

    public Map<String, String> speechToTextChatVoice(MultipartFile multipartFile) throws IOException {
        String text = speechToText(multipartFile);
        return textToSpeechChat(text);
    }

    // 질문을 LLM에 전달 하여 음성 변환에 사용할 답변 Text를 생성
    private String generateChatAnswer(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    // byte[] 음성 데이터를 base64 문자열로 변환하여 응답 Map으로 생성
    private Map<String, String> audioOnlyResponse(byte[] bytes) {
        String base64Audio = Base64.getEncoder().encodeToString(bytes);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("audio", base64Audio);
        return result;
    }

    // 업로드 된 음성 파일을 전사 Model에 전달할 임시 파일로 생성
    private Path createTempAudioFile(MultipartFile multipartFile) throws IOException {
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = ".tmp";

        if (originalFilename != null) {
            int extensionIndex = originalFilename.lastIndexOf('.');
            if (extensionIndex >= 0 && extensionIndex < originalFilename.length() - 1) {
                suffix = originalFilename.substring(extensionIndex);
            }
        }

        return Files.createTempFile("audio-upload-", suffix);
    }
}
