package com.example.webfluxwithmongoexample.infrastructure.user;

import com.example.webfluxwithmongoexample.domain.user.User;
import com.example.webfluxwithmongoexample.domain.user.repository.UserRepository;
import com.example.webfluxwithmongoexample.infrastructure.persistence.mongo.UserMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMongoRepository userMongoRepository;

    @Override
    public Mono<User> save(User user) {
        return userMongoRepository.save(user);
    }

    @Override
    public Mono<User> findById(String id) {
        return userMongoRepository.findOne(id);
    }

    @Override
    public Flux<User> findAll() {
        return userMongoRepository.findAll();
    }
}
