package com.example.webfluxwithredisexample.application;

import com.example.webfluxwithredisexample.infrastructure.repository.LuaRepository;
import com.example.webfluxwithredisexample.presentation.router.lua.LuaEvalShaRequest;
import com.example.webfluxwithredisexample.presentation.router.lua.LuaStockRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class LuaAsyncService {
    private final LuaRepository redis;

    public Mono<Long> decreaseStock(LuaStockRequest req) {
        return redis.decreaseStock(req.baseRequest().key(), req.quantity());
    }

    public Mono<String> loadScript() {
        return redis.loadStockScript();
    }

    public Mono<Long> decreaseStockBySha(LuaEvalShaRequest req) {
        return redis.decreaseStockBySha(req.sha(), req.baseRequest().key(), req.quantity());
    }
}
