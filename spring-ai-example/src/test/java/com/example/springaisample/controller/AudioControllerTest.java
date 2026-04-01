package com.example.springaisample.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.example.springaisample.service.AudioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

@WebMvcTest(AudioController.class)
class AudioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AudioService audioService;

    @Test
    void textToSpeechEndpointReturnsAudioPayload() throws Exception {
        when(audioService.textToSpeech("안녕하세요"))
                .thenReturn(Map.of("audio", "base64-audio"));

        mockMvc.perform(get("/test/audio/text-to-speech").param("prompt", "안녕하세요"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audio").value("base64-audio"));

        verify(audioService).textToSpeech("안녕하세요");
    }

    @Test
    void textToSpeechChatEndpointReturnsAnswerAndAudio() throws Exception {
        when(audioService.textToSpeechChat("질문"))
                .thenReturn(Map.of("answer", "응답", "audio", "base64-audio"));

        mockMvc.perform(get("/test/audio/text-to-speech-chat").param("prompt", "질문"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("응답"))
                .andExpect(jsonPath("$.audio").value("base64-audio"));

        verify(audioService).textToSpeechChat("질문");
    }

    @Test
    void textToSpeechChatStreamEndpointWritesAudioBytes() throws Exception {
        when(audioService.textToSpeechChatStream("스트림 질문"))
                .thenReturn(Flux.just("first".getBytes(StandardCharsets.UTF_8), "second".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/test/audio/text-to-speech-chat-stream").param("prompt", "스트림 질문"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes("firstsecond".getBytes(StandardCharsets.UTF_8)));

        verify(audioService).textToSpeechChatStream("스트림 질문");
    }

    @Test
    void speechToTextEndpointReturnsTranscribedText() throws Exception {
        MockMultipartFile attach = new MockMultipartFile("attach", "voice.mp3", "audio/mpeg", "voice".getBytes(StandardCharsets.UTF_8));
        when(audioService.speechToText(any(MultipartFile.class))).thenReturn("전사 결과");

        mockMvc.perform(multipart("/test/audio/speech-to-text").file(attach))
                .andExpect(status().isOk())
                .andExpect(content().string("전사 결과"));

        verify(audioService).speechToText(any(MultipartFile.class));
    }

    @Test
    void speechToTextEndpointRejectsMissingAudio() throws Exception {
        mockMvc.perform(multipart("/test/audio/speech-to-text"))
                .andExpect(status().isOk())
                .andExpect(content().string("음성 파일을 올려주세요."));

        verifyNoInteractions(audioService);
    }

    @Test
    void speechToTextChatEndpointReturnsFluxContent() throws Exception {
        MockMultipartFile attach = new MockMultipartFile("attach", "voice.mp3", "audio/mpeg", "voice".getBytes(StandardCharsets.UTF_8));
        when(audioService.speechToTextChat(any(MultipartFile.class))).thenReturn(Flux.just("첫", "응답"));

        mockMvc.perform(multipart("/test/audio/speech-to-text-chat").file(attach))
                .andExpect(status().isOk());

        verify(audioService).speechToTextChat(any(MultipartFile.class));
    }

    @Test
    void speechToTextChatVoiceEndpointReturnsAnswerAndAudio() throws Exception {
        MockMultipartFile attach = new MockMultipartFile("attach", "voice.mp3", "audio/mpeg", "voice".getBytes(StandardCharsets.UTF_8));
        when(audioService.speechToTextChatVoice(any(MultipartFile.class)))
                .thenReturn(Map.of("answer", "응답", "audio", "base64-audio"));

        mockMvc.perform(multipart("/test/audio/speech-to-text-chat-voice").file(attach))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("응답"))
                .andExpect(jsonPath("$.audio").value("base64-audio"));

        verify(audioService).speechToTextChatVoice(any(MultipartFile.class));
    }
}
