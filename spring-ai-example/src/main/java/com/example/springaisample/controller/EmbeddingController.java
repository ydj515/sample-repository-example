package com.example.springaisample.controller;

import com.example.springaisample.service.embedding.ChatJdbcService;
import com.example.springaisample.service.embedding.ChatPgvectorService;
import com.example.springaisample.service.embedding.HotelEmbeddingModelService;
import com.example.springaisample.service.embedding.TextEmbeddingModelService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/embedding")
@Slf4j
@RequiredArgsConstructor
// Chapter 7. Embedding Model Controller
public class EmbeddingController {

    // 1. Text Embedding
    // 1. Text Embedding - add
    // 1. Text Embedding - delete
    final TextEmbeddingModelService textEmbeddingModelService;
    // 2. Hotel Data Embedding
    // 2. Hotel Data Embedding - add
    // 2. Hotel Data Embedding - delete
    final HotelEmbeddingModelService hotelEmbeddingModelService;
    // 3. Chat Memory PGvector
    // 3. Chat Memory PGvector - delete
    // 3. Chat Memory PGvector - delete all
    final ChatPgvectorService chatPgvectorService;
    // 4. Chat Memory JDBC
    // 4. Chat Memory JDBC - delete
    // 4. Chat Memory JDBC - delete all
    final ChatJdbcService chatJdbcService;

    // 1. Text Embedding
    // 사전에 준비된 텍스트 데이터를 Embedding 모델을 통해 고차원 벡터(Vector Representation) 로 변환하고, 변환된 벡터를 Vector Store에 저장한 뒤, 사용자의 질문을 Embedding하여 유사도 검색(Semantic Search)으로 관련 문서를 조회하는 방식
    @RequestMapping("/text-embedding")
    public String textEmbedding(@RequestParam("prompt") String userPrompt, String section, String name) {
        log.info(userPrompt);
        return textEmbeddingModelService.similaritySearch(userPrompt).get(0).getText();
    }

    // 1. Text Embedding - add
    @RequestMapping("/add-texts")
    public String add() {
        return textEmbeddingModelService.addData();
    }

    // 1. Text Embedding - delete
    @RequestMapping("/delete-texts")
    public String delete() {
        return textEmbeddingModelService.deleteDate();
    }

    // 2. Hotel Data Embedding
    // 호텔 이름, 위치, 편의시설, 가격, 리뷰 등 호텔 관련 멀티필드 데이터를 Embedding하여 Vector Store에 저장한 뒤, 사용자의 검색 요구(예: “호텔 입실 시간은”)에 대해 의미 기반 검색을 수행
    @RequestMapping("/hotel-embedding")
    public String hotelEmbedding(@RequestParam("prompt") String userPrompt, String section, String name) {
        log.info(userPrompt);
        return hotelEmbeddingModelService.similaritySearch(userPrompt, section, name).get(0).getText();
    }

    // 2. Hotel Data Embedding - add
    @RequestMapping("/add-hotels")
    public String addHotels() {
        return hotelEmbeddingModelService.addData();
    }

    // 2. Hotel Data Embedding - delete
    @RequestMapping("/delete-hotels")
    public String deleteHotels() {
        return hotelEmbeddingModelService.deleteDate();
    }

    // 3. Chat Memory PGvector
    // 사용자와 LLM의 대화 내역을 실시간 Embedding하여 PGVector(PostgreSQL Vector Extension)에 저장하고, 후속 대화에서 유사한 이전 대화를 검색해 콘텍스트로 활용하는 방식
    @RequestMapping("/chat-pgvector")
    public String chatPgvector(@RequestParam("prompt") String userPrompt, HttpSession  session) {
        return chatPgvectorService.chat(userPrompt, session.getId());
    }

    // 3. Chat Memory PGvector - delete
    @RequestMapping("/delete-chat-pgvector")
    public String deleteChatPgvector(HttpSession  session) {
        return chatPgvectorService.deleteChat(session.getId());
    }

    // 3. Chat Memory PGvector - delete all
    @RequestMapping("/delete-all-chat-pgvector")
    public String deleteAllChatPgvector() {
        return chatPgvectorService.deleteAllChat();
    }

    // 4. Chat Memory JDBC
    // 사용자와 LLM의 대화 흐름을 관계형 데이터베이스(JDBC) 기반으로 저장하는 방식. Embedding 없이 텍스트 그대로 저장하여 대화 이력 관리가 필요한 경우에 사용
    @RequestMapping("/chat-jdbc")
    public String chatJdbc(@RequestParam("prompt") String userPrompt, HttpSession  session) {
        return chatJdbcService.chat(userPrompt, session.getId());
    }

    // 4. Chat Memory JDBC - delete
    @RequestMapping("/delete-chat-jdbc")
    public String deleteChatJdbc(HttpSession  session) {
        return chatJdbcService.deleteChat(session.getId());
    }

    // 4. Chat Memory JDBC - delete all
    @RequestMapping("/delete-all-chat-jdbc")
    public String deleteAllChatJdbc() {
        return chatJdbcService.deleteAllChat();
    }

}
