package com.example.oidcsimpleexample.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession

@Configuration
@ConditionalOnProperty(
    name = ["app.session.redis-enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableRedisIndexedHttpSession(
    redisNamespace = "oidc-example:session",
)
class SessionConfig
