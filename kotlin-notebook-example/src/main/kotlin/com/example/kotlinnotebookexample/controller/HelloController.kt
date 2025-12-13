package com.example.kotlinnotebookexample.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class Greeting(
    val message: String,
    val caller: String? = null
)

@RestController
class HelloController {

    @GetMapping("/hello")
    fun hello(): Greeting {
        return Greeting(message = "hello from kotlin notebook spring!")
    }

    @GetMapping("/hello-with-name")
    fun helloWithName(@RequestParam(required = false) name: String?): Greeting {
        return Greeting(
            message = "hello!",
            caller = name
        )
    }

    @GetMapping("/hello/{id}")
    fun helloById(@PathVariable id: Long): Greeting {
        return Greeting(message = "hello, id = $id")
    }
}