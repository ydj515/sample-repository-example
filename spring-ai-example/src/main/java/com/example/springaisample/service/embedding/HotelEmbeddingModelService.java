package com.example.springaisample.service.embedding;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class HotelEmbeddingModelService {
  private final VectorStore vectorStore;

  List<Document> documents = List.of(
          new Document("호텔 입실 시간은 오후 3시 입니다.", Map.of("section", "regulation","name","hotel1")),
          new Document("호텔 퇴실 시간은 오전 11시 입니다.", Map.of("section", "regulation","name","hotel1")),
          new Document("호텔 입실 시간은 오후 2시 입니다.", Map.of("section", "regulation","name","hotel2")),
          new Document("호텔 퇴실 시간은 오전 12시 입니다.", Map.of("section", "regulation","name","hotel2")),
          new Document("호텔 조식 시간은 오전 7시부터 오전 9시까지 입니다.", Map.of("section", "restaurant","name","hotel1")),
          new Document("호텔 석식 시간은 오후 6시부터 오후 9시까지 입니다.", Map.of("section", "restaurant","name","hotel1")),
          new Document("호텔 주변 관광지는 설악산 국립공원이 있습니다.", Map.of("section", "additional","name","hotel1")),
          new Document("호텔 주변 맛집은 순두부집이 있습니다.", Map.of("section", "additional","name","hotel1")));

    // Constructor
    public HotelEmbeddingModelService(VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  public String addData(){
    vectorStore.add(documents);
    return " Add Completed";
  }

  public String deleteDate(){
    vectorStore.delete("name == 'hotel1' or name == 'hotel2'");
    return "Delete Completed ";
  }

  public List<Document> similaritySearch(String question){
    return vectorStore.similaritySearch(question);
  }

  public List<Document> similaritySearch(String question, String section, String name){

    return vectorStore.similaritySearch(
            SearchRequest.builder()
                    .query(question)
                    .topK(1)
                    .similarityThreshold(0.5)
                    .filterExpression("section == '%s' and name == '%s'".formatted(section,name)).build());
  }

  // filterExpression
  public List<Document> similaritySearch(String question, String director, int year){
    FilterExpressionBuilder b = new FilterExpressionBuilder();

      return vectorStore.similaritySearch(
              SearchRequest.builder()
                      .query(question)
                      .topK(1) // → 벡터스토어에서 가져올 후보 개수
                      .similarityThreshold(0.5) // → 유사도 점수 기준 미달 결과 제거
                      .filterExpression(b.and(b.eq("director", director), b.gte("year", year)).build()) // → 벡터스토어 검색 조건에 포함됨, 메타데이터 필터
                      .build());
                      //.filterExpression("derector  == '%s' and year >= '%s'".formatted(director,year)).build());
  }

}
