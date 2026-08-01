package com.mahdiMb55.prices.data.remote

import retrofit2.Response
import retrofit2.http.GET

interface PricesApi {
    @GET("discovery")
    suspend fun discovery(): Response<DiscoveryResponseDto>
}
