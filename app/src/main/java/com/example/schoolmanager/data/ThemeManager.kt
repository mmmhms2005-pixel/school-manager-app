package com.example.schoolmanager.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeManager {
    private const val PREFS = "theme_prefs"
    private const val KEY = "app_theme"

    // ★ القيم الممكنة: "light", "dark", "system"
    var currentTheme by mutableStateOf("system")
        private set

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        currentTheme = prefs.getString(KEY, "system") ?: "system"
    }

    fun setTheme(context: Context, theme: String) {
        currentTheme = theme
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, theme).apply()
    }

    fun isDark(context: Context): Boolean {
        return when (currentTheme) {
            "dark" -> true
            "light" -> false
            else -> {
                // "system" → اتبع إعدادات النظام
                val uiMode = context.resources.configuration.uiMode
                (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
}
