package com.example.schoolmanager.data

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.mutableStateOf
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * إدارة الحماية: PIN، القفل التلقائي، خيارات النسخ الاحتياطي.
 */
object SecurityManager {
    private const val PREFS = "app_security"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_PIN_SALT = "pin_salt"
    private const val KEY_PIN_ENABLED = "pin_enabled"
    private const val KEY_ENCRYPT_BACKUP = "encrypt_backup"
    private const val KEY_AUTO_LOCK_MINUTES = "auto_lock_minutes"
    private const val DEFAULT_AUTO_LOCK = 5

    val isLockedState = mutableStateOf(true)
    val pinEnabledState = mutableStateOf(false)

    fun initialize(context: Context) {
        val enabled = isPinEnabled(context)
        pinEnabledState.value = enabled
        isLockedState.value = enabled
    }

    fun isPinEnabled(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_PIN_ENABLED, false)
    }

    fun setPin(context: Context, pin: String) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        val salt = generateSalt()
        val hash = hash(pin, salt)
        sp.putString(KEY_PIN_SALT, salt)
        sp.putString(KEY_PIN_HASH, hash)
        sp.putBoolean(KEY_PIN_ENABLED, true)
        sp.apply()
        pinEnabledState.value = true
        isLockedState.value = true
    }

    fun removePin(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        sp.remove(KEY_PIN_SALT)
        sp.remove(KEY_PIN_HASH)
        sp.putBoolean(KEY_PIN_ENABLED, false)
        sp.apply()
        pinEnabledState.value = false
        isLockedState.value = false
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val salt = sp.getString(KEY_PIN_SALT, null) ?: return false
        val storedHash = sp.getString(KEY_PIN_HASH, null) ?: return false
        return hash(pin, salt) == storedHash
    }

    fun lock() {
        if (pinEnabledState.value) isLockedState.value = true
    }

    fun unlock() {
        isLockedState.value = false
    }

    fun getAutoLockMinutes(context: Context): Int {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sp.getInt(KEY_AUTO_LOCK_MINUTES, DEFAULT_AUTO_LOCK)
    }

    fun setAutoLockMinutes(context: Context, minutes: Int) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        sp.putInt(KEY_AUTO_LOCK_MINUTES, minutes)
        sp.apply()
    }

    fun isBackupEncryptionEnabled(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_ENCRYPT_BACKUP, false)
    }

    fun setBackupEncryptionEnabled(context: Context, enabled: Boolean) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        sp.putBoolean(KEY_ENCRYPT_BACKUP, enabled)
        sp.apply()
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun hash(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}
