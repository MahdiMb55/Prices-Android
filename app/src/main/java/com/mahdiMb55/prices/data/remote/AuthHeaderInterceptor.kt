package com.mahdiMb55.prices.data.remote

import okhttp3.Interceptor
import okhttp3.Response

interface AccessTokenProvider {
    fun currentToken(): String?
}

object NoTokenAccessTokenProvider : AccessTokenProvider {
    override fun currentToken(): String? = null
}

class InMemoryAccessTokenProvider(initialToken: String? = null) : AccessTokenProvider {
    @Volatile
    private var token: String? = initialToken

    override fun currentToken(): String? = token

    fun update(token: String?) {
        this.token = token
    }

    override fun toString(): String = "InMemoryAccessTokenProvider(redacted)"
}

class AuthHeaderInterceptor(
    private val accessTokenProvider: AccessTokenProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header("Authorization") != null) return chain.proceed(request)

        val token = accessTokenProvider.currentToken()?.takeIf(String::isNotBlank)
            ?: return chain.proceed(request)
        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        )
    }
}
