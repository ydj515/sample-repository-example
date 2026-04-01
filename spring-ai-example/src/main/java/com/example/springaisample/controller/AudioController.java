package com.example.springaisample.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import com.example.springaisample.service.AudioService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/test/audio")
@Slf4j
@RequiredArgsConstructor
// Multimodality API – Audio and Speech Controller
public class AudioController {

    // 1. Text to Speech
    // 2. Text to Speech: Chat
    // 3. Text to Speech: Chat Stream
    // 4. Speech to Text
    // 5. Speech to Text: Chat
    // 6. Speech to Text: Chat Voice
    private final AudioService audioService;

    // 1. Text to Speech
    // 사용자가 입력한 텍스트를 LLM 기반 음성 합성 모델로 전달하여 해당 문장을 자연스러운 음성 데이터(Audio) 로 변환해 반환하는 기능입니다.
    @GetMapping("/text-to-speech")
    public Map<String, String> textToSpeech(@RequestParam("prompt") String question) {
        log.info("audio text-to-speech prompt={}", question);
        return audioService.textToSpeech(question);
    }

    // 2. Text to Speech: Chat
    // 사용자 입력 텍스트를 먼저 LLM에 전달하여 답변을 생성하고, 생성된 텍스트 응답을 다시 음성으로 변환하여 사용자에게 제공하는 방식입니다.
    @GetMapping("/text-to-speech-chat")
    public Map<String, String> textToSpeechChat(@RequestParam("prompt") String question) {
        log.info("audio text-to-speech-chat prompt={}", question);
        return audioService.textToSpeechChat(question);
    }

    // 3. Text to Speech: Chat Stream
    // 사용자 텍스트를 LLM에 전달하여 생성된 응답을 실시간 스트리밍(Streaming) 방식으로 음성 변환하여 전달하는 기능입니다.
    @GetMapping("/text-to-speech-chat-stream")
    public void textToSpeechChatStream(@RequestParam("prompt") String question, HttpServletResponse response) throws IOException {
        log.info("audio text-to-speech-chat-stream prompt={}", question);
        Flux<byte[]> bytes = audioService.textToSpeechChatStream(question);
        OutputStream os = response.getOutputStream();

        response.setContentType("audio/mpeg");

        ByteArrayOutputStream combined = new ByteArrayOutputStream();
        for (byte[] data : bytes.toIterable()) {
            combined.write(data);
        }
        os.write(combined.toByteArray());
        os.flush();
    }

    // 4. Speech to Text
    // 사용자가 말한 음성 입력(Audio)을 텍스트로 변환하여 Prompt 형태로 시스템에 전달하는 기능입니다.
    @PostMapping(value = "/speech-to-text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String speechToText(@RequestParam(value = "attach", required = false) MultipartFile attach) throws IOException {
        if (attach == null || attach.isEmpty()) {
            return "음성 파일을 올려주세요.";
        }

        log.info("audio speech-to-text contentType={}, size={}", attach.getContentType(), attach.getSize());
        return audioService.speechToText(attach);
    }

    // 5. Speech to Text: Chat
    // 사용자의 음성을 텍스트로 변환한 뒤, 그 텍스트를 LLM에 전달해 생성된 답변을 다시 사용자에게 제공하는 방식입니다.
    // 프로세스 흐름
    // Speech → STT → Text
    // Text → LLM → Text 응답
    @PostMapping(value = "/speech-to-text-chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> speechToTextChat(@RequestParam(value = "attach", required = false) MultipartFile attach) throws IOException {
        if (attach == null || attach.isEmpty()) {
            return Flux.just("음성 파일을 올려주세요.");
        }

        log.info("audio speech-to-text-chat contentType={}, size={}", attach.getContentType(), attach.getSize());
        return audioService.speechToTextChat(attach);
    }

    // 6. Speech to Text: Chat Voice
    // 사용자 음성을 텍스트로 변환한 후 LLM에 전달하여 답변을 생성하고, 그 응답을 텍스트 + 음성 두 가지 형태로 모두 제공하는 방식입니다.
    // 프로세스 흐름
    // Speech → STT → Text
    // Text → LLM → Text 응답
    // Text 응답 → TTS → Audio 응답
    @PostMapping(value = "/speech-to-text-chat-voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> speechToTextChatVoice(@RequestParam(value = "attach", required = false) MultipartFile attach) throws IOException {
        if (attach == null || attach.isEmpty()) {
            return Map.of("message", "음성 파일을 올려주세요.");
        }

        log.info("audio speech-to-text-chat-voice contentType={}, size={}", attach.getContentType(), attach.getSize());
        return audioService.speechToTextChatVoice(attach);
    }
}
