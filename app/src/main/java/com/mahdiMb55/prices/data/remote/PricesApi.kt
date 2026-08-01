package com.mahdiMb55.prices.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body

interface PricesApi {
    @GET("discovery")
    suspend fun discovery(): Response<DiscoveryResponseDto>

    @POST("pairing/exchange")
    suspend fun exchangePairingCode(@Body request: PairingExchangeRequestDto): Response<PairingExchangeEnvelopeDto>

    @GET("auth/me")
    suspend fun currentSession(): Response<CurrentSessionEnvelopeDto>
}
