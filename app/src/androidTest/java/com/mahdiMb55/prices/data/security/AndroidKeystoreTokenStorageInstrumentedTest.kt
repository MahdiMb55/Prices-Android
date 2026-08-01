package com.mahdiMb55.prices.data.security

import android.content.Context
import android.security.keystore.KeyInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.security.KeyStore
import java.util.Base64
import java.util.UUID
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreTokenStorageInstrumentedTest {
    private lateinit var context: Context
    private lateinit var preferencesName: String
    private lateinit var keyAlias: String
    private lateinit var preferences: android.content.SharedPreferences
    private lateinit var storage: AndroidKeystoreTokenStorage
    private val codec = JsonEncryptedTokenRecordCodec()

    @Before fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val suffix = UUID.randomUUID().toString()
        preferencesName = "prices_secure_session_test_$suffix"
        keyAlias = "com.mahdiMb55.prices.secure_session.test.$suffix"
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        storage = newStorage()
    }

    @After fun tearDown() {
        preferences.edit().clear().commit()
        context.deleteSharedPreferences(preferencesName)
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
            if (containsAlias(keyAlias)) deleteEntry(keyAlias)
        }
    }

    @Test fun emptyStorageReturnsMissingAndClearIsSafe() = runTest {
        assertTrue(storage.read() === SecureTokenReadResult.Missing)
        assertTrue(storage.clear() === SecureTokenWriteResult.Success)
    }

    @Test fun writeReadAndClearRoundTrip() = runTest {
        val token = "synthetic-instrumentation-token"
        assertTrue(storage.write(token) === SecureTokenWriteResult.Success)
        val result = storage.read()
        assertEquals(token, (result as SecureTokenReadResult.Available).token)
        assertTrue(storage.clear() === SecureTokenWriteResult.Success)
        assertTrue(storage.read() === SecureTokenReadResult.Missing)
    }

    @Test fun storedPreferencesNeverContainPlaintextToken() = runTest {
        val token = "synthetic-preference-token"
        storage.write(token)
        val raw = preferences.all.values.single().toString()
        assertFalse(raw.contains(token))
        assertFalse(preferences.all.values.any { it == token })
        assertFalse(EncryptedTokenRecord("iv", "ciphertext").toString().contains("ciphertext"))
    }

    @Test fun repeatedWritesUseFreshRandomizedIvAndDifferentRecords() = runTest {
        val token = "synthetic-repeat-token"
        storage.write(token)
        val first = preferences.getString(RECORD_KEY, null)!!
        storage.write(token)
        val second = preferences.getString(RECORD_KEY, null)!!
        assertFalse(first == second)
        assertFalse(codec.decode(first)!!.iv == codec.decode(second)!!.iv)
    }

    @Test fun tamperedCiphertextAndIvReturnCorrupted() = runTest {
        storage.write("synthetic-tamper-token")
        val original = codec.decode(preferences.getString(RECORD_KEY, null)!!)!!

        val ciphertext = Base64.getDecoder().decode(original.ciphertext).also { it[0] = (it[0].toInt() xor 1).toByte() }
        preferences.edit().putString(
            RECORD_KEY,
            codec.encode(original.copy(ciphertext = Base64.getEncoder().encodeToString(ciphertext))),
        ).commit()
        assertTrue(storage.read() === SecureTokenReadResult.Corrupted)

        storage.write("synthetic-tamper-token")
        val fresh = codec.decode(preferences.getString(RECORD_KEY, null)!!)!!
        val iv = Base64.getDecoder().decode(fresh.iv).also { it[0] = (it[0].toInt() xor 1).toByte() }
        preferences.edit().putString(
            RECORD_KEY,
            codec.encode(fresh.copy(iv = Base64.getEncoder().encodeToString(iv))),
        ).commit()
        assertTrue(storage.read() === SecureTokenReadResult.Corrupted)
    }

    @Test fun invalidBase64AndUnsupportedSchemaReturnCorrupted() = runTest {
        preferences.edit().putString(RECORD_KEY, "{\"schema_version\":1,\"iv\":\"!\",\"ciphertext\":\"!\"}").commit()
        assertTrue(storage.read() === SecureTokenReadResult.Corrupted)
        preferences.edit().putString(RECORD_KEY, "{\"schema_version\":99,\"iv\":\"AAECAwQFBgcICQoL\",\"ciphertext\":\"YQ==\"}").commit()
        assertTrue(storage.read() === SecureTokenReadResult.Corrupted)
    }

    @Test fun newStorageInstanceReadsExistingToken() = runTest {
        storage.write("synthetic-persistent-token")
        val result = newStorage().read()
        assertEquals("synthetic-persistent-token", (result as SecureTokenReadResult.Available).token)
    }

    @Test fun deletedKeyDoesNotGetGeneratedDuringReadButExplicitWriteCanCreateOne() = runTest {
        storage.write("synthetic-deleted-key-token")
        assertTrue(keyStore().containsAlias(keyAlias))
        keyStore().deleteEntry(keyAlias)

        assertTrue(storage.read() === SecureTokenReadResult.KeyUnavailable)
        assertFalse(keyStore().containsAlias(keyAlias))

        assertTrue(storage.write("synthetic-new-key-token") === SecureTokenWriteResult.Success)
        assertTrue(keyStore().containsAlias(keyAlias))
    }

    @Test fun keyIsAes256AndroidKeystoreNonExportableAndCipherRecordIsValid() = runTest {
        storage.write("synthetic-key-properties-token")
        val key = keyStore().getKey(keyAlias, null) as SecretKey
        assertEquals("AES", key.algorithm)
        assertNull(key.encoded)
        val keyInfo = SecretKeyFactory.getInstance("AES", "AndroidKeyStore")
            .getKeySpec(key, KeyInfo::class.java) as KeyInfo
        assertEquals(256, keyInfo.keySize)

        val record = codec.decode(preferences.getString(RECORD_KEY, null)!!)
        assertNotNull(record)
        assertTrue(Base64.getDecoder().decode(record!!.iv).isNotEmpty())
        assertTrue(Base64.getDecoder().decode(record.ciphertext).size > 16)
        assertEquals("AES/GCM/NoPadding", javax.crypto.Cipher.getInstance("AES/GCM/NoPadding").algorithm)
        assertFalse(SecureTokenReadResult.Available("synthetic-key-properties-token").toString().contains("synthetic"))
    }

    private fun newStorage(): AndroidKeystoreTokenStorage = AndroidKeystoreTokenStorage(
        context = context,
        keyProvider = AndroidKeystoreTokenKeyProvider(keyAlias),
        crypto = AndroidTokenCrypto(),
        recordStore = SharedPreferencesSecureTokenRecordStore(context, preferencesName),
        recordCodec = codec,
    )

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private companion object {
        const val RECORD_KEY = "encrypted_token_record"
    }
}
