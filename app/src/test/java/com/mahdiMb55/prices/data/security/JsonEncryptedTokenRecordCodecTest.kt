package com.mahdiMb55.prices.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonEncryptedTokenRecordCodecTest {
    private val codec = JsonEncryptedTokenRecordCodec()
    private val record = EncryptedTokenRecord(
        iv = "AAECAwQFBgcICQoL",
        ciphertext = "YWJjZGVmZ2hpamtsbW5vcA==",
    )

    @Test fun encodeDecodeRoundTrip() {
        assertEquals(record, codec.decode(codec.encode(record)))
    }

    @Test fun currentSchemaIsAcceptedAndUnsupportedOrMissingSchemaRejected() {
        assertNotNull(codec.decode(codec.encode(record)))
        assertNull(codec.decode(codec.encode(record).replace("\"schema_version\":1", "\"schema_version\":2")))
        assertNull(codec.decode(codec.encode(record).replace("\"schema_version\":1,", "")))
    }

    @Test fun missingOrInvalidFieldsAreRejected() {
        val encoded = codec.encode(record)
        assertNull(codec.decode(encoded.replace("\"iv\":\"AAECAwQFBgcICQoL\",", "")))
        assertNull(codec.decode(encoded.replace("\"ciphertext\":\"YWJjZGVmZ2hpamtsbW5vcA==\"", "")))
        assertNull(codec.decode(encoded.replace("AAECAwQFBgcICQoL", "!")))
        assertNull(codec.decode(encoded.replace("YWJjZGVmZ2hpamtsbW5vcA==", "!")))
        assertNull(codec.decode(encoded.replace("AAECAwQFBgcICQoL", "")))
        assertNull(codec.decode(encoded.replace("YWJjZGVmZ2hpamtsbW5vcA==", "")))
        assertNull(codec.decode("not-json"))
    }

    @Test fun extraFieldsAreIgnoredAndDuplicateValuesUseParserSemantics() {
        val encoded = codec.encode(record)
        assertEquals(record, codec.decode(encoded.dropLast(1) + ",\"future\":true}"))
        val duplicate = encoded.dropLast(1) + ",\"iv\":\"AAECAwQFBgcICQoL\"}"
        assertNotNull(codec.decode(duplicate))
    }

    @Test fun recordToStringIsRedacted() {
        val printable = record.toString()
        assertFalse(printable.contains(record.iv))
        assertFalse(printable.contains(record.ciphertext))
        assertEquals("EncryptedTokenRecord(redacted)", printable)
    }

    @Test fun availableToStringIsRedacted() {
        val printable = SecureTokenReadResult.Available("synthetic-test-token").toString()
        assertTrue(printable.contains("<redacted>"))
        assertFalse(printable.contains("synthetic-test-token"))
    }
}
