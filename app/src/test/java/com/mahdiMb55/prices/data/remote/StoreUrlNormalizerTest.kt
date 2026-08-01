package com.mahdiMb55.prices.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreUrlNormalizerTest {
    @Test
    fun bareHttpsHostAddsPricesApiNamespace() {
        assertEquals(
            "https://example.com/wp-json/prices/v1/",
            normalize("https://example.com").validUrl()
        )
    }

    @Test
    fun trailingSlashIsCanonicalized() {
        assertEquals(
            "https://example.com/wp-json/prices/v1/",
            normalize("https://example.com/").validUrl()
        )
    }

    @Test
    fun wordpressSubdirectoryIsPreserved() {
        assertEquals(
            "https://example.com/shop/wp-json/prices/v1/",
            normalize("https://example.com/shop/").validUrl()
        )
    }

    @Test
    fun existingNamespaceIsNotDuplicated() {
        assertEquals(
            "https://example.com/shop/wp-json/prices/v1/",
            normalize("https://example.com/shop/wp-json/prices/v1/").validUrl()
        )
    }

    @Test
    fun existingNamespaceWithoutTrailingSlashIsNotDuplicated() {
        assertEquals(
            "https://example.com/shop/wp-json/prices/v1/",
            normalize("https://example.com/shop/wp-json/prices/v1").validUrl()
        )
    }

    @Test
    fun nonDefaultPortIsPreserved() {
        assertEquals(
            "https://example.com:8443/wp-json/prices/v1/",
            normalize("https://example.com:8443").validUrl()
        )
    }

    @Test
    fun mixedCaseHostIsCanonicalized() {
        assertEquals(
            "https://example.com/wp-json/prices/v1/",
            normalize("https://EXAMPLE.COM").validUrl()
        )
    }

    @Test
    fun punycodeHostIsSupported() {
        assertEquals(
            "https://xn--bcher-kva.example/wp-json/prices/v1/",
            normalize("https://xn--bcher-kva.example").validUrl()
        )
    }

    @Test
    fun queryStringsAreRejected() {
        assertInvalid("https://example.com/shop?preview=true", StoreUrlValidationError.QueryNotAllowed)
    }

    @Test
    fun fragmentsAreRejected() {
        assertInvalid("https://example.com/#section", StoreUrlValidationError.FragmentNotAllowed)
    }

    @Test
    fun unsupportedSchemeIsRejected() {
        assertInvalid("ftp://example.com", StoreUrlValidationError.UnsupportedScheme)
    }

    @Test
    fun missingHostIsRejected() {
        assertInvalid("https:///shop", StoreUrlValidationError.MissingHost)
    }

    @Test
    fun blankInputIsRejected() {
        assertInvalid(" ", StoreUrlValidationError.Blank)
    }

    @Test
    fun pathAfterApiNamespaceIsRejected() {
        assertInvalid(
            "https://example.com/wp-json/prices/v1/discovery",
            StoreUrlValidationError.PathAfterApiNamespace
        )
    }

    private fun normalize(input: String): StoreUrlNormalizationResult = StoreUrlNormalizer.normalize(input)

    private fun StoreUrlNormalizationResult.validUrl(): String {
        assertTrue(this is StoreUrlNormalizationResult.Valid)
        return (this as StoreUrlNormalizationResult.Valid).baseUrl.value
    }

    private fun assertInvalid(input: String, expected: StoreUrlValidationError) {
        val result = normalize(input)
        assertTrue(result is StoreUrlNormalizationResult.Invalid)
        assertEquals(expected, (result as StoreUrlNormalizationResult.Invalid).reason)
    }
}
