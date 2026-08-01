package com.mahdiMb55.prices.data.security

import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidKeystoreTokenStorageTest {
    @Test fun missingRecordReturnsMissingAndSkipsSecurityBoundaries() = runTest {
        val keys = FakeKeyProvider()
        val crypto = FakeCrypto()
        val codec = FakeCodec()
        val result = storage(
            keys = keys,
            crypto = crypto,
            codec = codec,
            store = FakeStore(read = TokenRecordReadResult.Missing),
        ).read()

        assertSame(SecureTokenReadResult.Missing, result)
        assertEquals(0, keys.existingCalls)
        assertEquals(0, keys.createCalls)
        assertEquals(0, codec.decodeCalls)
        assertEquals(0, crypto.decryptCalls)
    }

    @Test fun recordReadFailureMapsToStorageFailure() = runTest {
        val result = storage(store = FakeStore(read = TokenRecordReadResult.Failure)).read()
        assertSame(SecureTokenReadResult.StorageFailure, result)
    }

    @Test fun validRecordIsDecodedAndDecrypted() = runTest {
        val crypto = FakeCrypto(decryptResult = TokenDecryptionResult.Success("original-token"))
        val result = storage(crypto = crypto).read()

        assertEquals("original-token", (result as SecureTokenReadResult.Available).token)
        assertEquals(1, crypto.decryptCalls)
    }

    @Test fun malformedRecordMapsToCorrupted() = runTest {
        val store = FakeStore(read = TokenRecordReadResult.Present("bad"))
        val result = storage(store = store, codec = FakeCodec(record = null)).read()
        assertSame(SecureTokenReadResult.Corrupted, result)
        assertEquals(1, store.clearCalls)
    }

    @Test fun authenticationFailureMapsToCorrupted() = runTest {
        val crypto = FakeCrypto(decryptResult = TokenDecryptionResult.AuthenticationFailure)
        val result = storage(crypto = crypto).read()
        assertSame(SecureTokenReadResult.Corrupted, result)
    }

    @Test fun missingAndUnavailableDecryptionKeysMapToKeyUnavailable() = runTest {
        assertSame(
            SecureTokenReadResult.KeyUnavailable,
            storage(keys = FakeKeyProvider(existing = TokenKeyResult.Missing)).read(),
        )
        assertSame(
            SecureTokenReadResult.KeyUnavailable,
            storage(keys = FakeKeyProvider(existing = TokenKeyResult.Unavailable)).read(),
        )
    }

    @Test fun invalidatedKeyMapsToKeyInvalidated() = runTest {
        val result = storage(keys = FakeKeyProvider(existing = TokenKeyResult.Invalidated)).read()
        assertSame(SecureTokenReadResult.KeyInvalidated, result)
    }

    @Test fun genericCryptoFailureMapsToCryptoFailure() = runTest {
        val crypto = FakeCrypto(decryptResult = TokenDecryptionResult.GenericCryptoFailure)
        assertSame(SecureTokenReadResult.CryptoFailure, storage(crypto = crypto).read())
    }

    @Test fun cancellationsFromStoreCodecAndCryptoPropagate() = runTest {
        assertCancellation { storage(store = FakeStore(readException = CancellationException())).read() }
        assertCancellation { storage(codec = FakeCodec(decodeException = CancellationException())).read() }
        assertCancellation { storage(crypto = FakeCrypto(decryptException = CancellationException())).read() }
    }

    @Test fun readNeverUsesEncryptionKeyCreation() = runTest {
        val keys = FakeKeyProvider()
        storage(keys = keys).read()
        assertEquals(0, keys.createCalls)
        assertEquals(1, keys.existingCalls)
    }

    @Test fun successfulWriteEncryptsEncodesAndPersistsOnlyEncodedRecord() = runTest {
        val store = FakeStore()
        val crypto = FakeCrypto(encryptedRecord = EncryptedTokenRecord("iv", "ciphertext"))
        val codec = FakeCodec(encoded = "encoded-encrypted-record")
        val result = storage(store = store, crypto = crypto, codec = codec).write("secret-token")

        assertSame(SecureTokenWriteResult.Success, result)
        assertEquals(1, crypto.encryptCalls)
        assertEquals("encoded-encrypted-record", store.writtenValue)
        assertFalse(store.writtenValue!!.contains("secret-token"))
        assertEquals("secret-token", crypto.encryptedToken)
    }

    @Test fun writeUsesEncryptionPathAndMapsKeyFailures() = runTest {
        val keys = FakeKeyProvider(create = TokenKeyResult.Unavailable)
        assertSame(SecureTokenWriteResult.KeyUnavailable, storage(keys = keys).write("token"))
        assertEquals(1, keys.createCalls)
        assertEquals(0, keys.existingCalls)

        assertSame(
            SecureTokenWriteResult.KeyInvalidated,
            storage(keys = FakeKeyProvider(create = TokenKeyResult.Invalidated)).write("token"),
        )
    }

    @Test fun writeMapsCryptoCodecAndRecordFailures() = runTest {
        assertSame(
            SecureTokenWriteResult.CryptoFailure,
            storage(crypto = FakeCrypto(encryptResult = TokenEncryptionResult.GenericCryptoFailure)).write("token"),
        )
        assertSame(
            SecureTokenWriteResult.CryptoFailure,
            storage(codec = FakeCodec(encodeException = IllegalStateException())).write("token"),
        )
        assertSame(
            SecureTokenWriteResult.StorageFailure,
            storage(store = FakeStore(writeResult = TokenRecordWriteResult.Failure)).write("token"),
        )
    }

    @Test fun writeCancellationPropagates() = runTest {
        assertCancellation { storage(keys = FakeKeyProvider(createException = CancellationException())).write("token") }
        assertCancellation { storage(crypto = FakeCrypto(encryptException = CancellationException())).write("token") }
        assertCancellation { storage(codec = FakeCodec(encodeException = CancellationException())).write("token") }
        assertCancellation { storage(store = FakeStore(writeException = CancellationException())).write("token") }
    }

    @Test fun successfulAndFailedClearMapCorrectlyWithoutSecurityCalls() = runTest {
        val keys = FakeKeyProvider()
        val crypto = FakeCrypto()
        val codec = FakeCodec()
        val store = FakeStore()
        assertSame(SecureTokenWriteResult.Success, storage(keys, crypto, store, codec).clear())
        assertSame(
            SecureTokenWriteResult.StorageFailure,
            storage(keys, crypto, FakeStore(clearResult = TokenRecordClearResult.Failure), codec).clear(),
        )
        assertEquals(0, keys.createCalls)
        assertEquals(0, keys.existingCalls)
        assertEquals(0, crypto.encryptCalls + crypto.decryptCalls)
        assertEquals(0, codec.encodeCalls + codec.decodeCalls)
    }

    @Test fun clearCancellationPropagates() = runTest {
        assertCancellation { storage(store = FakeStore(clearException = CancellationException())).clear() }
    }

    @Test fun operationsUseInjectedDispatcher() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatches = AtomicInteger()
        val tracking = object : kotlinx.coroutines.CoroutineDispatcher() {
            override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) {
                dispatches.incrementAndGet()
                dispatcher.dispatch(context, block)
            }
        }
        val storage = storage(ioDispatcher = tracking)

        storage.read()
        storage.write("token")
        storage.clear()

        assertEquals(3, dispatches.get())
    }

    private fun storage(
        keys: FakeKeyProvider = FakeKeyProvider(),
        crypto: FakeCrypto = FakeCrypto(),
        store: FakeStore = FakeStore(),
        codec: FakeCodec = FakeCodec(),
        ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
    ): AndroidKeystoreTokenStorage = AndroidKeystoreTokenStorage(
        context = null,
        ioDispatcher = ioDispatcher,
        keyProvider = keys,
        crypto = crypto,
        recordStore = store,
        recordCodec = codec,
    )

    private suspend fun assertCancellation(block: suspend () -> Unit) {
        try {
            block()
            throw AssertionError("Expected CancellationException")
        } catch (expected: CancellationException) {
            // Expected: cancellation is part of the storage contract.
        }
    }
}

