package com.unirem.spikea

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.unirem.spikea.logic.TvId
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Token and pin per identity alias. The alias is the TvId hash, never an IP or a
 * MAC. Values are AES/GCM ciphertext whose key lives in AndroidKeyStore; the
 * SharedPreferences file is backup-excluded by the manifest rules.
 */
class Vault(context: Context) {

    data class Entry(val token: String?, val pin: String)

    data class Audit(
        val keystoreBacked: Boolean,
        val entries: Int,
        val ciphertextOnly: Boolean,
    )

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun put(tvId: TvId, token: String?, pin: String) {
        val alias = tvId.hex
        val editor = prefs.edit().putString("pin_$alias", encrypt(pin))
        if (token != null) {
            editor.putString("token_$alias", encrypt(token))
        } else {
            editor.remove("token_$alias")
        }
        check(editor.commit()) { "could not persist pairing material" }
    }

    fun get(tvId: TvId): Entry? {
        val alias = tvId.hex
        val pinCipher = prefs.getString("pin_$alias", null) ?: return null
        val pin = decrypt(pinCipher) ?: return null
        val tokenCipher = prefs.getString("token_$alias", null)
        val token = if (tokenCipher == null) {
            null
        } else {
            decrypt(tokenCipher) ?: return null
        }
        return Entry(token, pin)
    }

    fun clear(tvId: TvId): Boolean =
        prefs.edit()
            .remove("pin_${tvId.hex}")
            .remove("token_${tvId.hex}")
            .commit()

    fun isKeystoreBacked(): Boolean = try {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.containsAlias(KEY_ALIAS)
    } catch (e: Exception) {
        false
    }

    fun audit(): Audit {
        val values = prefs.all.values.filterIsInstance<String>()
        val ciphertextOnly = values.all { decrypt(it) != null }
        return Audit(
            keystoreBacked = isKeystoreBacked(),
            entries = prefs.all.keys.count { it.startsWith("pin_") },
            ciphertextOnly = ciphertextOnly,
        )
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return Base64.getEncoder().encodeToString(combined)
    }

    private fun decrypt(value: String): String? {
        return try {
            val combined = Base64.getDecoder().decode(value)
            if (combined.size <= IV_BYTES) {
                null
            } else {
                val iv = combined.copyOfRange(0, IV_BYTES)
                val ciphertext = combined.copyOfRange(IV_BYTES, combined.size)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_BITS, iv))
                String(cipher.doFinal(ciphertext), Charsets.UTF_8)
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val KEY_ALIAS = "unirem-spike-a-vault"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val PREFS_NAME = "spike-a-vault"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val IV_BYTES = 12
    }
}
