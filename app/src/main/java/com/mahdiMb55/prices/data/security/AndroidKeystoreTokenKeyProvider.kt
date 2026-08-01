package com.mahdiMb55.prices.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import kotlinx.coroutines.CancellationException
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal class AndroidKeystoreTokenKeyProvider(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
) : TokenKeyProvider {
    override fun getOrCreateEncryptionKey(): TokenKeyResult = try {
        existingKey()?.let(TokenKeyResult::Available) ?: createKey()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: KeyPermanentlyInvalidatedException) {
        TokenKeyResult.Invalidated
    } catch (_: Exception) {
        TokenKeyResult.Unavailable
    }

    override fun getExistingDecryptionKey(): TokenKeyResult = try {
        existingKey()?.let(TokenKeyResult::Available) ?: TokenKeyResult.Missing
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: KeyPermanentlyInvalidatedException) {
        TokenKeyResult.Invalidated
    } catch (_: Exception) {
        TokenKeyResult.Unavailable
    }

    private fun existingKey(): SecretKey? = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        .getKey(keyAlias, null) as? SecretKey

    private fun createKey(): TokenKeyResult.Available = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES,
        KEYSTORE,
    ).apply {
        init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256)
                .build(),
        )
    }.generateKey().let(TokenKeyResult::Available)

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val DEFAULT_KEY_ALIAS = "com.mahdiMb55.prices.secure_session.v1"
    }
}
