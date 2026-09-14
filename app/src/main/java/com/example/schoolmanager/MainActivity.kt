package com.example.schoolmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.schoolmanager.ui.screens.ClassesScreen
import com.example.schoolmanager.ui.screens.DashboardScreen
import com.example.schoolmanager.ui.screens.GradesScreen
import com.example.schoolmanager.ui.screens.StudentsScreen
import com.example.schoolmanager.ui.screens.SubjectsScreen
import com.example.schoolmanager.ui.screens.TeachersScreen
import com.example.schoolmanager.ui.theme.SchoolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SchoolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNav()
                }
            }
        }
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
        composable("reports") { PlaceholderScreen("الكشوفات والتقارير") }
        composable("settings") { PlaceholderScreen("بيانات المدرسة") }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}
