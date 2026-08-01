package com.mahdiMb55.prices.data.remote

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApiErrorParserTest {
    private val parser = ApiErrorParser()

    @Test
    fun standardBackendErrorPreservesStableCodeAndMessage() {
        val error = parser.parse(401, """{"error":{"code":"AUTHENTICATION_REQUIRED","message":"Authentication is required.","details":{},"request_id":"request-1"}}""")

        assertEquals(NetworkErrorCategory.Authentication, error.category)
        assertEquals("AUTHENTICATION_REQUIRED", error.apiCode)
        assertEquals("Authentication is required.", error.serverMessage)
        assertEquals("request-1", error.requestId)
    }

    @Test
    fun validationDetailsArePreservedWithoutTranslatingCode() {
        val error = parser.parse(422, """{"error":{"code":"INVALID_FILTER","message":"Invalid filter.","details":{"field":"status"},"request_id":"request-2"}}""")

        assertEquals(NetworkErrorCategory.Validation, error.category)
        assertEquals("INVALID_FILTER", error.apiCode)
        assertEquals(JsonPrimitive("status"), error.validationDetails?.get("field"))
    }

    @Test
    fun malformedAndEmptyBodiesRemainHttpErrors() {
        assertNull(parser.parse(500, "not-json").apiCode)
        assertEquals(NetworkErrorCategory.Server, parser.parse(500, "").category)
    }

    @Test
    fun statusCodesMapToStableCategories() {
        assertEquals(NetworkErrorCategory.Permission, parser.parse(403, null).category)
        assertEquals(NetworkErrorCategory.Conflict, parser.parse(409, null).category)
        assertEquals(NetworkErrorCategory.Validation, parser.parse(422, null).category)
        assertEquals(NetworkErrorCategory.Server, parser.parse(500, null).category)
    }

    @Test
    fun unknownBackendCodeIsPreserved() {
        val error = parser.parse(400, """{"error":{"code":"FUTURE_CODE","message":"Future message","details":{},"request_id":"request-4"}}""")

        assertEquals("FUTURE_CODE", error.apiCode)
        assertEquals(NetworkErrorCategory.Http, error.category)
    }
}
