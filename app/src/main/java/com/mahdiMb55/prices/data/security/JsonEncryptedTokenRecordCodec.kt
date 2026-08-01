package com.mahdiMb55.prices.data.security

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal class JsonEncryptedTokenRecordCodec : EncryptedTokenRecordCodec {
    override fun encode(record: EncryptedTokenRecord): String = buildJsonObject {
        put(SCHEMA_VERSION_FIELD, SCHEMA_VERSION)
        put(IV_FIELD, record.iv)
        put(CIPHERTEXT_FIELD, record.ciphertext)
    }.toString()

    override fun decode(value: String): EncryptedTokenRecord? {
        return try {
            val json = JSON.parseToJsonElement(value).jsonObject
            if (json.optInt(SCHEMA_VERSION_FIELD) != SCHEMA_VERSION) {
                null
            } else {
                val iv = json[IV_FIELD]?.jsonPrimitive?.content ?: return null
                val ciphertext = json[CIPHERTEXT_FIELD]?.jsonPrimitive?.content ?: return null
                val decodedIv = java.util.Base64.getDecoder().decode(iv)
                val decodedCiphertext = java.util.Base64.getDecoder().decode(ciphertext)
                if (decodedIv.count() == 12 && decodedCiphertext.isNotEmpty()) {
                    EncryptedTokenRecord(iv, ciphertext)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
        const val SCHEMA_VERSION = 1
        const val SCHEMA_VERSION_FIELD = "schema_version"
        const val IV_FIELD = "iv"
        const val CIPHERTEXT_FIELD = "ciphertext"
    }
}

private fun JsonObject.optInt(name: String): Int? = runCatching {
    this[name]?.jsonPrimitive?.int
}.getOrNull()
