package com.mahdiMb55.prices.data.remote

import okhttp3.Interceptor
import okhttp3.Response

interface AccessTokenProvider {
    fun currentToken(): String?
}

interface MutableAccessTokenStore : AccessTokenProvider {
    fun updateToken(token: String?)
    fun clear()
}

object NoTokenAccessTokenProvider : AccessTokenProvider {
    override fun currentToken(): String? = null
}

class InMemoryAccessTokenProvider(initialToken: String? = null) : MutableAccessTokenStore {
    @Volatile
    private var token: String? = initialToken

    override fun currentToken(): String? = token

    override fun updateToken(token: String?) {
        this.token = token
    }

    fun update(token: String?) = updateToken(token)

    override fun clear() { token = null }

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
