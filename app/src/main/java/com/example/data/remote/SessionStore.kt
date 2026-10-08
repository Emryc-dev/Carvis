package com.example.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMs: Long,
    val lastActiveAtMs: Long,
)

class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("carvision_session", Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun load(): StoredSession? = runCatching {
        val access = decrypt(preferences.getString(ACCESS_TOKEN, null) ?: return null)
        val refresh = decrypt(preferences.getString(REFRESH_TOKEN, null) ?: return null)
        StoredSession(
            accessToken = access,
            refreshToken = refresh,
            expiresAtMs = preferences.getLong(EXPIRES_AT, 0L),
            lastActiveAtMs = preferences.getLong(LAST_ACTIVE_AT, 0L),
        )
    }.getOrElse { clear(); null }

    fun save(session: StoredSession) {
        preferences.edit()
            .putString(ACCESS_TOKEN, encrypt(session.accessToken))
            .putString(REFRESH_TOKEN, encrypt(session.refreshToken))
            .putLong(EXPIRES_AT, session.expiresAtMs)
            .putLong(LAST_ACTIVE_AT, session.lastActiveAtMs)
            .apply()
    }

    fun updateLastActive(timestampMs: Long) {
        preferences.edit().putLong(LAST_ACTIVE_AT, timestampMs).apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun secretKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        val iv = payload.copyOfRange(0, IV_LENGTH)
        val encrypted = payload.copyOfRange(IV_LENGTH, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private companion object {
        const val KEY_ALIAS = "carvision_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val ACCESS_TOKEN = "access_token"
        const val REFRESH_TOKEN = "refresh_token"
        const val EXPIRES_AT = "expires_at"
        const val LAST_ACTIVE_AT = "last_active_at"
    }
}
