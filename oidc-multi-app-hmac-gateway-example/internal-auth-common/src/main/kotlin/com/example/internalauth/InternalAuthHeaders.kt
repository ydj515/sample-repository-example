package com.example.internalauth

object InternalAuthHeaders {
    const val APP_ID = "X-Internal-Auth-App"
    const val SESSION_ID = "X-Internal-Auth-Session"
    const val ISSUED_AT = "X-Internal-Auth-Iat"
    const val METHOD = "X-Internal-Auth-Method"
    const val PATH = "X-Internal-Auth-Path"
    const val SIGNATURE = "X-Internal-Auth-Signature"

    val all: List<String> = listOf(
        APP_ID,
        SESSION_ID,
        ISSUED_AT,
        METHOD,
        PATH,
        SIGNATURE,
    )
}
