package com.example.webfluxwithredisexample.presentation.router.usecase.popularity;

import static com.example.webfluxwithredisexample.presentation.router.usecase.UsecaseResponses.ok;

import com.example.webfluxwithredisexample.application.usecase.popularity.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;

import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PopularityHandler {
    private final WishlistService wishlist;

    private final RankingService ranking;

    private final ProductVisitService visits;

    public Mono<ServerResponse> wish(ServerRequest req) {
        return ok(wishlist.add(req.pathVariable("userId"), req.pathVariable("productId"))
                .map(count -> Map.of("added", count))
        );
    }

    public Mono<ServerResponse> wishes(ServerRequest req) {
        return ok(wishlist.get(req.pathVariable("userId")).collectList());
    }

    public Mono<ServerResponse> score(ServerRequest req) {
        return ok(ranking.increment(req.pathVariable("productId"))
                .map(score -> Map.of("score", score))
        );
    }

    public Mono<ServerResponse> top(ServerRequest req) {
        return ok(ranking.top(Integer.parseInt(req.queryParam("limit").orElse("10"))).collectList());
    }

    public Mono<ServerResponse> visit(ServerRequest req) {
        return ok(visits.record(
                        req.pathVariable("productId"),
                        Long.parseLong(req.pathVariable("visitorId")))
                .map(count -> Map.of("estimatedUniqueVisitors", count))
        );
    }

    public Mono<ServerResponse> visited(ServerRequest req) {
        return ok(visits.visited(
                        req.pathVariable("productId"),
                        Long.parseLong(req.pathVariable("visitorId")))
                .map(value -> Map.of("visited", value))
        );
    }
}
