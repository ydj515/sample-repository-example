package com.example.timescaledbapistatsexample.domain.service

import java.security.MessageDigest

object Sha256ApiKeyHasher {
    fun hash(apiKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(apiKey.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}
