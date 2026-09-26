package com.example.schoolmanager.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object LanguageManager {
    private const val PREFS = "language_prefs"
    private const val KEY = "app_language"

    // ★ اللغة الحالية: "ar" أو "en"
    var currentLanguage by mutableStateOf("ar")
        private set

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        currentLanguage = prefs.getString(KEY, "ar") ?: "ar"
    }

    fun setLanguage(context: Context, lang: String) {
        currentLanguage = lang
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, lang).apply()
    }

    fun isArabic(): Boolean = currentLanguage == "ar"

    fun t(key: String): String {
        return translations[currentLanguage]?.get(key)
            ?: translations["ar"]?.get(key)
            ?: key
    }

    private val translations: Map<String, Map<String, String>> = mapOf(
        "ar" to mapOf(
            // ═══ عام ═══
            "app_name" to "نظام المدرسة الذكي",
            "welcome" to "أهلاً بك في نظام إدارة المدرسة",
            "morning" to "صباح الخير ☀️",
            "afternoon" to "مساء الخير 🌤️",
            "evening" to "مساء الخير 🌙",

            // ═══ الشريط السفلي ═══
            "home" to "الرئيسية",
            "students" to "الطلاب",
            "reports" to "التقارير",
            "settings" to "الإعدادات",

            // ═══ الأقسام ═══
            "classes" to "الصفوف والشعب",
            "subjects" to "المواد الدراسية",
            "teachers" to "المعلمون",
            "grades" to "الدرجات",

            // ═══ لوحة التحكم ═══
            "quick_view" to "📊 نظرة سريعة",
            "main_sections" to "🎯 الأقسام الرئيسية",
            "students_count" to "الطلاب",
            "teachers_count" to "المعلمون",
            "classes_count" to "الصفوف",
            "subjects_count" to "المواد",
            "manage_students" to "إدارة بيانات الطلاب",
            "manage_classes" to "إدارة الصفوف",
            "manage_subjects" to "إدارة المواد",
            "manage_teachers" to "إدارة المعلمين",
            "manage_grades" to "إدخال وإدارة الدرجات",

            // ═══ الإعدادات ═══
            "language" to "🌐 اللغة",
            "choose_language" to "اختر اللغة",
            "arabic" to "العربية",
            "english" to "English",

            // ═══ عام ═══
            "save" to "حفظ",
            "cancel" to "إلغاء",
            "delete" to "حذف",
            "edit" to "تعديل",
            "add" to "إضافة",
            "search" to "بحث",
            "close" to "إغلاق",
            "ok" to "موافق",
            "yes" to "نعم",
            "no" to "لا",
            "loading" to "جار التحميل...",
            "error" to "خطأ"
        ),
        "en" to mapOf(
            // ═══ General ═══
            "app_name" to "Smart School System",
            "welcome" to "Welcome to School Management System",
            "morning" to "Good Morning ☀️",
            "afternoon" to "Good Afternoon 🌤️",
            "evening" to "Good Evening 🌙",

            // ═══ Bottom Bar ═══
            "home" to "Home",
            "students" to "Students",
            "reports" to "Reports",
            "settings" to "Settings",

            // ═══ Sections ═══
            "classes" to "Classes",
            "subjects" to "Subjects",
            "teachers" to "Teachers",
            "grades" to "Grades",

            // ═══ Dashboard ═══
            "quick_view" to "📊 Quick View",
            "main_sections" to "🎯 Main Sections",
            "students_count" to "Students",
            "teachers_count" to "Teachers",
            "classes_count" to "Classes",
            "subjects_count" to "Subjects",
            "manage_students" to "Manage Students",
            "manage_classes" to "Manage Classes",
            "manage_subjects" to "Manage Subjects",
            "manage_teachers" to "Manage Teachers",
            "manage_grades" to "Manage Grades",

            // ═══ Settings ═══
            "language" to "🌐 Language",
            "choose_language" to "Choose Language",
            "arabic" to "العربية",
            "english" to "English",

            // ═══ General ═══
            "save" to "Save",
            "cancel" to "Cancel",
            "delete" to "Delete",
            "edit" to "Edit",
            "add" to "Add",
            "search" to "Search",
            "close" to "Close",
            "ok" to "OK",
            "yes" to "Yes",
            "no" to "No",
            "loading" to "Loading...",
            "error" to "Error"
        )
    )
}
