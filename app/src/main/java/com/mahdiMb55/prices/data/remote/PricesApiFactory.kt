package com.mahdiMb55.prices.data.remote

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object NetworkTimeouts {
    const val ConnectSeconds = 15L
    const val ReadSeconds = 20L
    const val WriteSeconds = 20L
    const val CallSeconds = 30L
}

object PricesNetworkJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
}

class PricesApiFactory(
    private val accessTokenProvider: AccessTokenProvider,
    private val json: Json = PricesNetworkJson.instance
) {
    fun create(baseUrl: NormalizedApiBaseUrl): PricesApi {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(NetworkTimeouts.ConnectSeconds, TimeUnit.SECONDS)
            .readTimeout(NetworkTimeouts.ReadSeconds, TimeUnit.SECONDS)
            .writeTimeout(NetworkTimeouts.WriteSeconds, TimeUnit.SECONDS)
            .callTimeout(NetworkTimeouts.CallSeconds, TimeUnit.SECONDS)
            .addInterceptor(AuthHeaderInterceptor(accessTokenProvider))
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl.value.toHttpUrl())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PricesApi::class.java)
    }
}
