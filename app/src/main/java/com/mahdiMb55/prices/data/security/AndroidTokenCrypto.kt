package com.mahdiMb55.prices.data.security

import android.security.keystore.KeyPermanentlyInvalidatedException
import kotlinx.coroutines.CancellationException
import java.nio.charset.StandardCharsets
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class AndroidTokenCrypto : TokenCrypto {
    override fun encrypt(token: String, key: SecretKey): TokenEncryptionResult = try {
        Cipher.getInstance(CIPHER_TRANSFORMATION).run {
            init(Cipher.ENCRYPT_MODE, key)
            TokenEncryptionResult.Success(
                EncryptedTokenRecord(
                    iv = java.util.Base64.getEncoder().encodeToString(iv),
                    ciphertext = java.util.Base64.getEncoder().encodeToString(doFinal(token.toByteArray(StandardCharsets.UTF_8))),
                ),
            )
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: KeyPermanentlyInvalidatedException) {
        TokenEncryptionResult.KeyInvalidated
    } catch (_: Exception) {
        TokenEncryptionResult.GenericCryptoFailure
    }

    override fun decrypt(record: EncryptedTokenRecord, key: SecretKey): TokenDecryptionResult = try {
        val iv = java.util.Base64.getDecoder().decode(record.iv)
        val ciphertext = java.util.Base64.getDecoder().decode(record.ciphertext)
        Cipher.getInstance(CIPHER_TRANSFORMATION).run {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            TokenDecryptionResult.Success(String(doFinal(ciphertext), StandardCharsets.UTF_8))
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: KeyPermanentlyInvalidatedException) {
        TokenDecryptionResult.KeyInvalidated
    } catch (_: AEADBadTagException) {
        TokenDecryptionResult.AuthenticationFailure
    } catch (_: Exception) {
        TokenDecryptionResult.GenericCryptoFailure
    }

    private companion object {
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
