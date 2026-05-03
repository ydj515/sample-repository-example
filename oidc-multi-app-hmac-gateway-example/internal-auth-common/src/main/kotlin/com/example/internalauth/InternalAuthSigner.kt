package com.example.internalauth

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class InternalAuthSigner(
    secret: String,
) {
    private val signingKey = secret.toByteArray(StandardCharsets.UTF_8)

    init {
        require(secret.isNotBlank()) { "internal auth secret must not be blank" }
    }

    fun sign(payload: InternalAuthPayload): String {
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(SecretKeySpec(signingKey, HMAC_SHA256))
        val raw = mac.doFinal(payload.canonicalValue().toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw)
    }

    fun verify(
        payload: InternalAuthPayload,
        signature: String,
    ): Boolean {
        val expected = sign(payload)
        return MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.UTF_8),
            signature.toByteArray(StandardCharsets.UTF_8),
        )
    }

    private companion object {
        private const val HMAC_SHA256 = "HmacSHA256"
    }
}
