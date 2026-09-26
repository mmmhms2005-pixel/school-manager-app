package com.example.schoolmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.schoolmanager.SchoolApplication
import java.util.Calendar

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

    var showMenu by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf<MenuOption?>(null) }

    // ★★★ تحية ديناميكية حسب الوقت ★★★
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "صباح الخير ☀️"
            hour < 17 -> "مساء الخير 🌤️"
            else -> "مساء الخير 🌙"
        }
    }

    val items = listOf(
        NavItem("students", "الطلاب", "👨‍🎓", "إدارة بيانات الطلاب"),
        NavItem("classes", "الصفوف والشعب", "🏫", "إدارة الصفوف"),
        NavItem("subjects", "المواد الدراسية", "📚", "إدارة المواد"),
        NavItem("teachers", "المعلمون", "👨‍🏫", "إدارة المعلمين"),
        NavItem("grades", "الدرجات", "📝", "إدخال وإدارة الدرجات"),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("نظام المدرسة الذكي", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "القائمة")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ═══ بطاقة الترحيب ═══
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        greeting,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "أهلاً بك في نظام إدارة المدرسة",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ═══ عنوان الإحصائيات ═══
            Text(
                "📊 نظرة سريعة",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // ═══ شبكة الإحصائيات 2×2 ═══
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    emoji = "👨‍🎓",
                    label = "الطلاب",
                    value = students.size.toString(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    emoji = "👨‍🏫",
                    label = "المعلمون",
                    value = teachers.size.toString(),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    emoji = "🏫",
                    label = "الصفوف",
                    value = classes.size.toString(),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    emoji = "📚",
                    label = "المواد",
                    value = subjects.size.toString(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // ═══ عنوان الأقسام ═══
            Text(
                "🎯 الأقسام الرئيسية",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // ═══ شبكة الأقسام ═══
            items.chunked(2).forEach { rowItems ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { item ->
                        Card(
                            onClick = { nav.navigate(item.route) },
                            modifier = Modifier.weight(1f).height(140.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    // إذا كان العدد فردياً، نضيف مساحة فارغة
                    if (rowItems.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // ★ قائمة ☰ المنبثقة
    if (showMenu) {
        AppMenuPopup(
            onDismiss = { showMenu = false },
            onOptionSelected = { option -> selectedOption = option }
        )
    }

    // ★ النوافذ المنبثقة للأقسام
    when (selectedOption) {
        MenuOption.LICENSE -> LicenseDialog(onDismiss = { selectedOption = null })
        MenuOption.SECURITY -> SecurityDialog(onDismiss = { selectedOption = null })
        MenuOption.BACKUP -> BackupDialog(onDismiss = { selectedOption = null })
        MenuOption.DANGER -> DangerZoneDialog(onDismiss = { selectedOption = null })
        MenuOption.ABOUT -> AboutDialog(onDismiss = { selectedOption = null })
        null -> {}
        else -> { }
    }
}

// ═══ بطاقة إحصائية ═══
@Composable
private fun StatCard(
    emoji: String,
    label: String,
    value: String,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 30.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(
                    label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
