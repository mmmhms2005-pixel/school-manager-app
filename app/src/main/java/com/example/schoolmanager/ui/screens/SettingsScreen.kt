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
                title = { Text("بيانات المدرسة", fontWeight = FontWeight.Bold) },
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "📌 هذه البيانات تظهر تلقائياً في ترويسة الكشوفات والتقارير المطبوعة.",
                    Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = schoolName,
                onValueChange = { schoolName = it },
                label = { Text("🏫 اسم المدرسة") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = academicYear,
                onValueChange = { academicYear = it },
                label = { Text("📅 العام الدراسي") },
                placeholder = { Text("مثال: 1446هـ") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = principalName,
                onValueChange = { principalName = it },
                label = { Text("👨‍💼 مدير المدرسة") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(20.dp))

            Text("🖼️ شعار المدرسة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                contentDescription = "شعار المدرسة",
                                modifier = Modifier.size(120.dp)
                            )
                        } else {
                            Text("⚠️ تعذر عرض الصورة", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error)
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
                    Text(if (logoBase64.isBlank()) "اختيار شعار" else "تغيير الشعار")
                }
                if (logoBase64.isNotBlank()) {
                    OutlinedButton(
                        onClick = { logoBase64 = "" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("حذف")
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
                                "✅ تم حفظ بيانات المدرسة",
                                duration = SnackbarDuration.Short
                            )
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("❌ ${e.message ?: "خطأ"}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("حفظ البيانات", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(30.dp))
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
