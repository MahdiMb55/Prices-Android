package com.mahdiMb55.prices.data.security

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface SecureTokenReadResult {
    data object Missing : SecureTokenReadResult

    class Available internal constructor(internal val token: String) : SecureTokenReadResult {
        override fun toString(): String = "Available(token=<redacted>)"
    }

    data object Corrupted : SecureTokenReadResult
    data object KeyUnavailable : SecureTokenReadResult
    data object KeyInvalidated : SecureTokenReadResult
    data object CryptoFailure : SecureTokenReadResult
    data object StorageFailure : SecureTokenReadResult
}

sealed interface SecureTokenWriteResult {
    data object Success : SecureTokenWriteResult
    data object KeyUnavailable : SecureTokenWriteResult
    data object KeyInvalidated : SecureTokenWriteResult
    data object CryptoFailure : SecureTokenWriteResult
    data object StorageFailure : SecureTokenWriteResult
}

internal interface TokenKeyProvider {
    fun getOrCreateEncryptionKey(): TokenKeyResult
    fun getExistingDecryptionKey(): TokenKeyResult
}

internal interface TokenCrypto {
    fun encrypt(token: String, key: javax.crypto.SecretKey): TokenEncryptionResult
    fun decrypt(record: EncryptedTokenRecord, key: javax.crypto.SecretKey): TokenDecryptionResult
}

internal interface SecureTokenRecordStore {
    fun read(): TokenRecordReadResult
    fun write(encodedRecord: String): TokenRecordWriteResult
    fun clear(): TokenRecordClearResult
}

internal interface EncryptedTokenRecordCodec {
    fun encode(record: EncryptedTokenRecord): String
    fun decode(value: String): EncryptedTokenRecord?
}

internal data class EncryptedTokenRecord(
    val iv: String,
    val ciphertext: String,
) {
    override fun toString(): String = "EncryptedTokenRecord(redacted)"
}

internal sealed interface TokenKeyResult {
    data class Available(val key: javax.crypto.SecretKey) : TokenKeyResult
    data object Missing : TokenKeyResult
    data object Unavailable : TokenKeyResult
    data object Invalidated : TokenKeyResult
}

internal sealed interface TokenEncryptionResult {
    data class Success(val record: EncryptedTokenRecord) : TokenEncryptionResult
    data object KeyInvalidated : TokenEncryptionResult
    data object GenericCryptoFailure : TokenEncryptionResult
}

internal sealed interface TokenDecryptionResult {
    data class Success(val token: String) : TokenDecryptionResult
    data object AuthenticationFailure : TokenDecryptionResult
    data object KeyInvalidated : TokenDecryptionResult
    data object GenericCryptoFailure : TokenDecryptionResult
}

internal sealed interface TokenRecordReadResult {
    data object Missing : TokenRecordReadResult
    data class Present(val value: String) : TokenRecordReadResult
    data object Failure : TokenRecordReadResult
}

internal sealed interface TokenRecordWriteResult {
    data object Success : TokenRecordWriteResult
    data object Failure : TokenRecordWriteResult
}

internal sealed interface TokenRecordClearResult {
    data object Success : TokenRecordClearResult
    data object Failure : TokenRecordClearResult
}

internal class AndroidKeystoreTokenStorage(
    context: Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val keyProvider: TokenKeyProvider = AndroidKeystoreTokenKeyProvider(),
    private val crypto: TokenCrypto = AndroidTokenCrypto(),
    private val recordStore: SecureTokenRecordStore = context?.let(::SharedPreferencesSecureTokenRecordStore)
        ?: error("A Context is required when no record store is injected"),
    private val recordCodec: EncryptedTokenRecordCodec = JsonEncryptedTokenRecordCodec(),
) : SecureTokenStorage {
    override suspend fun read(): SecureTokenReadResult = withContext(ioDispatcher) {
        when (val stored = recordStore.read()) {
            TokenRecordReadResult.Missing -> SecureTokenReadResult.Missing
            TokenRecordReadResult.Failure -> SecureTokenReadResult.StorageFailure
            is TokenRecordReadResult.Present -> readPresentRecord(stored.value)
        }
    }

    override suspend fun write(token: String): SecureTokenWriteResult = withContext(ioDispatcher) {
        when (val key = keyProvider.getOrCreateEncryptionKey()) {
            is TokenKeyResult.Available -> when (val encrypted = crypto.encrypt(token, key.key)) {
                is TokenEncryptionResult.Success -> when (val encoded = encodeRecord(encrypted.record)) {
                    is RecordEncoding.Success -> when (recordStore.write(encoded.value)) {
                        TokenRecordWriteResult.Success -> SecureTokenWriteResult.Success
                        TokenRecordWriteResult.Failure -> SecureTokenWriteResult.StorageFailure
                    }
                    RecordEncoding.Failure -> SecureTokenWriteResult.CryptoFailure
                }
                TokenEncryptionResult.KeyInvalidated -> SecureTokenWriteResult.KeyInvalidated
                TokenEncryptionResult.GenericCryptoFailure -> SecureTokenWriteResult.CryptoFailure
            }
            TokenKeyResult.Invalidated -> SecureTokenWriteResult.KeyInvalidated
            TokenKeyResult.Missing, TokenKeyResult.Unavailable -> SecureTokenWriteResult.KeyUnavailable
        }
    }

    override suspend fun clear(): SecureTokenWriteResult = withContext(ioDispatcher) {
        when (recordStore.clear()) {
            TokenRecordClearResult.Success -> SecureTokenWriteResult.Success
            TokenRecordClearResult.Failure -> SecureTokenWriteResult.StorageFailure
        }
    }

    private fun readPresentRecord(raw: String): SecureTokenReadResult {
        val record = try {
            recordCodec.decode(raw)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        } ?: return clearAndMap(SecureTokenReadResult.Corrupted)
        return when (val key = keyProvider.getExistingDecryptionKey()) {
            TokenKeyResult.Missing, TokenKeyResult.Unavailable -> SecureTokenReadResult.KeyUnavailable
            TokenKeyResult.Invalidated -> SecureTokenReadResult.KeyInvalidated
            is TokenKeyResult.Available -> when (val decrypted = crypto.decrypt(record, key.key)) {
                is TokenDecryptionResult.Success -> SecureTokenReadResult.Available(decrypted.token)
                TokenDecryptionResult.AuthenticationFailure -> clearAndMap(SecureTokenReadResult.Corrupted)
                TokenDecryptionResult.KeyInvalidated -> SecureTokenReadResult.KeyInvalidated
                TokenDecryptionResult.GenericCryptoFailure -> SecureTokenReadResult.CryptoFailure
            }
        }
    }

    private fun clearAndMap(result: SecureTokenReadResult): SecureTokenReadResult {
        recordStore.clear()
        return result
    }

    private fun encodeRecord(record: EncryptedTokenRecord): RecordEncoding = try {
        RecordEncoding.Success(recordCodec.encode(record))
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        RecordEncoding.Failure
    }

    private sealed interface RecordEncoding {
        data class Success(val value: String) : RecordEncoding
        data object Failure : RecordEncoding
    }
}

interface SecureTokenStorage {
    suspend fun read(): SecureTokenReadResult
    suspend fun write(token: String): SecureTokenWriteResult
    suspend fun clear(): SecureTokenWriteResult
}
