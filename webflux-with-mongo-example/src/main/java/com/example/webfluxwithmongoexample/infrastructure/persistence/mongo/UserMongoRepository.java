package com.example.webfluxwithmongoexample.infrastructure.persistence.mongo;

import com.example.webfluxwithmongoexample.domain.user.User;
import com.example.webfluxwithmongoexample.domain.user.service.UserCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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

    public Flux<User> findAllByCommand(UserCommand command) {
        Query query = new Query();
        if (command.getName() != null && !command.getName().isEmpty()) {
            query.addCriteria(Criteria.where("name").is(command.getName()));
        }
        if (command.getAge() != 0) { // Assuming 0 means age is not specified for filtering
            query.addCriteria(Criteria.where("age").is(command.getAge()));
        }
        return mongoTemplate.find(query, User.class);
    }

    public Mono<Void> deleteAll() {
        return mongoTemplate.remove(new Query(), User.class).then();
    }
}
