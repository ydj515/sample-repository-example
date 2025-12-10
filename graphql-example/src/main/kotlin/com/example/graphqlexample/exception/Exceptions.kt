package com.example.graphqlexample.exception

class NotFoundException(message: String) : RuntimeException(message)
class InvalidInputException(message: String) : RuntimeException(message)
