package com.example.schoolmanager

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.schoolmanager.data.LicenseManager
import com.example.schoolmanager.data.SecurityManager
import com.example.schoolmanager.ui.screens.AboutScreen
import com.example.schoolmanager.ui.screens.ActivationScreen
import com.example.schoolmanager.ui.screens.ClassesScreen
import com.example.schoolmanager.ui.screens.DashboardScreen
import com.example.schoolmanager.ui.screens.GradesScreen
import com.example.schoolmanager.ui.screens.LockScreen
import com.example.schoolmanager.ui.screens.ReportsScreen
import com.example.schoolmanager.ui.screens.SettingsScreen
import com.example.schoolmanager.ui.screens.StudentsScreen
import com.example.schoolmanager.ui.screens.SubjectsScreen
import com.example.schoolmanager.ui.screens.TeachersScreen
import com.example.schoolmanager.ui.theme.SchoolTheme

class MainActivity : ComponentActivity() {

    private var backgroundTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ★ منع لقطات الشاشة وتسجيلها
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()

        // ★ تهيئة مدير الأمان
        SecurityManager.initialize(this)

        setContent {
            SchoolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundTime = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        // ★ القفل التلقائي بعد فترة الخمول
        if (backgroundTime > 0L && SecurityManager.isPinEnabled(this)) {
            val elapsed = System.currentTimeMillis() - backgroundTime
            val limit = SecurityManager.getAutoLockMinutes(this) * 60_000L
            if (elapsed > limit) {
                SecurityManager.lock()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val ctx = LocalContext.current

    // ═══════════════════════════════════
    // ★ حالة الترخيص
    // ═══════════════════════════════════
    var licenseOk by remember { mutableStateOf(LicenseManager.isActivated(ctx)) }

    // ═══════════════════════════════════
    // ★ حالة القفل بالـ PIN
    // ═══════════════════════════════════
    val pinEnabled = SecurityManager.pinEnabledState.value
    val locked = SecurityManager.isLockedState.value

    // ═══════════════════════════════════
    // ★ الترتيب المهم:
    // 1. إذا لم يُفعَّل → شاشة التفعيل
    // 2. إذا كان PIN مفعَّلاً ومقفلاً → شاشة القفل
    // 3. وإلا → التطبيق
    // ═══════════════════════════════════

    if (!licenseOk) {
        // ★ الشاشة الأولى: التفعيل
        ActivationScreen(
            onActivated = {
                licenseOk = true
            }
        )
    } else if (pinEnabled && locked) {
        // ★ الشاشة الثانية: القفل بالـ PIN
        LockScreen()
    } else {
        // ★ الشاشة الثالثة: التطبيق
        AppNav()
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "dashboard") {
        composable("dashboard") { DashboardScreen(nav) }
        composable("students") { StudentsScreen(nav) }
        composable("classes") { ClassesScreen(nav) }
        composable("subjects") { SubjectsScreen(nav) }
        composable("teachers") { TeachersScreen(nav) }
        composable("grades") { GradesScreen(nav) }
        composable("reports") { ReportsScreen(nav) }
        composable("settings") { SettingsScreen(nav) }
        composable("about") { AboutScreen(nav) }
        composable("pdf_preview/{filePath}") { backStackEntry ->
    val filePath = backStackEntry.arguments?.getString("filePath") ?: ""
    PdfPreviewScreen(nav = nav, filePath = filePath)
}
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}
