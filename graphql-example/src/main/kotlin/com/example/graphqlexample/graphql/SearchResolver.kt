package com.example.graphqlexample.graphql

import com.example.graphqlexample.exception.InvalidInputException
import com.example.graphqlexample.graphql.dto.SearchResult as SearchResultDto
import com.example.graphqlexample.service.ProductService
import com.example.graphqlexample.service.UserService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class SearchResolver(
    private val userService: UserService,
    private val productService: ProductService,
    private val mapper: GraphqlMapper
) {
    @QueryMapping
    fun search(@Argument keyword: String): List<SearchResultDto> {
        if (keyword.isBlank()) {
            throw InvalidInputException("keyword must not be blank")
        }
        val users = userService.search(keyword)
        val products = productService.search(keyword)
        return (users + products).map { mapper.toSearchResultDto(it) }
    }
}
