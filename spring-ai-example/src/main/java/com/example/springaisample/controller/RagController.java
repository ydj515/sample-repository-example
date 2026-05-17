package com.example.springaisample.controller;

import com.example.springaisample.service.rag.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/rag")
@Slf4j
@RequiredArgsConstructor
// Chapter 8. RAG (Retrieval-Augmented Generation) Controller
public class  RagController {

    // 1. ETL Pipeline - add / delete
    private final RagEtlPipelineService ragEtlPipelineService;
    // 2. RAG Chat
    private final RagChatService ragChatService;
    // 3. RAG Chat: template
    private final RagChatPromptTemplateService ragChatPromptTemplateService;
    // 4. Retrieval Augmentation Advisor
    private final RagRetrievalAugmentationAdvisorService ragRetrievalAugmentationAdvisorService;
    // 5. Compression Query Transformer
    private final RagCompressionQueryTransformerService ragCompressionQueryTransformerService;
    // 6. Rewrite Query Transformer
    private final RagRewriteQueryTransformerService ragRewriteQueryTransformerService;
    // 7. Translation Query Transformer
    private final RagTranslationQueryTransformerService ragTranslationQueryTransformerService;
    // 8. Multi Query Expander
    private final RagMultiQueryExpanderService ragMultiQueryExpanderService;

    // 1. ETL Pipeline - add
    // 사용자가 업로드한 파일(TXT, PDF, DOC 등)을 처리하여 Vector Store에 저장하는 기능. 각 파일의 타입을 지정하여 데이터 구분 및 관리가 가능
    @RequestMapping("/add-vector-store")
    public String addDocument(@RequestParam("type") String type,
                              @RequestParam(value="attach", required = false) MultipartFile attach) throws IOException {
        log.info("addDocument {} {}", type, attach.getOriginalFilename());
        return ragEtlPipelineService.addVectorStore(type, attach);
    }

    // 1. ETL Pipeline - delete
    // 사용자가 업로드한 파일(TXT, PDF, DOC 등)을 처리하여 Vector Store에 삭제하는 기능. 각 파일의 타입을 지정하여 데이터 구분 및 관리가 가능
    @RequestMapping("/clear-vector-store")
    public String deleteAllDocument(@RequestParam("type") String type) {
        log.info("deleteAllDocument");
        return ragEtlPipelineService.clearVectorStore(type);
    }

    // 2. RAG Chat
    // Vector Store에 저장된 데이터 기반으로 LLM과 대화하며, QuestionAnswerAdvisor를 활용하여 RAG(Retrieval-Augmented Generation) 기반 질문 응답을 수행
    @RequestMapping("/rag-chat")
    public Flux<String> ragChat(@RequestParam("type") String type, @RequestParam("prompt") String question) {
        log.info("ragChat: {}", question);
        return ragChatService.ragChat(question, type);
    }

    // 3. RAG Chat: template
    // RAG Chat 기능에 PromptTemplate을 적용하여, 검색된 문서를 기반으로 LLM이 생성한 답변을 지정된 형식으로 가공하여 전달
    @RequestMapping("/rag-chat-prompt-template")
    public Flux<String> ragChatPromptTemplate(@RequestParam("type") String type, @RequestParam("prompt") String question) {
        log.info("ragChat: {} {}", question, type);
        return ragChatPromptTemplateService.ragChat(question, type);
    }

    // 4. Retrieval Augmentation Advisor
    // RetrievalAugmentationAdvisor를 사용하여, Vector Store에 정확한 검색 결과가 없어도 LLM이 유연하게 대화를 이어갈 수 있도록 지원
    @RequestMapping("/raa-rag-chat")
    public Flux<String> retrievalAugmentationAdvisorChat(@RequestParam("type") String type, @RequestParam("prompt") String question) {
        log.info("ragChat: {}", question);
        return ragRetrievalAugmentationAdvisorService.ragChat(question, type);
    }

    // 5. Compression Query Transformer
    // CompressionQueryTransformer는 대화 기록과 후속 질문을 압축하여, 화 핵심을 포착한 독립적인 쿼리로 재구성한 후 LLM과 대화할 수 있도록 지원
    @RequestMapping("/cqt-rag-chat")
    public Flux<String> compressionQueryTransformer(@RequestParam("type") String type, @RequestParam("prompt") String question, HttpSession session) {
        log.info("ragChat: {}", question);
        return ragCompressionQueryTransformerService.ragChat(question, type, session.getId());
    }

    // 6. Rewrite Query Transformer
    // RewriteQueryTransformer는 사용자의 질문이 장황하거나 모호할 때, 질문을 명확하게 재작성하여 LLM이 정확히 이해하고 응답할 수 있도록 지원
    @RequestMapping("/rqt-rag-chat")
    public Flux<String> rewriteQueryTransformer(@RequestParam("type") String type, @RequestParam("prompt") String question, HttpSession session) {
        log.info("ragChat: {}", question);
        return ragRewriteQueryTransformerService.ragChat(question, type, session.getId());
    }

    // 7. Translation Query Transformer
    // TranslationQueryTransformer는 사용자의 질문을 특정 언어로 번역하여(입력 언어 → 지정 언어로 자동 변환) LLM과 다국어 대화를 가능하게 하는 기능
    @RequestMapping("/tqt-rag-chat")
    public Flux<String> translationQueryTransformer(@RequestParam("type") String type, @RequestParam("prompt") String question, HttpSession session) {
        log.info("ragChat: {}", question);
        return ragTranslationQueryTransformerService.ragChat(question, type, session.getId());
    }

    // 8. Multi Query Expander
    // MultiQueryExpander는 사용자의 질문을 다양한 변형(Query Expansion)으로 만들어 LLM과 대화할 수 있도록 확장하는 기능
    @RequestMapping("/mqe-rag-chat")
    public Flux<String> multiQueryExpander(@RequestParam("type") String type, @RequestParam("prompt") String question, HttpSession session) {
        log.info("ragChat: {}", question);
        return ragMultiQueryExpanderService.ragChat(question, type, session.getId());
    }
}
