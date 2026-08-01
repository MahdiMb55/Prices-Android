package com.mahdiMb55.prices.data.remote

import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response

class NetworkRequestExecutorTest {
    private val executor = NetworkRequestExecutor(ApiErrorParser())

    @Test
    fun successfulResponseReturnsValue() = runTest {
        val result = executor.execute { Response.success("ok") }

        assertEquals(NetworkResult.Success("ok"), result)
    }

    @Test
    fun ioAndTimeoutFailuresAreClassified() = runTest {
        val ioResult = executor.execute<String> { throw IOException("offline") }
        val timeoutResult = executor.execute<String> { throw SocketTimeoutException("slow") }

        assertEquals(NetworkErrorCategory.Connectivity, ioResult.failureCategory())
        assertEquals(NetworkErrorCategory.Timeout, timeoutResult.failureCategory())
    }

    @Test
    fun httpAndSerializationFailuresAreClassified() = runTest {
        val httpResult = executor.execute<String> {
            Response.error(409, """{"error":{"code":"PRICE_CONFLICT","message":"Price conflict.","details":{},"request_id":"request-3"}}""".toResponseBody("application/json".toMediaType()))
        }
        val serializationResult = executor.execute<String> { throw SerializationException("invalid") }

        assertEquals(NetworkErrorCategory.Conflict, httpResult.failureCategory())
        assertEquals("PRICE_CONFLICT", (httpResult as NetworkResult.Failure).error.apiCode)
        assertEquals(NetworkErrorCategory.Serialization, serializationResult.failureCategory())
    }

    @Test
    fun cancellationIsRethrown() = runTest {
        try {
            executor.execute<String> { throw CancellationException("cancelled") }
            fail("CancellationException should be rethrown")
        } catch (_: CancellationException) {
        }
    }

    private fun NetworkResult<*>.failureCategory(): NetworkErrorCategory =
        (this as NetworkResult.Failure).error.category
}
