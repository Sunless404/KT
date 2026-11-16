package com.sunless404.keeptrack.ai

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Guarda la clave de la API cifrada con una clave AES del Android Keystore,
 * que nunca sale del chip de seguridad del móvil.
 */
class SecretStore(private val prefs: SharedPreferences) {

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return gen.generateKey()
    }

    var apiKey: String
        get() {
            val stored = prefs.getString(PREF, null) ?: return ""
            return try {
                val bytes = Base64.decode(stored, Base64.NO_WRAP)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, IV_SIZE))
                String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE))
            } catch (_: Exception) {
                ""
            }
        }
        set(value) {
            if (value.isBlank()) {
                prefs.edit().remove(PREF).apply()
                return
            }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val out = cipher.iv + cipher.doFinal(value.trim().toByteArray())
            prefs.edit().putString(PREF, Base64.encodeToString(out, Base64.NO_WRAP)).apply()
        }

    companion object {
        private const val ALIAS = "keeptrack_ai_key"
        private const val PREF = "aiKeyEnc"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
    }
}
