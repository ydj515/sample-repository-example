package com.example.webfluxwithredisexample.presentation.router.sortedset;

import com.example.webfluxwithredisexample.application.SortedSetAsyncService;
import com.example.webfluxwithredisexample.domain.SortedSetModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class SortedSetHandler {
    private final SortedSetAsyncService sortedSetAsyncService;

    public Mono<ServerResponse> setSortedSet(ServerRequest request) {
        return request.bodyToMono(SortedSetRequest.class)
                .flatMap(sortedSetAsyncService::setSortedSet)
                .then(ServerResponse.ok().build());
    }

    public Mono<ServerResponse> getSortedSet(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        Double min = Double.parseDouble(request.queryParam("min").orElse("0"));
        Double max = Double.parseDouble(request.queryParam("max").orElse("100"));

        return sortedSetAsyncService.getSetDataByRange(key, min, max)
                .flatMap(sortedSet -> ServerResponse.ok().body(Flux.fromIterable(sortedSet), SortedSetModel.class));
    }

    public Mono<ServerResponse> getTopN(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        Integer n = Integer.parseInt(request.queryParam("n").orElse("10"));

        return sortedSetAsyncService.getTopN(key, n)
                .flatMap(topN -> ServerResponse.ok().body(Flux.fromIterable(topN), SortedSetModel.class));
    }

    public Mono<ServerResponse> getRank(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        String member = request.queryParam("member").orElse("");
        return ServerResponse.ok().body(sortedSetAsyncService.rank(key, member), Long.class);
    }

    public Mono<ServerResponse> getReverseRank(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        String member = request.queryParam("member").orElse("");
        return ServerResponse.ok().body(sortedSetAsyncService.reverseRank(key, member), Long.class);
    }

    public Mono<ServerResponse> getScore(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        String member = request.queryParam("member").orElse("");
        return ServerResponse.ok().body(sortedSetAsyncService.score(key, member), Double.class);
    }

    public Mono<ServerResponse> incrementScore(ServerRequest request) {
        return request.bodyToMono(SortedSetDeltaRequest.class)
                .flatMap(sortedSetAsyncService::incrementScore)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    public Mono<ServerResponse> addWithOption(ServerRequest request) {
        return request.bodyToMono(SortedSetConditionalAddRequest.class)
                .flatMap(sortedSetAsyncService::addWithOption)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }
}
