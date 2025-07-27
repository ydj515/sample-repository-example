package com.example.webfluxwithmongoexample.domain.user;

import com.example.webfluxwithmongoexample.domain.user.service.UserCommand;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    private String id;
    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public User(UserCommand userCommand) {
        this.name = userCommand.getName();
        this.age = userCommand.getAge();
    }
}