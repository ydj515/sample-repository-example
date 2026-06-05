package com.example.webfluxwithredisexample.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
@Repository
public class LuaRepository {
    private final ReactiveRedisConnectionFactory connectionFactory;

    public Mono<Long> decreaseStock(String key, long quantity) {
        return Mono.usingWhen(
                Mono.fromSupplier(connectionFactory::getReactiveConnection),
                connection -> connection.scriptingCommands()
                        .eval(
                                scriptBuffer(),
                                ReturnType.INTEGER,
                                1,
                                toBuffer(key),
                                toBuffer(Long.toString(quantity))
                        )
                        .single()
                        .map(value -> ((Number) value).longValue()),
                ReactiveRedisConnection::closeLater
        );
    }

    public Mono<String> loadStockScript() {
        return Mono.usingWhen(
                Mono.fromSupplier(connectionFactory::getReactiveConnection),
                connection -> connection.scriptingCommands().scriptLoad(scriptBuffer()),
                ReactiveRedisConnection::closeLater
        );
    }

    public Mono<Long> decreaseStockBySha(String sha, String key, long quantity) {
        return Mono.usingWhen(
                Mono.fromSupplier(connectionFactory::getReactiveConnection),
                connection -> connection.scriptingCommands()
                        .evalSha(
                                sha,
                                ReturnType.INTEGER,
                                1,
                                toBuffer(key),
                                toBuffer(Long.toString(quantity))
                        )
                        .single()
                        .map(value -> ((Number) value).longValue()),
                ReactiveRedisConnection::closeLater
        );
    }

    private ByteBuffer scriptBuffer() {
        return ByteBuffer.wrap(readScriptBytes());
    }

    private ByteBuffer toBuffer(String value) {
        return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] readScriptBytes() {
        try {
            return new ClassPathResource("lua/stock.lua").getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load lua/stock.lua", e);
        }
    }
}
