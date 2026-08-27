package com.boikhata.data.encryption

import android.util.Base64
import com.boikhata.security.SecurityManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataEncryptionHelper @Inject constructor(
    private val securityManager: SecurityManager
) {
    fun encryptSensitiveField(value: String): String {
        try {
            val encrypted = securityManager.encryptData(value)
            val combined = encrypted.data + encrypted.iv
            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            return value // Fallback to plain text if encryption fails
        }
    }

    fun decryptSensitiveField(encoded: String): String {
        try {
            val combined = Base64.decode(encoded, Base64.DEFAULT)
            val ivSize = 12 // GCM IV size
            val iv = combined.copyOfRange(combined.size - ivSize, combined.size)
            val data = combined.copyOfRange(0, combined.size - ivSize)
            return securityManager.decryptData(com.boikhata.security.EncryptedData(data, iv))
        } catch (e: Exception) {
            return encoded
        }
    }
}
