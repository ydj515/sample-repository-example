package com.example.webfluxwithredisexample.application;

import com.example.webfluxwithredisexample.domain.StringModel;
import com.example.webfluxwithredisexample.infrastructure.repository.StringRepository;
import com.example.webfluxwithredisexample.presentation.router.string.MultiStringRequest;
import com.example.webfluxwithredisexample.presentation.router.string.RawStringRequest;
import com.example.webfluxwithredisexample.presentation.router.string.StringRequest;
import com.example.webfluxwithredisexample.presentation.router.string.StringDeltaRequest;
import com.example.webfluxwithredisexample.presentation.router.string.StringResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StringAsyncService {
    private final StringRepository redis;

    public Mono<Void> set(StringRequest req) {
        String key = req.baseRequest().key();
        StringModel newModel = new StringModel(key, req.name());

        return redis.setData(key, newModel)
                .then();
    }

    public Mono<StringResponse> get(String key) {
        return redis.getData(key, StringModel.class)
                .map(result -> {
                    List<StringModel> res = new ArrayList<>();
                    if (result != null) {
                        res.add(result);
                    }
                    return new StringResponse(res);
                })
                .switchIfEmpty(Mono.just(new StringResponse(new ArrayList<>())));
    }

    public Mono<Void> multiSet(MultiStringRequest req) {
        Map<String, Object> dataMap = new HashMap<>();

        for (int i = 0; i < req.names().length; i++) {
            String name = req.names()[i];
            String key = "key:" + (i + 1);

            StringModel newModel = new StringModel(key, name);
            dataMap.put(key, newModel);
        }

        return redis.multiSetData(dataMap)
                .then();
    }

    public Mono<Void> setRaw(RawStringRequest req) {
        return redis.setRaw(req.baseRequest().key(), req.value(), toDuration(req.ttlSeconds()))
                .then();
    }

    public Mono<String> getRaw(String key) {
        return redis.getRaw(key);
    }

    public Mono<List<String>> multiGet(List<String> keys) {
        return redis.multiGetRaw(keys);
    }

    public Mono<Long> increment(StringDeltaRequest req) {
        return redis.increment(req.baseRequest().key(), req.delta());
    }

    public Mono<Long> decrement(StringDeltaRequest req) {
        return redis.decrement(req.baseRequest().key(), req.delta());
    }

    public Mono<Duration> ttl(String key) {
        return redis.getExpire(key);
    }

    public Mono<Boolean> setIfAbsent(RawStringRequest req) {
        return redis.setIfAbsent(req.baseRequest().key(), req.value(), toDuration(req.ttlSeconds()));
    }

    private Duration toDuration(Long ttlSeconds) {
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return null;
        }

        return Duration.ofSeconds(ttlSeconds);
    }
}
