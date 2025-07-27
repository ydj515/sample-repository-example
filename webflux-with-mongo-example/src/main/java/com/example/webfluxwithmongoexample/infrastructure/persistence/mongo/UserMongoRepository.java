package com.example.webfluxwithmongoexample.infrastructure.persistence.mongo;

import com.example.webfluxwithmongoexample.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class UserMongoRepository {
    private final ReactiveMongoTemplate mongoTemplate;

    public Mono<User> save(User user) {
        return mongoTemplate.save(user);
    }

    public Mono<User> findOne(String id) {
        return mongoTemplate.findById(id, User.class);
    }

    public Flux<User> findAll() {
        return mongoTemplate.findAll(User.class);
    }
}
