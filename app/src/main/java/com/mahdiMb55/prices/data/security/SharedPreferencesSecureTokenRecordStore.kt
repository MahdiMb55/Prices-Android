package com.mahdiMb55.prices.data.security

import android.content.Context

internal class SharedPreferencesSecureTokenRecordStore(
    context: Context,
    preferencesName: String = DEFAULT_PREFERENCES_NAME,
) : SecureTokenRecordStore {
    private val preferences = context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun read(): TokenRecordReadResult = try {
        preferences.getString(RECORD_KEY, null)?.let(TokenRecordReadResult::Present)
            ?: TokenRecordReadResult.Missing
    } catch (_: Exception) {
        TokenRecordReadResult.Failure
    }

    override fun write(encodedRecord: String): TokenRecordWriteResult = try {
        if (preferences.edit().putString(RECORD_KEY, encodedRecord).commit()) {
            TokenRecordWriteResult.Success
        } else {
            TokenRecordWriteResult.Failure
        }
    } catch (_: Exception) {
        TokenRecordWriteResult.Failure
    }

    override fun clear(): TokenRecordClearResult = try {
        if (preferences.edit().remove(RECORD_KEY).commit()) {
            TokenRecordClearResult.Success
        } else {
            TokenRecordClearResult.Failure
        }
    } catch (_: Exception) {
        TokenRecordClearResult.Failure
    }

    internal companion object {
        const val DEFAULT_PREFERENCES_NAME = "prices_secure_session"
        const val RECORD_KEY = "encrypted_token_record"
    }
}
