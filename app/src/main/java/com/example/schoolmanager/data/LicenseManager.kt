package com.example.schoolmanager.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * إدارة الترخيص — تحقق محلي من كود التفعيل.
 *
 * ⚠️ مهم: LICENSE_SECRET يجب أن يكون مطابقاً تماماً لما في keygen.html
 */
object LicenseManager {

    private const val PREFS = "app_license"
    private const val KEY_ACTIVATED = "activated"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_LICENSE = "license_key"
    private const val KEY_LAST_CHECK = "last_check"

    // ★ المفتاح السري (يجب أن يطابق مفتاح keygen.html تماماً)
    private const val LICENSE_SECRET = "SM-2026-LICENSE-SECRET-P4Q8R2W9X"

    // ★ نقطة البداية لحساب الأيام
    private const val BASE_MILLIS = 1577836800000L // 2020-01-01 UTC
    private const val DAY_MS = 86400000L

    // ★ مدة السماح بعد الانتهاء (7 أيام)
    private const val GRACE_PERIOD_MS = 7 * DAY_MS

    // ═══════════════════════════════════
    // حالة الترخيص
    // ═══════════════════════════════════

    data class LicenseStatus(
        val isActive: Boolean,
        val isExpired: Boolean,
        val isGracePeriod: Boolean,
        val expiresAt: Long,
        val daysRemaining: Int,
        val message: String
    )

    fun getStatus(context: Context): LicenseStatus {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val expiresAt = sp.getLong(KEY_EXPIRES_AT, 0L)
        val now = System.currentTimeMillis()

        if (expiresAt == 0L) {
            return LicenseStatus(
                isActive = false,
                isExpired = false,
                isGracePeriod = false,
                expiresAt = 0,
                daysRemaining = 0,
                message = "لم يتم تفعيل التطبيق بعد"
            )
        }

        val diff = expiresAt - now
        val daysRemaining = (diff / DAY_MS).toInt()

        return when {
            diff > 0 -> LicenseStatus(
                isActive = true,
                isExpired = false,
                isGracePeriod = false,
                expiresAt = expiresAt,
                daysRemaining = daysRemaining,
                message = "مُفعَّل — متبقٍ $daysRemaining يوم"
            )
            diff > -GRACE_PERIOD_MS -> LicenseStatus(
                isActive = true,
                isExpired = false,
                isGracePeriod = true,
                expiresAt = expiresAt,
                daysRemaining = daysRemaining,
                message = "انتهى الاشتراك — في فترة السماح (${-daysRemaining} يوم)"
            )
            else -> LicenseStatus(
                isActive = false,
                isExpired = true,
                isGracePeriod = false,
                expiresAt = expiresAt,
                daysRemaining = 0,
                message = "انتهى الاشتراك — يرجى التجديد"
            )
        }
    }

    fun isActivated(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val expiresAt = sp.getLong(KEY_EXPIRES_AT, 0L)
        return System.currentTimeMillis() < expiresAt + GRACE_PERIOD_MS
    }

    fun getExpiryDate(context: Context): Long {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sp.getLong(KEY_EXPIRES_AT, 0L)
    }

    fun getFormattedExpiry(context: Context): String {
        val expiry = getExpiryDate(context)
        if (expiry == 0L) return "غير مُفعَّل"
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date(expiry))
    }

    fun getStoredLicense(context: Context): String {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sp.getString(KEY_LICENSE, "") ?: ""
    }

    // ═══════════════════════════════════
    // التفعيل
    // ═══════════════════════════════════

    fun verifyAndActivate(context: Context, licenseKey: String): ActivationResult {
        val deviceId = DeviceIdManager.getDeviceId(context)
        val result = verifyKey(deviceId, licenseKey)

        if (result is ActivationResult.Success) {
            val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            sp.putBoolean(KEY_ACTIVATED, true)
            sp.putLong(KEY_EXPIRES_AT, result.expiresAt)
            sp.putString(KEY_LICENSE, licenseKey.trim().uppercase())
            sp.putLong(KEY_LAST_CHECK, System.currentTimeMillis())
            sp.apply()
        }

        return result
    }

    sealed class ActivationResult {
        data class Success(val expiresAt: Long) : ActivationResult()
        data class Failure(val reason: String) : ActivationResult()
    }

    /**
     * التحقق من كود التفعيل محلياً.
     * صيغة الكود: SM26-XXXX-XXXX-XXXX-XXXX
     *   XXXX (1) = آخر 4 أحرف من كود الجهاز
     *   XXXX (2) = تاريخ الانتهاء (أيام منذ 2020-01-01) بالـ Hex
     *   XXXX (3+4) = توقيع HMAC-SHA256 (أول 8 أحرف)
     */
    private fun verifyKey(deviceId: String, key: String): ActivationResult {
        val cleaned = key.trim().uppercase().replace(" ", "")

        if (!cleaned.startsWith("SM26-")) {
            return ActivationResult.Failure("صيغة الكود غير صحيحة (يجب أن يبدأ بـ SM26-)")
        }

        val parts = cleaned.removePrefix("SM26-").split("-")
        if (parts.size != 4) {
            return ActivationResult.Failure("صيغة الكود غير صحيحة (يجب أن يحتوي على 4 مجموعات)")
        }

        val suffix = parts[0]
        val expiryHex = parts[1]
        val sig = parts[2] + parts[3]

        // التحقق من كود الجهاز
        val cleanDeviceId = deviceId.replace("-", "").uppercase()
        val mySuffix = cleanDeviceId.takeLast(4)

        if (mySuffix != suffix) {
            return ActivationResult.Failure("هذا الكود لا يصلح لجهازك (مرتبط بجهاز آخر)")
        }

        // استخراج تاريخ الانتهاء
        val expiryDays = try {
            expiryHex.toLong(16)
        } catch (e: Exception) {
            return ActivationResult.Failure("صيغة التاريخ غير صحيحة")
        }

        // التحقق من التوقيع
        val payload = "$deviceId:$expiryDays"
        val expectedSig = hmacSha256(payload, LICENSE_SECRET).take(8).uppercase()

        if (expectedSig != sig) {
            return ActivationResult.Failure("كود التفعيل غير صالح (توقيع خاطئ)")
        }

        // حساب تاريخ الانتهاء
        val expiresAt = BASE_MILLIS + expiryDays * DAY_MS

        if (expiresAt <= System.currentTimeMillis() - GRACE_PERIOD_MS) {
            return ActivationResult.Failure("الكود منتهي الصلاحية")
        }

        return ActivationResult.Success(expiresAt)
    }

    private fun hmacSha256(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val spec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(spec)
        val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ═══════════════════════════════════
    // حذف الترخيص (عند حذف البيانات)
    // ═══════════════════════════════════

    fun clearLicense(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        sp.clear()
        sp.apply()
    }
}
