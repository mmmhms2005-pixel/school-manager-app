package com.example.schoolmanager.data

import android.content.Context
import com.example.schoolmanager.data.PdfGenerator.MonthlyCertEntry
import com.example.schoolmanager.data.PdfGenerator.SchoolInfo
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object WhatsAppHelper {

    fun buildMonthlyCertMessage(
    entry: MonthlyCertEntry,
    school: SchoolInfo,
    month: String,
    customNote: String = ""
): String {
    val sb = StringBuilder()
    sb.append("🏫 *${school.schoolName}*\n")
    sb.append("📅 العام الدراسي: ${school.academicYear}\n")
    sb.append("━━━━━━━━━━━━━━━━━━━\n\n")
    sb.append("📜 *شهادة الطالب الشهرية*\n")
    sb.append("📅 الشهر: $month\n\n")
    sb.append("👤 الطالب: ${entry.studentName}\n")
    sb.append("📚 الصف: ${entry.className} - ${entry.sectionName}\n\n")
    sb.append("*المواد والدرجات:*\n")

    var totalScore = 0
    var totalMax = 0
    entry.grades.forEach { g: PdfGenerator.SubjectGrade ->
        sb.append("• ${g.subjectName}: ${g.score}/${g.maxScore}\n")
        totalScore += g.score
        totalMax += g.maxScore
    }

    val avg = if (totalMax > 0) (totalScore.toDouble() / totalMax * 20) else 0.0
    val rating = when {
        avg >= 18 -> "ممتاز"
        avg >= 16 -> "جيد جداً"
        avg >= 14 -> "جيد"
        avg >= 10 -> "مقبول"
        else -> "راسب"
    }

    sb.append("\n*المجموع:* $totalScore/$totalMax\n")
    sb.append("*المتوسط:* ${String.format("%.2f", avg)}/20\n")
    sb.append("*التقدير:* $rating\n\n")

    // ★★★ إضافة الملاحظة إذا كانت موجودة ★★★
    if (customNote.isNotBlank()) {
        sb.append("━━━━━━━━━━━━━━━━━━━\n")
        sb.append("📝 *ملاحظة الإدارة:*\n")
        sb.append("$customNote\n")
        sb.append("━━━━━━━━━━━━━━━━━━━\n\n")
    }

    sb.append("مع تحيات إدارة ${school.schoolName}")

    return sb.toString()
}

    fun sendViaWhatsApp(context: Context, phoneNumber: String, message: String) {
        try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            val url = "https://wa.me/$cleanNumber?text=${URLEncoder.encode(message, "UTF-8")}"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
  }
    fun buildNoteOnlyMessage(
    entry: MonthlyCertEntry,
    school: SchoolInfo,
    customNote: String
): String {
    val sb = StringBuilder()
    sb.append("🏫 *${school.schoolName}*\n")
    sb.append("📅 العام الدراسي: ${school.academicYear}\n")
    sb.append("━━━━━━━━━━━━━━━━━━━\n\n")
    sb.append("📝 *ملاحظة إلى ولي أمر الطالب:*\n")
    sb.append("👤 الطالب: ${entry.studentName}\n")
    sb.append("📚 الصف: ${entry.className} - ${entry.sectionName}\n\n")
    sb.append("━━━━━━━━━━━━━━━━━━━\n")
    sb.append("$customNote\n")
    sb.append("━━━━━━━━━━━━━━━━━━━\n\n")
    sb.append("مع تحيات إدارة ${school.schoolName}")
    return sb.toString()
}

fun sendViaSms(context: Context, phoneNumber: String, message: String) {
    try {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:$cleanNumber")
            putExtra("sms_body", message)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
}
