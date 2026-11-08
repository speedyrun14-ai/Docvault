package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecureVaultManager {

    private const val PREFS_NAME = "docuvault_secure_vault"
    private const val KEY_PIN_HASH = "vault_pin_hash"
    private const val KEY_PIN_SALT = "vault_pin_salt"
    private const val KEY_IS_SETUP = "vault_is_setup"
    private const val KEY_AUTO_LOCK_MINUTES = "vault_auto_lock_minutes"

    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isVaultSetup(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_SETUP, false)
    }

    fun setupPin(context: Context, pin: String): Boolean {
        if (pin.length < 4) return false
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val hash = hashPin(pin, salt)

        getPrefs(context).edit()
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putBoolean(KEY_IS_SETUP, true)
            .putInt(KEY_AUTO_LOCK_MINUTES, 5)
            .apply()
        return true
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = getPrefs(context)
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_PIN_SALT, null) ?: return false

        val salt = Base64.decode(storedSalt, Base64.NO_WRAP)
        val computedHash = hashPin(pin, salt)
        val computedHashStr = Base64.encodeToString(computedHash, Base64.NO_WRAP)
        return storedHash == computedHashStr
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun getSecretKey(pin: String, salt: ByteArray): SecretKeySpec {
        val keyBytes = hashPin(pin, salt)
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptFile(context: Context, sourceFile: File, pin: String, destFile: File) {
        val prefs = getPrefs(context)
        val salt = Base64.decode(prefs.getString(KEY_PIN_SALT, "") ?: "", Base64.NO_WRAP)
        val key = getSecretKey(pin, salt)

        val iv = ByteArray(IV_LENGTH).apply { SecureRandom().nextBytes(this) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val plainBytes = FileInputStream(sourceFile).use { it.readBytes() }
        val cipherBytes = cipher.doFinal(plainBytes)

        FileOutputStream(destFile).use { out ->
            out.write(iv) // Prefix 12 bytes IV
            out.write(cipherBytes)
        }
    }

    fun decryptFile(context: Context, encryptedFile: File, pin: String, destFile: File): Boolean {
        return try {
            val prefs = getPrefs(context)
            val salt = Base64.decode(prefs.getString(KEY_PIN_SALT, "") ?: "", Base64.NO_WRAP)
            val key = getSecretKey(pin, salt)

            val fileBytes = FileInputStream(encryptedFile).use { it.readBytes() }
            if (fileBytes.size <= IV_LENGTH) return false

            val iv = fileBytes.copyOfRange(0, IV_LENGTH)
            val cipherBytes = fileBytes.copyOfRange(IV_LENGTH, fileBytes.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val plainBytes = cipher.doFinal(cipherBytes)

            FileOutputStream(destFile).use { it.write(plainBytes) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
