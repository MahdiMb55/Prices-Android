package com.mahdiMb55.prices.data.remote

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class ApiErrorParser(
    private val json: Json = PricesNetworkJson.instance
) {
    fun parse(httpStatus: Int, body: String?): NetworkError {
        val payload = body?.takeIf(String::isNotBlank)?.let(::decode)
        val category = categoryFor(httpStatus)
        return NetworkError(
            category = category,
            httpStatus = httpStatus,
            apiCode = payload?.error?.code,
            serverMessage = payload?.error?.message,
            validationDetails = payload?.error?.details as? JsonObject,
            requestId = payload?.error?.requestId,
            isRetryable = category == NetworkErrorCategory.Server
        )
    }

    private fun decode(body: String): ApiErrorEnvelopeDto? = try {
        json.decodeFromString<ApiErrorEnvelopeDto>(body)
    } catch (_: SerializationException) {
        null
    }

    private fun categoryFor(httpStatus: Int): NetworkErrorCategory = when (httpStatus) {
        401 -> NetworkErrorCategory.Authentication
        403 -> NetworkErrorCategory.Permission
        409 -> NetworkErrorCategory.Conflict
        422 -> NetworkErrorCategory.Validation
        in 500..599 -> NetworkErrorCategory.Server
        else -> NetworkErrorCategory.Http
    }
}