private val TEST_KEY: SecretKey = SecretKeySpec("01234567890123456789012345678901".toByteArray(), "AES")

private class FakeKeyProvider(
    private val create: TokenKeyResult = TokenKeyResult.Available(TEST_KEY),
    private val existing: TokenKeyResult = TokenKeyResult.Available(TEST_KEY),
    private val createException: RuntimeException? = null,
) : TokenKeyProvider {
    var createCalls = 0
    var existingCalls = 0
    override fun getOrCreateEncryptionKey(): TokenKeyResult {
        createCalls++
        createException?.let { throw it }
        return create
    }
    override fun getExistingDecryptionKey(): TokenKeyResult {
        existingCalls++
        return existing
    }
}

private class FakeCrypto(
    private val encryptedRecord: EncryptedTokenRecord = EncryptedTokenRecord("iv", "ciphertext"),
    private val encryptResult: TokenEncryptionResult = TokenEncryptionResult.Success(encryptedRecord),
    private val decryptResult: TokenDecryptionResult = TokenDecryptionResult.Success("token"),
    private val encryptException: RuntimeException? = null,
    private val decryptException: RuntimeException? = null,
) : TokenCrypto {
    var encryptCalls = 0
    var decryptCalls = 0
    var encryptedToken: String? = null
    override fun encrypt(token: String, key: SecretKey): TokenEncryptionResult {
        encryptCalls++
        encryptedToken = token
        encryptException?.let { throw it }
        return encryptResult
    }
    override fun decrypt(record: EncryptedTokenRecord, key: SecretKey): TokenDecryptionResult {
        decryptCalls++
        decryptException?.let { throw it }
        return decryptResult
    }
}

private class FakeStore(
    private val read: TokenRecordReadResult = TokenRecordReadResult.Present("encoded"),
    private val writeResult: TokenRecordWriteResult = TokenRecordWriteResult.Success,
    private val clearResult: TokenRecordClearResult = TokenRecordClearResult.Success,
    private val readException: RuntimeException? = null,
    private val writeException: RuntimeException? = null,
    private val clearException: RuntimeException? = null,
) : SecureTokenRecordStore {
    var writtenValue: String? = null
    var clearCalls = 0
    override fun read(): TokenRecordReadResult {
        readException?.let { throw it }
        return read
    }
    override fun write(encodedRecord: String): TokenRecordWriteResult {
        writtenValue = encodedRecord
        writeException?.let { throw it }
        return writeResult
    }
    override fun clear(): TokenRecordClearResult {
        clearCalls++
        clearException?.let { throw it }
        return clearResult
    }
}

private class FakeCodec(
    private val record: EncryptedTokenRecord? = EncryptedTokenRecord("iv", "ciphertext"),
    private val encoded: String = "encoded",
    private val decodeException: RuntimeException? = null,
    private val encodeException: RuntimeException? = null,
) : EncryptedTokenRecordCodec {
    var decodeCalls = 0
    var encodeCalls = 0
    override fun encode(record: EncryptedTokenRecord): String {
        encodeCalls++
        encodeException?.let { throw it }
        return encoded
    }
    override fun decode(value: String): EncryptedTokenRecord? {
        decodeCalls++
        decodeException?.let { throw it }
        return record
    }
}
