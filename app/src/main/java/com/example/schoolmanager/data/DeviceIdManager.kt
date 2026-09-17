package com.example.schoolmanager.data

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

/**
 * توليد كود فريد للجهاز — لا يتغير أبداً.
 * يعتمد على: Android ID + Package Name + Salt.
 *
 * ⚠️ مهم: DEVICE_SALT يجب أن يكون مطابقاً تماماً لما في keygen.html
 */
object DeviceIdManager {

    // ★ المفتاح السري المدمج في التطبيق (لازم يطابق مفتاح keygen.html)
    private const val DEVICE_SALT = "SM-2026-DEVICE-SALT-K9X2M"

    /**
     * يعيد كود الجهاز بصيغة XXXXXXXX-XXXXXXXX
     * مثال: A1B2C3D4-E5F6G7H8
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        val androidId: String = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "no-id"
        } catch (e: Exception) {
            "no-id"
        }

        val packageName = context.packageName
        val combined = "$androidId|$packageName|$DEVICE_SALT"

        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(combined.toByteArray(Charsets.UTF_8))
        val hex = hash.joinToString("") { "%02x".format(it) }.uppercase()

        // أول 16 حرفاً، مقسمة على 8-8
        return "${hex.substring(0, 8)}-${hex.substring(8, 16)}"
    }
}
