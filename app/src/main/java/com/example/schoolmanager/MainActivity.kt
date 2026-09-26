package com.example.schoolmanager

import android.os.Bundle
import com.example.schoolmanager.ui.screens.PdfPreviewScreen
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.schoolmanager.data.LanguageManager
import com.example.schoolmanager.data.LicenseManager
import com.example.schoolmanager.data.SecurityManager
import com.example.schoolmanager.data.ThemeManager
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

        // ★★★ تهيئة مدير اللغة ★★★
        LanguageManager.initialize(this)

        // ★★★ تهيئة مدير الوضع الليلي ★★★
        ThemeManager.initialize(this)

        setContent {
            // ★★★ تطبيق الوضع الليلي بناءً على الإعداد ★★★
            SchoolTheme(useDark = ThemeManager.isDark(LocalContext.current)) {
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
    var licenseOk by remember { mutableStateOf(LicenseManager.isActivated(ctx)) }
    val pinEnabled = SecurityManager.pinEnabledState.value
    val locked = SecurityManager.isLockedState.value

    if (!licenseOk) {
        ActivationScreen(onActivated = { licenseOk = true })
    } else if (pinEnabled && locked) {
        LockScreen()
    } else {
        AppNav()
    }
}

// ═══ بيانات عنصر شريط التنقل السفلي ═══
data class BottomNavItem(
    val route: String,
    val labelKey: String,
    val icon: ImageVector
)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // ★ عناصر شريط التنقل السفلي (بمفاتيح الترجمة)
    val bottomItems = listOf(
        BottomNavItem("dashboard", "home", Icons.Default.Home),
        BottomNavItem("students", "students", Icons.Default.Person),
        BottomNavItem("reports", "reports", Icons.Default.Description),
        BottomNavItem("settings", "settings", Icons.Default.Settings)
    )

    val showBottomBar = currentRoute != null && !currentRoute.startsWith("pdf_preview")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                // ★ التنقل المبسّط (بدون saveState/restoreState)
                                if (currentRoute != item.route) {
                                    nav.navigate(item.route) {
                                        popUpTo("dashboard") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                Icon(item.icon, contentDescription = LanguageManager.t(item.labelKey))
                            },
                            label = {
                                Text(LanguageManager.t(item.labelKey), fontSize = 11.sp)
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding)
        ) {
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
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}
