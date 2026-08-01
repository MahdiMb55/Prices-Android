package com.mahdiMb55.prices.data.remote

import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException
import javax.net.ssl.SSLException
import kotlinx.serialization.SerializationException
import retrofit2.Response

class NetworkRequestExecutor(
    private val errorParser: ApiErrorParser
) {
    suspend fun <T> execute(call: suspend () -> Response<T>): NetworkResult<T> = try {
        val response = call()
        if (response.isSuccessful) {
            response.body()?.let { NetworkResult.Success(it) } ?: NetworkResult.Failure(
                NetworkError(
                    category = NetworkErrorCategory.Serialization,
                    httpStatus = response.code(),
                    isRetryable = false
                )
            )
        } else {
            NetworkResult.Failure(errorParser.parse(response.code(), response.errorBody()?.string()))
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: SocketTimeoutException) {
        NetworkResult.Failure(NetworkError(NetworkErrorCategory.Timeout, isRetryable = true))
    } catch (exception: SSLException) {
        NetworkResult.Failure(NetworkError(NetworkErrorCategory.Tls, isRetryable = false))
    } catch (exception: IOException) {
        NetworkResult.Failure(NetworkError(NetworkErrorCategory.Connectivity, isRetryable = true))
    } catch (exception: SerializationException) {
        NetworkResult.Failure(NetworkError(NetworkErrorCategory.Serialization, isRetryable = false))
    } catch (exception: Exception) {
        NetworkResult.Failure(NetworkError(NetworkErrorCategory.Unknown, isRetryable = false))
    }
}
