package com.example.springaisample.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class RagEtlPipelineService {

    private final VectorStore vectorStore;

    // Constructor
    public RagEtlPipelineService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public String clearVectorStore(String type) {
        vectorStore.delete("type == '%s'".formatted(type));
        return "cleared: " + type;
    }

    public String addVectorStore(String type, MultipartFile attach) throws IOException {
        // MultipartFile에서 Document를 생성, contentType에 따라 변환
        List<Document> documents = textExtraction(attach, Objects.requireNonNull(attach.getContentType()));
        if (documents == null) {
            return "파일을 입력 하세요";
        }
        // Document 개수확인
        log.info("생성된 Document 수: {} 개", documents.size());

        for (Document doc : documents) {
            doc.getMetadata().put("type", type);
            doc.getMetadata().put("name", attach.getOriginalFilename());
        }

        // Document를 Split
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
        List<Document> transformedDocuments = tokenTextSplitter.apply(documents);
        // Split된 개수 확인
        log.info("Split 된 Document 수: {} 개", transformedDocuments.size());

        // Vector Store에 저장
        // 이때 Pgvector에 자동적으로 생성된 "vector_store" 테이블에 저장
        vectorStore.add(transformedDocuments);

        return "ETL완료";
    }

    private List<Document> textExtraction(MultipartFile attach, String contentType) throws IOException {

        Resource resource = new ByteArrayResource(attach.getBytes());
        List<Document> documents = null;

        switch (contentType) {
            case "text/plain":
                documents = new TextReader(resource).read();
                break;
            case "application/pdf":
                documents = new PagePdfDocumentReader(resource).read();
                break;
            case "wordprocessingml":
                documents = new TikaDocumentReader(resource).read();
                break;
            default: break;
        }

        return documents;
    }
}
