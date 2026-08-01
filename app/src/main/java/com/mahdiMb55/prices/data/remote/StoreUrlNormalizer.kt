package com.mahdiMb55.prices.data.remote

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class NormalizedApiBaseUrl(val value: String)

sealed interface StoreUrlNormalizationResult {
    data class Valid(val baseUrl: NormalizedApiBaseUrl) : StoreUrlNormalizationResult
    data class Invalid(val reason: StoreUrlValidationError) : StoreUrlNormalizationResult
}

enum class StoreUrlValidationError {
    Blank,
    Malformed,
    UnsupportedScheme,
    MissingHost,
    QueryNotAllowed,
    FragmentNotAllowed,
    CredentialsNotAllowed,
    PathAfterApiNamespace
}

object StoreUrlNormalizer {
    private const val ApiNamespacePath = "/wp-json/prices/v1"

    fun normalize(input: String): StoreUrlNormalizationResult {
        if (input.isBlank()) return StoreUrlNormalizationResult.Invalid(StoreUrlValidationError.Blank)
        if (input != input.trim()) return StoreUrlNormalizationResult.Invalid(StoreUrlValidationError.Malformed)

        val uri = try {
            URI(input)
        } catch (_: URISyntaxException) {
            return StoreUrlNormalizationResult.Invalid(StoreUrlValidationError.Malformed)
        }
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "https" && scheme != "http") {
            return StoreUrlNormalizationResult.Invalid(StoreUrlValidationError.UnsupportedScheme)
        }
        if (uri.rawQuery != null) return StoreUrlNormalizationResult.Invalid(StoreUrlValidationError.QueryNotAllowed)
        if (uri.rawFragment != null) return StoreUrlNormalizationResult.Invalid(StoreUrlValidationError.FragmentNotAllowed)
        if (uri.userInfo != null) return StoreUrlNormalizationResult.Invalid(StoreUrlValidationError.CredentialsNotAllowed)
        val host = uri.host?.lowercase(Locale.ROOT)
            ?: return StoreUrlNormalizationResult.Invalid(StoreUrlValidationError.MissingHost)

        val wordpressPath = extractWordPressPath(uri.rawPath.orEmpty())
            ?: return StoreUrlNormalizationResult.Invalid(StoreUrlValidationError.PathAfterApiNamespace)
        val hostPart = if (host.contains(':') && !host.startsWith('[')) "[$host]" else host
        val portPart = uri.port.takeIf { it != -1 }?.let { ":$it" }.orEmpty()
        val baseUrl = "$scheme://$hostPart$portPart$wordpressPath$ApiNamespacePath/"
        if (baseUrl.toHttpUrlOrNull() == null) {
            return StoreUrlNormalizationResult.Invalid(StoreUrlValidationError.Malformed)
        }
        return StoreUrlNormalizationResult.Valid(NormalizedApiBaseUrl(baseUrl))
    }

    private fun extractWordPressPath(rawPath: String): String? {
        val path = rawPath.trimEnd('/')
        val namespaceIndex = path.lowercase(Locale.ROOT).indexOf(ApiNamespacePath)
        if (namespaceIndex == -1) return path
        if (namespaceIndex + ApiNamespacePath.length != path.length) return null
        return path.substring(0, namespaceIndex).trimEnd('/')
    }
}
