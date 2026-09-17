package com.example.schoolmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(nav: NavController) {
    val ctx = LocalContext.current

    // معلومات الإصدار
    val versionName = remember {
        try {
            val pInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) { "1.0" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("حول التطبيق", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
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
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(20.dp))

            // ═══ الشعار الرمزي ═══
            Box(
                Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("📋", fontSize = 60.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "نظام المدرسة الذكي",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    "الإصدار $versionName",
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.height(24.dp))

            // ═══ الوصف ═══
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "📖 عن التطبيق",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "تطبيق متكامل لإدارة المدارس يعمل بالكامل بدون إنترنت، ويوفر أدوات شاملة لإدارة الطلاب والصفوف والمواد والمعلمين والدرجات، مع نظام تقارير احترافي يدعم الطباعة بصيغة PDF.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ═══ المميزات ═══
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "✨ المميزات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))

                    FeatureItem("👨‍🎓", "إدارة كاملة للطلاب (فردي وجماعي)")
                    FeatureItem("🏫", "الصفوف والشعب مع تحديد مربي الصف")
                    FeatureItem("📚", "المواد الدراسية وربطها بالصفوف")
                    FeatureItem("👨‍🏫", "المعلمون مع ربط كل معلم بمواده وصفوفه")
                    FeatureItem("📝", "إدخال الدرجات بنظام ذكي للتنقل")
                    FeatureItem("📊", "6 أنواع من الكشوفات والتقارير")
                    FeatureItem("🖨️", "توليد PDF احترافي مع الصفحات المتعددة")
                    FeatureItem("💾", "النسخ الاحتياطي والاستعادة")
                    FeatureItem("📴", "يعمل بالكامل بدون إنترنت")
                }
            }

            Spacer(Modifier.height(16.dp))

            // ═══ معلومات المطوّر ═══
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "👨‍💻 المطوّر",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "تم تطوير هذا التطبيق بعناية لخدمة المدارس.\nلأي استفسار أو اقتراح، تواصل معنا.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ═══ حقوق النشر ═══
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "© 2026 - جميع الحقوق محفوظة",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "صُنع بـ ❤️ لخدمة التعليم",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun FeatureItem(emoji: String, text: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 12.sp, lineHeight = 18.sp)
    }
}
