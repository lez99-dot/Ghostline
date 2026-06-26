package com.example

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesEncryptionHelper {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    fun encrypt(plainText: String, secretKey: String): Pair<String, String> {
        if (plainText.isEmpty()) return Pair("", "")
        return try {
            val keyBytes = digestKey(secretKey)
            val secretKeySpec = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            
            Pair(encryptedBase64, ivBase64)
        } catch (e: Exception) {
            Pair("Encryption Error: ${e.message}", "")
        }
    }

    fun decrypt(encryptedBase64: String, secretKey: String, ivBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        return try {
            val keyBytes = digestKey(secretKey)
            val secretKeySpec = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "Decryption Error: ${e.message}"
        }
    }

    private fun digestKey(key: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(key.toByteArray(Charsets.UTF_8))
    }
}
