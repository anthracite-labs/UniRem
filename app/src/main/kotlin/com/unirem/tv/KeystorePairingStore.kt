package com.unirem.tv

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.unirem.core.api.RedactedLog
import com.unirem.core.api.TvId
import com.unirem.tv.api.PairingEntry
import com.unirem.tv.api.PairingStore
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "Pairing"

/**
 * [PairingStore] port implementation. Token and pin live as AES/GCM
 * ciphertext whose key lives in AndroidKeyStore, under a per-TV alias that is
 * the [TvId] hash (never an IP or a MAC). The preferences file holding the
 * ciphertext is backup-excluded by the manifest rules. Session-only targets
 * never reach here: they have no [TvId] and are kept in memory only.
 */
class KeystorePairingStore(context: Context) : PairingStore {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun save(tvId: TvId, token: String?, pin: String): Unit = withContext(Dispatchers.IO) {
        try {
            val editor = prefs.edit().putString(pinKey(tvId), encrypt(pin))
            if (token != null) {
                editor.putString(tokenKey(tvId), encrypt(token))
            } else {
                editor.remove(tokenKey(tvId))
            }
            if (!editor.commit()) RedactedLog.warn(TAG, "Pairing material unwritable")
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "Pairing material unwritable")
        }
    }

    override suspend fun load(tvId: TvId): PairingEntry? = withContext(Dispatchers.IO) {
        try {
            val pinCipher = prefs.getString(pinKey(tvId), null) ?: return@withContext null
            val pin = decrypt(pinCipher) ?: return@withContext null
            val tokenCipher = prefs.getString(tokenKey(tvId), null)
            val token = if (tokenCipher == null) null else decrypt(tokenCipher) ?: return@withContext null
            PairingEntry(token, pin)
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "Pairing material unreadable")
            null
        }
    }

    override suspend fun clear(tvId: TvId): Boolean = withContext(Dispatchers.IO) {
        try {
            val had = prefs.contains(pinKey(tvId)) || prefs.contains(tokenKey(tvId))
            val ok = prefs.edit().remove(pinKey(tvId)).remove(tokenKey(tvId)).commit()
            if (!ok) RedactedLog.warn(TAG, "Pairing material unclearable")
            had && ok
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "Pairing material unclearable")
            false
        }
    }

    private fun pinKey(tvId: TvId): String = "pin_${tvId.hex}"

    private fun tokenKey(tvId: TvId): String = "token_${tvId.hex}"

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(VaultKeyAlias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                VaultKeyAlias,
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
            if (combined.size <= IV_BYTES) return null
            val iv = combined.copyOfRange(0, IV_BYTES)
            val ciphertext = combined.copyOfRange(IV_BYTES, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val VaultKeyAlias = "unirem-pairing-key"
        const val PREFS_NAME = "unirem_pairing"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val IV_BYTES = 12
    }
}
