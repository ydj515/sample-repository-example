package com.example.webfluxwithmongoexample.domain.user.service;

import com.example.webfluxwithmongoexample.TestcontainersConfiguration;
import com.example.webfluxwithmongoexample.domain.user.User;
import com.example.webfluxwithmongoexample.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll().block();
    }

    @Test
    void testGetAllUsersWithFiltering() {
        // Given
        User user1 = new User("John Doe", 30);
        User user2 = new User("Jane Doe", 25);
        User user3 = new User("John Smith", 30);

        userRepository.save(user1).block();
        userRepository.save(user2).block();
        userRepository.save(user3).block();

        // When: Filter by name "John Doe"
        UserCommand command1 = new UserCommand("John Doe", 0);
        StepVerifier.create(userService.getAllUsers(command1))
                .expectNextMatches(userInfo -> userInfo.getName().equals("John Doe"))
                .verifyComplete();

        // When: Filter by age 30
        UserCommand command2 = new UserCommand(null, 30);
        StepVerifier.create(userService.getAllUsers(command2))
                .expectNextCount(2) // John Doe and John Smith
                .verifyComplete();

        // When: Filter by name "John" and age 30
        UserCommand command3 = new UserCommand("John Doe", 30);
        StepVerifier.create(userService.getAllUsers(command3))
                .expectNextMatches(userInfo -> userInfo.getName().equals("John Doe"))
                .verifyComplete();

        // When: No filter (all users)
        UserCommand command4 = new UserCommand(null, 0);
        StepVerifier.create(userService.getAllUsers(command4))
                .expectNextCount(3)
                .verifyComplete();
    }
}
