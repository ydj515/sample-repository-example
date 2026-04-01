package com.example.springaisample.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.mock.web.MockMultipartFile;

class AudioServiceTest {

    @Test
    void textToSpeechReturnsBase64Audio() {
        byte[] audioBytes = "audio-data".getBytes(StandardCharsets.UTF_8);
        AudioService service = new AudioService(
                ChatClient.builder(new CapturingChatModel()).build(),
                new CapturingTranscriptionModel(transcriptionResponse("unused")),
                new CapturingTextToSpeechModel(speechResponse(audioBytes))
        );

        Map<String, String> result = service.textToSpeech("안녕하세요");

        assertThat(result).containsEntry("audio", Base64.getEncoder().encodeToString(audioBytes));
    }

    @Test
    void textToSpeechChatReturnsAnswerAndAudio() {
        byte[] audioBytes = "chat-audio".getBytes(StandardCharsets.UTF_8);
        CapturingChatModel chatModel = new CapturingChatModel("친절한 답변");
        CapturingTextToSpeechModel speechModel = new CapturingTextToSpeechModel(speechResponse(audioBytes));
        AudioService service = new AudioService(
                ChatClient.builder(chatModel).build(),
                new CapturingTranscriptionModel(transcriptionResponse("unused")),
                speechModel
        );

        Map<String, String> result = service.textToSpeechChat("질문입니다");
        Prompt prompt = chatModel.lastPrompt();
        TextToSpeechPrompt speechPrompt = speechModel.lastCallPrompt();
        OpenAiAudioSpeechOptions speechOptions = (OpenAiAudioSpeechOptions) speechPrompt.getOptions();

        assertThat(result).containsEntry("answer", "친절한 답변");
        assertThat(result).containsEntry("audio", Base64.getEncoder().encodeToString(audioBytes));
        assertThat(prompt.getSystemMessage().getText()).contains("한국어로 친절하게");
        assertThat(prompt.getUserMessage().getText()).isEqualTo("질문입니다");
        assertThat(speechPrompt.getInstructions().getText()).isEqualTo("친절한 답변");
        assertThat(speechOptions.getModel()).isEqualTo("gpt-4o-mini-tts");
        assertThat(speechOptions.getVoice()).isEqualTo("nova");
        assertThat(speechOptions.getFormat()).isEqualTo("mp3");
        assertThat(speechOptions.getSpeed()).isEqualTo(1.0);
    }

    @Test
    void textToSpeechChatStreamReturnsSpeechBytes() {
        CapturingChatModel chatModel = new CapturingChatModel("스트림 답변");
        CapturingTextToSpeechModel speechModel = new CapturingTextToSpeechModel(speechResponse("unused".getBytes(StandardCharsets.UTF_8)));
        speechModel.enqueueStreamResponses(
                speechResponse("first".getBytes(StandardCharsets.UTF_8)),
                speechResponse("second".getBytes(StandardCharsets.UTF_8))
        );

        AudioService service = new AudioService(
                ChatClient.builder(chatModel).build(),
                new CapturingTranscriptionModel(transcriptionResponse("unused")),
                speechModel
        );

        List<byte[]> result = service.textToSpeechChatStream("스트림 질문")
                .collectList()
                .block();

        TextToSpeechPrompt speechPrompt = speechModel.lastStreamPrompt();

        assertThat(result)
                .hasSize(2)
                .allSatisfy(bytes -> assertThat(bytes).isNotEmpty());
        assertThat(result.get(0)).isEqualTo("first".getBytes(StandardCharsets.UTF_8));
        assertThat(result.get(1)).isEqualTo("second".getBytes(StandardCharsets.UTF_8));
        assertThat(speechPrompt.getInstructions().getText()).isEqualTo("스트림 답변");
    }

    @Test
    void speechToTextUsesWhisperConfiguration() throws Exception {
        CapturingTranscriptionModel transcriptionModel = new CapturingTranscriptionModel(transcriptionResponse("전사 결과"));
        AudioService service = new AudioService(
                ChatClient.builder(new CapturingChatModel()).build(),
                transcriptionModel,
                new CapturingTextToSpeechModel(speechResponse("unused".getBytes(StandardCharsets.UTF_8)))
        );
        MockMultipartFile attach = new MockMultipartFile("attach", "voice.mp3", "audio/mpeg", "voice".getBytes(StandardCharsets.UTF_8));

        String result = service.speechToText(attach);

        OpenAiAudioTranscriptionOptions options = (OpenAiAudioTranscriptionOptions) transcriptionModel.lastPrompt().getOptions();

        assertThat(result).isEqualTo("전사 결과");
        assertThat(options.getModel()).isEqualTo("whisper-1");
        assertThat(options.getLanguage()).isEqualTo("ko");
    }

    @Test
    void speechToTextChatStreamsChatAnswerFromTranscribedText() throws Exception {
        CapturingChatModel chatModel = new CapturingChatModel("채팅 응답");
        AudioService service = new AudioService(
                ChatClient.builder(chatModel).build(),
                new CapturingTranscriptionModel(transcriptionResponse("변환된 질문")),
                new CapturingTextToSpeechModel(speechResponse("unused".getBytes(StandardCharsets.UTF_8)))
        );
        MockMultipartFile attach = new MockMultipartFile("attach", "voice.mp3", "audio/mpeg", "voice".getBytes(StandardCharsets.UTF_8));

        List<String> result = service.speechToTextChat(attach).collectList().block();
        Prompt prompt = chatModel.lastPrompt();

        assertThat(result).containsExactly("채팅 응답");
        assertThat(prompt.getUserMessage().getText()).isEqualTo("변환된 질문");
    }

    @Test
    void speechToTextChatVoiceReturnsAudioForTranscribedText() throws Exception {
        byte[] audioBytes = "voice-answer".getBytes(StandardCharsets.UTF_8);
        AudioService service = new AudioService(
                ChatClient.builder(new CapturingChatModel("음성 답변")).build(),
                new CapturingTranscriptionModel(transcriptionResponse("변환된 질문")),
                new CapturingTextToSpeechModel(speechResponse(audioBytes))
        );
        MockMultipartFile attach = new MockMultipartFile("attach", "voice.mp3", "audio/mpeg", "voice".getBytes(StandardCharsets.UTF_8));

        Map<String, String> result = service.speechToTextChatVoice(attach);

        assertThat(result).containsEntry("answer", "음성 답변");
        assertThat(result).containsEntry("audio", Base64.getEncoder().encodeToString(audioBytes));
    }

    private static TextToSpeechResponse speechResponse(byte[] bytes) {
        return new TextToSpeechResponse(List.of(new Speech(bytes)));
    }

    private static AudioTranscriptionResponse transcriptionResponse(String text) {
        return new AudioTranscriptionResponse(new AudioTranscription(text));
    }
}
