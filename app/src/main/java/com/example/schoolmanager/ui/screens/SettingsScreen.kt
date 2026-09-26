package com.example.schoolmanager.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.schoolmanager.SchoolApplication
import com.example.schoolmanager.data.LanguageManager
import com.example.schoolmanager.data.SchoolSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SchoolApplication
    val dao = app.database.dao()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ★ لإعادة تحميل الشاشة عند تغيير اللغة
    val currentLang = LanguageManager.currentLanguage

    var schoolName by remember { mutableStateOf("") }
    var academicYear by remember { mutableStateOf("") }
    var principalName by remember { mutableStateOf("") }
    var logoBase64 by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        var s = dao.settings().first()
        var retries = 0
        while (s == null && retries < 15) {
            delay(100)
            s = dao.settings().first()
            retries++
        }
        if (s != null) {
            schoolName = s.schoolName
            academicYear = s.academicYear
            principalName = s.principalName
            logoBase64 = s.logoBase64
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = ctx.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val resized = resizeBitmap(bitmap, 400)
                    val baos = ByteArrayOutputStream()
                    resized.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    val bytes = baos.toByteArray()
                    logoBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            } catch (e: Exception) { }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (currentLang == "ar") "بيانات المدرسة" else "School Data",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = if (currentLang == "ar") "رجوع" else "Back"
                        )
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ═══════════════════════════════════
            // ★★★ قسم اللغة ★★★
            // ═══════════════════════════════════
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        LanguageManager.t("language"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ★ زر العربية
                        LanguageButton(
                            text = "العربية",
                            selected = currentLang == "ar",
                            onClick = {
                                LanguageManager.setLanguage(ctx, "ar")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        // ★ زر الإنجليزية
                        LanguageButton(
                            text = "English",
                            selected = currentLang == "en",
                            onClick = {
                                LanguageManager.setLanguage(ctx, "en")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ═══ معلومات المدرسة ═══
            Card(Modifier.fillMaxWidth()) {
                Text(
                    if (currentLang == "ar")
                        "📌 هذه البيانات تظهر تلقائياً في ترويسة الكشوفات والتقارير المطبوعة."
                    else
                        "📌 This data appears automatically in the header of printed reports.",
                    Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = schoolName,
                onValueChange = { schoolName = it },
                label = {
                    Text(if (currentLang == "ar") "🏫 اسم المدرسة" else "🏫 School Name")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = academicYear,
                onValueChange = { academicYear = it },
                label = {
                    Text(if (currentLang == "ar") "📅 العام الدراسي" else "📅 Academic Year")
                },
                placeholder = {
                    Text(if (currentLang == "ar") "مثال: 1446هـ" else "e.g., 2024-2025")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = principalName,
                onValueChange = { principalName = it },
                label = {
                    Text(if (currentLang == "ar") "👨‍💼 مدير المدرسة" else "👨‍💼 Principal Name")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(20.dp))

            Text(
                if (currentLang == "ar") "🖼️ شعار المدرسة" else "🖼️ School Logo",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))

            if (logoBase64.isNotBlank()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val bitmap = remember(logoBase64) {
                            try {
                                val bytes = Base64.decode(logoBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) { null }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Logo",
                                modifier = Modifier.size(120.dp)
                            )
                        } else {
                            Text(
                                if (currentLang == "ar") "⚠️ تعذر عرض الصورة" else "⚠️ Cannot display image",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (logoBase64.isBlank()) {
                            if (currentLang == "ar") "اختيار شعار" else "Choose Logo"
                        } else {
                            if (currentLang == "ar") "تغيير الشعار" else "Change Logo"
                        }
                    )
                }
                if (logoBase64.isNotBlank()) {
                    OutlinedButton(
                        onClick = { logoBase64 = "" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (currentLang == "ar") "حذف" else "Delete")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            dao.saveSettings(
                                SchoolSettings(
                                    id = 1,
                                    schoolName = schoolName.trim(),
                                    academicYear = academicYear.trim(),
                                    principalName = principalName.trim(),
                                    logoBase64 = logoBase64
                                )
                            )
                            snackbarHostState.showSnackbar(
                                if (currentLang == "ar") "✅ تم حفظ بيانات المدرسة"
                                else "✅ School data saved",
                                duration = SnackbarDuration.Short
                            )
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("❌ ${e.message ?: "Error"}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (currentLang == "ar") "حفظ البيانات" else "Save Data",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

// ═══ زر اللغة ═══
@Composable
private fun LanguageButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(text, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text)
        }
    }
}

private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxSize && height <= maxSize) return bitmap
    val ratio = width.toFloat() / height.toFloat()
    val (newW, newH) = if (width > height) {
        maxSize to (maxSize / ratio).toInt()
    } else {
        (maxSize * ratio).toInt() to maxSize
    }
    return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
}
