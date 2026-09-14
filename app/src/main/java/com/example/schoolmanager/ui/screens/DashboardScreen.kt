package com.example.schoolmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.schoolmanager.SchoolApplication

data class NavItem(val route: String, val title: String, val emoji: String, val desc: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SchoolApplication
    val dao = app.database.dao()

    val students by dao.students().collectAsState(initial = emptyList())
    val classes by dao.classes().collectAsState(initial = emptyList())
    val subjects by dao.subjects().collectAsState(initial = emptyList())
    val teachers by dao.teachers().collectAsState(initial = emptyList())

    val items = listOf(
        NavItem("students", "الطلاب", "👨‍🎓", "إدارة بيانات الطلاب"),
        NavItem("classes", "الصفوف والشعب", "🏫", "إدارة الصفوف"),
        NavItem("subjects", "المواد الدراسية", "📚", "إدارة المواد"),
        NavItem("teachers", "المعلمون", "👨‍🏫", "إدارة المعلمين"),
        NavItem("grades", "الدرجات", "📝", "إدخال وإدارة الدرجات"),
        NavItem("reports", "الكشوفات والتقارير", "📋", "إصدار التقارير"),
        NavItem("settings", "بيانات المدرسة", "⚙️", "إعدادات المدرسة")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة المدرسة", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "أهلاً بك في نظام إدارة المدرسة 👋",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "اختر قسماً من الأيقونات أدناه",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCell("الطلاب", students.size.toString())
                    StatCell("الصفوف", classes.size.toString())
                    StatCell("المواد", subjects.size.toString())
                    StatCell("المعلمون", teachers.size.toString())
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { item ->
                    Card(
                        onClick = { nav.navigate(item.route) },
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(item.emoji, fontSize = 38.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                item.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                item.desc,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
    }
}
