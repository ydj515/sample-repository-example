package com.example.timescaledbapistatsexample.domain.service

import com.example.timescaledbapistatsexample.domain.model.ApiRoute

class RoutePatternMatcher(routes: List<ApiRoute>) {
    private val compiledRoutes = routes
        .map { route -> CompiledRoute(route = route, regex = route.pathPattern.toRegexPattern()) }
        .sortedWith(
            compareByDescending<CompiledRoute> { it.route.pathPattern.count { char -> char == '/' } }
                .thenBy { it.route.pathPattern.count { char -> char == '{' } },
        )

    fun find(method: String, path: String): ApiRoute? {
        val normalizedMethod = method.uppercase()
        val normalizedPath = path.trimEnd('/').ifBlank { "/" }
        return compiledRoutes.firstOrNull { compiled ->
            compiled.route.method == normalizedMethod && compiled.regex.matches(normalizedPath)
        }?.route
    }

    private data class CompiledRoute(
        val route: ApiRoute,
        val regex: Regex,
    )

    private fun String.toRegexPattern(): Regex {
        val regex = split("/")
            .joinToString("/") { segment ->
                if (segment.startsWith("{") && segment.endsWith("}")) "[^/]+" else Regex.escape(segment)
            }
        return Regex("^$regex$")
    }
}
