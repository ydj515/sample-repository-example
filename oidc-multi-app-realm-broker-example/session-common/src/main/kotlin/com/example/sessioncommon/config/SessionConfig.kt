package com.example.sessioncommon.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession

@Configuration
@ConditionalOnProperty(
    name = ["app.session.redis-enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableRedisIndexedHttpSession(redisNamespace = "oidc-multi-app-realm-broker-example:session")
class SessionConfig
