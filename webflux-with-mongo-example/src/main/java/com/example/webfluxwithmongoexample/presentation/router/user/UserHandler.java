package com.example.webfluxwithmongoexample.presentation.router.user;

import com.example.webfluxwithmongoexample.application.facade.user.UserCriteria;
import com.example.webfluxwithmongoexample.application.facade.user.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserHandler {

    private final UserFacade userFacade;

    public Mono<ServerResponse> createUser(ServerRequest request) {
        return request.bodyToMono(UserRequest.class)
                .flatMap(userRequest -> userFacade.createUser(
                        new UserCriteria(userRequest.getName(), userRequest.getAge())
                ))
                .flatMap(userResult -> {
                    UserResponse userResponse = new UserResponse(userResult.getName());
                    return ServerResponse.status(HttpStatus.CREATED).bodyValue(userResponse);
                })
                .switchIfEmpty(ServerResponse.status(HttpStatus.BAD_REQUEST).build());
    }

    public Mono<ServerResponse> getUserById(ServerRequest request) {
        String id = request.pathVariable("id");
        return userFacade.findById(id)
                .flatMap(userResult -> {
                    UserResponse userResponse = new UserResponse(userResult.getName());
                    return ServerResponse.ok().bodyValue(userResponse);
                })
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> getAllUsers(ServerRequest request) {
        String name = request.queryParam("name").orElse(null);
        Integer age = request.queryParam("age")
                .map(Integer::parseInt)
                .orElse(null);

        UserCriteria criteria = new UserCriteria(name, age);
        return userFacade.getAllUsers(criteria)
                .collectList()
                .flatMap(users -> ServerResponse.ok().bodyValue(users))
                .switchIfEmpty(ServerResponse.noContent().build());
    }
}
