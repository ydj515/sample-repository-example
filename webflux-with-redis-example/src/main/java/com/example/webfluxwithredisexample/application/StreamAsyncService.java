package com.example.webfluxwithredisexample.application;

import com.example.webfluxwithredisexample.infrastructure.repository.StreamRepository;
import com.example.webfluxwithredisexample.presentation.router.stream.StreamAckRequest;
import com.example.webfluxwithredisexample.presentation.router.stream.StreamAddRequest;
import com.example.webfluxwithredisexample.presentation.router.stream.StreamEntryResponse;
import com.example.webfluxwithredisexample.presentation.router.stream.StreamGroupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class StreamAsyncService {
    private final StreamRepository redis;

    public Mono<String> add(StreamAddRequest req) {
        return redis.add(req.baseRequest().key(), req.fields());
    }

    public Mono<Long> size(String key) {
        return redis.size(key);
    }

    public Flux<StreamEntryResponse> read(String key, String offset, long count) {
        return redis.read(key, offset, count);
    }

    public Mono<String> createGroup(StreamGroupRequest req) {
        String offset = req.offset() == null || req.offset().isBlank() ? "0-0" : req.offset();
        return redis.createGroup(req.baseRequest().key(), offset, req.group());
    }

    public Flux<StreamEntryResponse> readGroup(String key, String group, String consumer, long count) {
        return redis.readGroup(key, group, consumer, count);
    }

    public Mono<Long> acknowledge(StreamAckRequest req) {
        return redis.acknowledge(req.baseRequest().key(), req.group(), req.recordId());
    }
}
