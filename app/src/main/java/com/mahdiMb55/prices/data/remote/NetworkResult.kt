package com.mahdiMb55.prices.data.remote

import kotlinx.serialization.json.JsonObject

sealed interface NetworkResult<out T> {
    data class Success<T>(val value: T) : NetworkResult<T>
    data class Failure(val error: NetworkError) : NetworkResult<Nothing>
}

data class NetworkError(
    val category: NetworkErrorCategory,
    val httpStatus: Int? = null,
    val apiCode: String? = null,
    val serverMessage: String? = null,
    val validationDetails: JsonObject? = null,
    val requestId: String? = null,
    val isRetryable: Boolean
)

enum class NetworkErrorCategory {
    Connectivity,
    Timeout,
    Tls,
    Http,
    Authentication,
    Permission,
    Validation,
    Conflict,
    Server,
    Serialization,
    Unknown
}
