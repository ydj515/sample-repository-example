package com.example.springaisample.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatPlaygroundService {

    private final ChatClient chatClient;

    public ChatPlaygroundService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.clone().build();
    }

    public String chat(String question) {
        return this.chatClient.prompt()
                .system("질문에 대한 답변을 한국어로 친절하게 답변해야 합니다.")
                .user(question)
                .call()
                .content();
    }

    public Flux<String> chatStream(String question) {
        return this.chatClient.prompt()
                .system("질문에 대한 답변을 한국어로 친절하게 답변해야 합니다.")
                .user(question)
                .stream()
                .content();
    }

    public Flux<String> chatChainOfThought(String question) {
        String userText = """
                %s
                위의 질문을 단계별로 해결해 봅시다.

                [예시]
                질문:
                연비가 1리터에 10Km인 자동차를 가지고
                서울에서 부산까지 왕복 할때 사용되는 총 소요 비용과 총 소요 시간은 얼마 일까요
                서울에서 부산까지의 거리는 약 500km이고, 평균 속도가 시속 100km라고 가정합니다.
                현재 기름의 가격은 1리터에 1500원 입니다.
                편도 톨게이트 비용은 3만원 입니다.

                답변:
                1단계: "서울에서 부산까지 시속 100km의 속도로 이동하는 데 걸리는 시간을 계산해 보겠습니다. 시간은 약 5시간 입니다."
                2단계: "1리터에 10km를 운행 할 수 있으니 500km 운행에 필요한 기름은 50리터 입니다."
                3단계: "50리터를 금액으로 환산 하면 75000원 입니다."
                4단계: "따라서 총 운행 시간은 왕복 10시간이며 소요되는 기름의 양은 100리터 입니다."
                5단계: "서울에서 부산까지 왕복 하는데 사용되는 총 비용은 왕복 기름값 150000원과 톨게이트 비용 60000원을 합친 210000원이며 소요 시간은 총 10시간 입니다."
                """.formatted(question);

        return this.chatClient.prompt()
                .user(userText)
                .stream()
                .content();
    }

    public String chatFewShot(String question) {
        String userText = """
                사용자가 요청한 명소 또는 맛집 정보를 JSON 형식으로 만들어 줍니다.
                아래 규칙을 반드시 지켜주세요:
                1. 요청한 정보에 대해 10개의 정보를 조회 합니다.
                2. 맛집인 경우 메뉴는 5개 이상 알려줘
                3. 응답은 특수문자 형식 없이 JSON 문자열로만 반환해야 합니다.

                예시1:
                서울 종로에 맛집 정보 알려줘
                JSON 응답:
                [
                  {
                    "name": "옥순당",
                    "menu": ["감자탕", "김치찌개", "제육볶음", "된장찌개", "계란말이"],
                    "address": "서울시 중구 371번지",
                    "lat": 37.4985,
                    "lng": 127.0300
                  }
                ]

                예시2:
                서울 종로에 숙박업소 정보 알려줘
                JSON 응답:
                [
                  {
                    "name": "옥순장",
                    "address": "서울시 중구 371번지",
                    "lat": 37.4985,
                    "lng": 127.0300
                  }
                ]

                고객 주문: %s
                """.formatted(question);

        return this.chatClient.prompt()
                .user(userText)
                .call()
                .content();
    }
}
