package com.example.webfluxwithmongoexample.application.facade.user;

import com.example.webfluxwithmongoexample.domain.user.service.UserCommand;
import com.example.webfluxwithmongoexample.domain.user.service.UserInfo;
import com.example.webfluxwithmongoexample.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;

    public Mono<UserResult> createUser(UserCriteria criteria) {
        UserCommand userCommand = new UserCommand(criteria);
        Mono<UserInfo> userInfo = userService.save(userCommand);

        return userInfo.map(UserResult::new);
    }

    public Flux<UserResult> getAllUsers(UserCriteria criteria) {
        UserCommand userCommand = new UserCommand(criteria);
        Flux<UserInfo> usersInfo = userService.getAllUsers(userCommand);

        return usersInfo.map(UserResult::new);
    }

    public Mono<UserResult> findById(String id) {
        return userService.findById(id).map(UserResult::new);
    }
}
