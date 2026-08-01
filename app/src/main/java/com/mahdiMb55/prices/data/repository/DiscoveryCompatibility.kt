package com.mahdiMb55.prices.data.repository

object StoreUrlInput {
    fun prepare(input: String): String =
        if (input.isNotBlank() && !input.contains("://")) "https://${input}" else input
}

enum class ApiVersionCompatibility { Supported, Unsupported, Malformed }

object ApiVersionPolicy {
    fun check(apiVersion: String): ApiVersionCompatibility = when {
        !Regex("prices/v\\d+").matches(apiVersion) -> ApiVersionCompatibility.Malformed
        apiVersion == "prices/v1" -> ApiVersionCompatibility.Supported
        else -> ApiVersionCompatibility.Unsupported
    }
}

sealed interface MinimumAppVersionCheck {
    data object Allowed : MinimumAppVersionCheck
    data object UpdateRequired : MinimumAppVersionCheck
    data class Malformed(val value: String) : MinimumAppVersionCheck
}

object MinimumAppVersionPolicy {
    fun check(minimumVersion: String?, currentVersion: String): MinimumAppVersionCheck {
        if (minimumVersion == null) return MinimumAppVersionCheck.Allowed
        val minimum = parse(minimumVersion) ?: return MinimumAppVersionCheck.Malformed(minimumVersion)
        val current = parse(currentVersion) ?: return MinimumAppVersionCheck.Malformed(minimumVersion)
        return if (current < minimum) MinimumAppVersionCheck.UpdateRequired else MinimumAppVersionCheck.Allowed
    }

    private fun parse(value: String): List<Int>? {
        val parts = value.split('.')
        if (parts.isEmpty() || parts.size > 3 || parts.any { it.isEmpty() || it.any(Char::isLetter) }) return null
        return parts.map { it.toIntOrNull() ?: return null }.let { it + List(3 - it.size) { 0 } }
    }

    private operator fun List<Int>.compareTo(other: List<Int>): Int {
        for (index in indices) {
            val comparison = this[index].compareTo(other[index])
            if (comparison != 0) return comparison
        }
        return 0
    }
}
