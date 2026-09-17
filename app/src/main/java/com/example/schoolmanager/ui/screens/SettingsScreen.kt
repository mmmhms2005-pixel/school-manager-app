package com.example.schoolmanager.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.schoolmanager.SchoolApplication
import com.example.schoolmanager.data.BackupManager
import com.example.schoolmanager.data.DeviceIdManager
import com.example.schoolmanager.data.LicenseManager
import com.example.schoolmanager.data.SchoolClass
import com.example.schoolmanager.data.SchoolSettings
import com.example.schoolmanager.data.SecurityManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SchoolApplication
    val dao = app.database.dao()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current

    // ═══ بيانات المدرسة ═══
    var schoolName by remember { mutableStateOf("") }
    var academicYear by remember { mutableStateOf("") }
    var principalName by remember { mutableStateOf("") }
    var logoBase64 by remember { mutableStateOf("") }

    // ═══ الحماية ═══
    var pinEnabled by remember { mutableStateOf(SecurityManager.isPinEnabled(ctx)) }
    var autoLockMinutes by remember { mutableStateOf(SecurityManager.getAutoLockMinutes(ctx).toString()) }
    var encryptBackup by remember { mutableStateOf(SecurityManager.isBackupEncryptionEnabled(ctx)) }

    // ═══ الترخيص ═══
    val deviceId = remember { DeviceIdManager.getDeviceId(ctx) }
    var licenseStatus by remember { mutableStateOf(LicenseManager.getStatus(ctx)) }

    // ═══ حوارات ═══
    var showPinDialog by remember { mutableStateOf(false) }
    var pinMode by remember { mutableStateOf("set") }
    var showRemovePinDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var pendingBackupUri by remember { mutableStateOf<Uri?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var restoreNeedsPassword by remember { mutableStateOf(false) }

    // ★ حوارات حذف البيانات
    var showDeleteWarning by remember { mutableStateOf(false) }
    var showDeleteBackupOffer by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDeletePinDialog by remember { mutableStateOf(false) }

    // ★ حوار التفعيل الجديد
    var showReactivationDialog by remember { mutableStateOf(false) }
    var newLicenseKey by remember { mutableStateOf("") }
    var reactivationError by remember { mutableStateOf("") }

    // تحميل الإعدادات
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
        // ★ تحديث حالة الترخيص
        licenseStatus = LicenseManager.getStatus(ctx)
    }

    // منتقي الصور
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
                    logoBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                }
            } catch (e: Exception) { }
        }
    }

    // منتقي النسخة الاحتياطية
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            if (encryptBackup) {
                pendingBackupUri = it
                showPasswordDialog = true
            } else {
                scope.launch {
                    val result = BackupManager.exportBackup(ctx, it, null)
                    if (result.isSuccess) {
                        snackbarHostState.showSnackbar("✅ تم الحفظ (${result.getOrNull()} سجل)")
                    } else {
                        snackbarHostState.showSnackbar("❌ ${result.exceptionOrNull()?.message ?: "خطأ"}")
                    }
                }
            }
        }
    }

    // منتقي النسخة الاحتياطية قبل الحذف
    val deleteBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = BackupManager.exportBackup(ctx, it, null)
                if (result.isSuccess) {
                    snackbarHostState.showSnackbar("✅ تم الحفظ (${result.getOrNull()} سجل)")
                    delay(800)
                    showDeleteConfirm = true
                } else {
                    snackbarHostState.showSnackbar("❌ فشل الحفظ — لم يتم الحذف")
                }
            }
        } ?: run {
            showDeleteBackupOffer = true
        }
    }

    // منتقي ملف الاستعادة
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingRestoreUri = it
            scope.launch {
                try {
                    val raw = ctx.contentResolver.openInputStream(it)?.use { input ->
                        val bytes = ByteArray(60)
                        val n = input.read(bytes)
                        String(bytes, 0, n.coerceAtLeast(0), Charsets.UTF_8)
                    } ?: ""
                    restoreNeedsPassword = raw.startsWith("ENCRYPTED:")
                    showRestoreConfirmDialog = true
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("❌ تعذر قراءة الملف")
                }
            }
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
            // ═══════════════════════════════════
            // ★★★ قسم الترخيص (في الأعلى) ★★★
            // ═══════════════════════════════════

            LicenseStatusCard(
                status = licenseStatus,
                deviceId = deviceId,
                onCopyDeviceId = {
                    clipboard.setText(AnnotatedString(deviceId))
                    scope.launch {
                        snackbarHostState.showSnackbar("✅ تم نسخ كود الجهاز")
                    }
                },
                onReactivate = { showReactivationDialog = true }
            )

            Spacer(Modifier.height(24.dp))

            // ═══════════════════════════════════
            // بيانات المدرسة
            // ═══════════════════════════════════
            Text(
                "🏫 بيانات المدرسة",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))

            Card(Modifier.fillMaxWidth()) {
                Text(
                    "📌 هذه البيانات تظهر تلقائياً في ترويسة الكشوفات والتقارير المطبوعة.",
                    Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))

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

            Spacer(Modifier.height(20.dp))
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

            // ═══ قسم الحماية ═══
            Spacer(Modifier.height(32.dp))
            Divider()
            Spacer(Modifier.height(20.dp))

            Text(
                "🔒 الحماية والأمان",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("قفل التطبيق", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                if (pinEnabled) "مُفعَّل — التطبيق محمي برقم سري"
                                else "غير مُفعَّل",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Switch(
                            checked = pinEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    pinMode = "set"
                                    showPinDialog = true
                                } else {
                                    showRemovePinDialog = true
                                }
                            }
                        )
                    }

                    if (pinEnabled) {
                        Spacer(Modifier.height(12.dp))
                        Divider()
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                pinMode = "change"
                                showPinDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🔑 تغيير الرقم السري")
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("⏱️ القفل التلقائي بعد",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("المدة التي يُقفل فيها التطبيق تلقائياً بعد تركه",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 5, 10, 30).forEach { m ->
                            FilterChip(
                                selected = autoLockMinutes == m.toString(),
                                onClick = {
                                    autoLockMinutes = m.toString()
                                    SecurityManager.setAutoLockMinutes(ctx, m)
                                },
                                label = { Text("$m د", fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("🔐 تشفير النسخ الاحتياطية",
                            fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "تشفير ملف النسخة الاحتياطية بكلمة مرور (AES-256)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Switch(
                        checked = encryptBackup,
                        onCheckedChange = {
                            encryptBackup = it
                            SecurityManager.setBackupEncryptionEnabled(ctx, it)
                        }
                    )
                }
            }

            // ═══ قسم النسخ الاحتياطي ═══
            Spacer(Modifier.height(32.dp))
            Divider()
            Spacer(Modifier.height(20.dp))

            Text(
                "💾 النسخ الاحتياطي والاستعادة",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Text(
                    "📌 احفظ نسخة كاملة من بياناتك في ملف JSON، لاستعادتها عند الحاجة." +
                            if (encryptBackup) " ⚠️ التشفير مُفعَّل: ستحتاج كلمة مرور."
                            else " يمكنك تفعيل التشفير من قسم الحماية.",
                    Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { backupLauncher.launch(BackupManager.suggestFileName()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("💾 إنشاء نسخة احتياطية", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { restoreLauncher.launch("application/json") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("♻️ استعادة من نسخة احتياطية", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "⚠️ عند الاستعادة، ستُحذف جميع البيانات الحالية وتُستبدل ببيانات النسخة.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error
            )

            // ═══ منطقة الخطر ═══
            Spacer(Modifier.height(32.dp))
            Divider(color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "⚠️ منطقة الخطر",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(8.dp))

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    "📌 الإجراءات في هذا القسم لا يمكن التراجع عنها. تأكد قبل المتابعة.",
                    Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { showDeleteWarning = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("🗑️ حذف جميع البيانات", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // ═══════════════════════════════════
    // ★ حوار إعادة التفعيل
    // ═══════════════════════════════════
    if (showReactivationDialog) {
        AlertDialog(
            onDismissRequest = {
                showReactivationDialog = false
                newLicenseKey = ""
                reactivationError = ""
            },
            icon = { Text("🔑", fontSize = 32.sp) },
            title = { Text("تجديد الاشتراك") },
            text = {
                Column {
                    Text(
                        "أدخل كود التفعيل الجديد الذي حصلت عليه من البائع:",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newLicenseKey,
                        onValueChange = {
                            newLicenseKey = it.uppercase().filter { c ->
                                c.isLetterOrDigit() || c == '-'
                            }
                            reactivationError = ""
                        },
                        label = { Text("كود التفعيل") },
                        placeholder = { Text("SM26-XXXX-XXXX-XXXX-XXXX") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = reactivationError.isNotBlank(),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )
                    if (reactivationError.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "❌ $reactivationError",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newLicenseKey.trim().length >= 10,
                    onClick = {
                        val result = LicenseManager.verifyAndActivate(ctx, newLicenseKey)
                        when (result) {
                            is LicenseManager.ActivationResult.Success -> {
                                licenseStatus = LicenseManager.getStatus(ctx)
                                showReactivationDialog = false
                                newLicenseKey = ""
                                reactivationError = ""
                                scope.launch {
                                    snackbarHostState.showSnackbar("✅ تم تجديد الاشتراك بنجاح")
                                }
                            }
                            is LicenseManager.ActivationResult.Failure -> {
                                reactivationError = result.reason
                            }
                        }
                    }
                ) { Text("تفعيل") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReactivationDialog = false
                    newLicenseKey = ""
                    reactivationError = ""
                }) { Text("إلغاء") }
            }
        )
    }

    // ═══════════════════════════════════
    // حوار PIN (تعيين / تغيير)
    // ═══════════════════════════════════
    if (showPinDialog) {
        var pin1 by remember { mutableStateOf("") }
        var pin2 by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            icon = { Text("🔐", fontSize = 28.sp) },
            title = { Text(if (pinMode == "set") "تعيين رقم سري" else "تغيير الرقم السري") },
            text = {
                Column {
                    Text("أدخل رقماً مكوناً من 4 أرقام على الأقل.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pin1,
                        onValueChange = { if (it.all { c -> c.isDigit() }) pin1 = it },
                        label = { Text("الرقم السري") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pin2,
                        onValueChange = { if (it.all { c -> c.isDigit() }) pin2 = it },
                        label = { Text("تأكيد الرقم") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        pin1.length < 4 -> error = "الرقم يجب أن يكون 4 أرقام على الأقل"
                        pin1 != pin2 -> error = "الرقمان غير متطابقين"
                        else -> {
                            SecurityManager.setPin(ctx, pin1)
                            pinEnabled = true
                            showPinDialog = false
                            scope.launch { snackbarHostState.showSnackbar("✅ تم تعيين الرقم السري") }
                        }
                    }
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    pinEnabled = SecurityManager.isPinEnabled(ctx)
                }) { Text("إلغاء") }
            }
        )
    }

    // ═══════════════════════════════════
    // حوار إزالة PIN
    // ═══════════════════════════════════
    if (showRemovePinDialog) {
        var currentPin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRemovePinDialog = false },
            icon = { Text("⚠️", fontSize = 28.sp) },
            title = { Text("إلغاء تفعيل القفل") },
            text = {
                Column {
                    Text("أدخل الرقم السري الحالي للتأكيد.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { if (it.all { c -> c.isDigit() }) currentPin = it },
                        label = { Text("الرقم السري الحالي") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (SecurityManager.verifyPin(ctx, currentPin)) {
                        SecurityManager.removePin(ctx)
                        pinEnabled = false
                        showRemovePinDialog = false
                        scope.launch { snackbarHostState.showSnackbar("✅ تم إلغاء القفل") }
                    } else {
                        error = "الرقم غير صحيح"
                    }
                }) { Text("تأكيد الإلغاء", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePinDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // ═══════════════════════════════════
    // حوار كلمة مرور التشفير
    // ═══════════════════════════════════
    if (showPasswordDialog) {
        var pw by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            icon = { Text("🔐", fontSize = 28.sp) },
            title = { Text("كلمة مرور التشفير") },
            text = {
                Column {
                    Text("أدخل كلمة المرور لتشفير النسخة الاحتياطية.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pw,
                        onValueChange = { pw = it },
                        label = { Text("كلمة المرور") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("⚠️ احتفظ بكلمة المرور في مكان آمن — بدونها لا يمكن استعادة النسخة.",
                        fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pw.length < 4) {
                        error = "كلمة المرور قصيرة جداً"
                    } else {
                        val uri = pendingBackupUri
                        showPasswordDialog = false
                        if (uri != null) {
                            scope.launch {
                                val result = BackupManager.exportBackup(ctx, uri, pw)
                                if (result.isSuccess) {
                                    snackbarHostState.showSnackbar("✅ تم الحفظ (${result.getOrNull()} سجل)")
                                } else {
                                    snackbarHostState.showSnackbar("❌ ${result.exceptionOrNull()?.message ?: "خطأ"}")
                                }
                            }
                        }
                        pendingBackupUri = null
                    }
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    pendingBackupUri = null
                }) { Text("إلغاء") }
            }
        )
    }

    // ═══════════════════════════════════
    // حوار تأكيد الاستعادة
    // ═══════════════════════════════════
    if (showRestoreConfirmDialog && pendingRestoreUri != null) {
        var pw by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            icon = { Text("⚠️", fontSize = 28.sp) },
            title = { Text("تأكيد الاستعادة") },
            text = {
                Column {
                    Text(
                        "سيتم حذف جميع البيانات الحالية واستبدالها ببيانات النسخة.\nهل أنت متأكد؟"
                    )
                    if (restoreNeedsPassword) {
                        Spacer(Modifier.height(12.dp))
                        Text("🔐 الملف مشفّر — أدخل كلمة المرور:",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = pw,
                            onValueChange = { pw = it },
                            label = { Text("كلمة المرور") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingRestoreUri ?: return@TextButton
                    if (restoreNeedsPassword && pw.isBlank()) {
                        error = "أدخل كلمة المرور"
                        return@TextButton
                    }
                    showRestoreConfirmDialog = false
                    scope.launch {
                        val result = BackupManager.importBackup(
                            ctx, uri,
                            if (restoreNeedsPassword) pw else null
                        )
                        if (result.isSuccess) {
                            snackbarHostState.showSnackbar(
                                "✅ تمت الاستعادة (${result.getOrNull()} سجل)",
                                duration = SnackbarDuration.Long
                            )
                            delay(1500)
                            val s = dao.settings().first()
                            if (s != null) {
                                schoolName = s.schoolName
                                academicYear = s.academicYear
                                principalName = s.principalName
                                logoBase64 = s.logoBase64
                            }
                        } else {
                            snackbarHostState.showSnackbar(
                                "❌ ${result.exceptionOrNull()?.message ?: "فشلت الاستعادة"}",
                                duration = SnackbarDuration.Long
                            )
                        }
                    }
                    pendingRestoreUri = null
                }) { Text("نعم، استعد", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirmDialog = false
                    pendingRestoreUri = null
                }) { Text("إلغاء") }
            }
        )
    }

    // ═══════════════════════════════════
    // المرحلة 1: تحذير أولي
    // ═══════════════════════════════════
    if (showDeleteWarning) {
        AlertDialog(
            onDismissRequest = { showDeleteWarning = false },
            icon = { Text("⚠️", fontSize = 40.sp) },
            title = {
                Text(
                    "تحذير خطير!",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "سيتم حذف جميع البيانات نهائياً:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("• الطلاب والمعلمون", fontSize = 13.sp)
                    Text("• الدرجات والكشوفات", fontSize = 13.sp)
                    Text("• الصفوف والمواد والشعب", fontSize = 13.sp)
                    Text("• بيانات المدرسة والإعدادات", fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            "⚠️ لا يمكن التراجع عن هذه العملية!",
                            Modifier.padding(10.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteWarning = false
                    showDeleteBackupOffer = true
                }) {
                    Text("متابعة", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteWarning = false }) { Text("إلغاء") }
            }
        )
    }

    // المرحلة 2: اقتراح نسخة احتياطية
    if (showDeleteBackupOffer) {
        AlertDialog(
            onDismissRequest = { showDeleteBackupOffer = false },
            icon = { Text("💾", fontSize = 36.sp) },
            title = { Text("نسخة احتياطية؟", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "قبل حذف البيانات، هل تريد إنشاء نسخة احتياطية لحفظ بياناتك؟\n\n" +
                            "ننصح بشدة بالحفظ قبل الحذف."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteBackupOffer = false
                    val fname = "before_delete_${BackupManager.suggestFileName()}"
                    deleteBackupLauncher.launch(fname)
                }) {
                    Text("💾 نعم، احفظ أولاً", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteBackupOffer = false
                    showDeleteConfirm = true
                }) {
                    Text("تخطي والحذف", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // المرحلة 3: تأكيد نهائي
    if (showDeleteConfirm) {
        var typedWord by remember { mutableStateOf("") }
        val confirmWord = "حذف"

        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Text("🗑️", fontSize = 40.sp) },
            title = {
                Text(
                    "التأكيد النهائي",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "للتأكيد، اكتب كلمة \"$confirmWord\" في المربع أدناه:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = typedWord,
                        onValueChange = { typedWord = it },
                        label = { Text("اكتب كلمة: $confirmWord") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = typedWord.isNotBlank() && typedWord.trim() != confirmWord
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⚠️ هذا الإجراء نهائي ولا يمكن التراجع عنه.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = typedWord.trim() == confirmWord,
                    onClick = {
                        if (SecurityManager.isPinEnabled(ctx)) {
                            showDeleteConfirm = false
                            showDeletePinDialog = true
                        } else {
                            performDeleteAll(
                                dao = dao,
                                snackbar = snackbarHostState,
                                scope = scope,
                                onDone = {
                                    showDeleteConfirm = false
                                    schoolName = ""
                                    academicYear = ""
                                    principalName = ""
                                    logoBase64 = ""
                                }
                            )
                        }
                    }
                ) {
                    Text(
                        "🗑️ حذف نهائي",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("إلغاء") }
            }
        )
    }

    // حوار PIN قبل الحذف
    if (showDeletePinDialog) {
        var pin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDeletePinDialog = false },
            icon = { Text("🔐", fontSize = 32.sp) },
            title = { Text("تأكيد الهوية") },
            text = {
                Column {
                    Text("أدخل الرقم السري للتأكيد النهائي:",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.all { c -> c.isDigit() }) pin = it },
                        label = { Text("الرقم السري") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (SecurityManager.verifyPin(ctx, pin)) {
                        showDeletePinDialog = false
                        performDeleteAll(
                            dao = dao,
                            snackbar = snackbarHostState,
                            scope = scope,
                            onDone = {
                                schoolName = ""
                                academicYear = ""
                                principalName = ""
                                logoBase64 = ""
                            }
                        )
                    } else {
                        error = "الرقم غير صحيح"
                    }
                }) {
                    Text("تأكيد", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePinDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

// ═══════════════════════════════════
// بطاقة حالة الترخيص
// ═══════════════════════════════════
@Composable
private fun LicenseStatusCard(
    status: LicenseManager.LicenseStatus,
    deviceId: String,
    onCopyDeviceId: () -> Unit,
    onReactivate: () -> Unit
) {
    val bgColor = when {
        status.isExpired -> MaterialTheme.colorScheme.errorContainer
        status.isGracePeriod -> MaterialTheme.colorScheme.tertiaryContainer
        status.isActive -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val onBgColor = when {
        status.isExpired -> MaterialTheme.colorScheme.onErrorContainer
        status.isGracePeriod -> MaterialTheme.colorScheme.onTertiaryContainer
        status.isActive -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        status.isExpired -> "❌"
                        status.isGracePeriod -> "⏰"
                        status.isActive -> "✅"
                        else -> "🔑"
                    },
                    fontSize = 26.sp
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "الترخيص",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = onBgColor
                    )
                    Text(
                        status.message,
                        fontSize = 12.sp,
                        color = onBgColor.copy(alpha = 0.85f)
                    )
                }
            }

            if (status.expiresAt > 0L) {
                Spacer(Modifier.height(10.dp))
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                Text(
                    "📅 ينتهي في: ${fmt.format(Date(status.expiresAt))}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = onBgColor
                )
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = onBgColor.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))

            Text(
                "📱 كود الجهاز:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = onBgColor
            )
            Spacer(Modifier.height(6.dp))

            Surface(
                Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    deviceId,
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyDeviceId,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = onBgColor
                    )
                ) {
                    Text("📋 نسخ", fontSize = 12.sp)
                }
                Button(
                    onClick = onReactivate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🔑 تجديد", fontSize = 12.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════
// دالة حذف جميع البيانات
// ═══════════════════════════════════
private fun performDeleteAll(
    dao: com.example.schoolmanager.data.SchoolDao,
    snackbar: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    onDone: () -> Unit
) {
    scope.launch {
        try {
            dao.clearAllGrades()
            dao.clearAllTeachers()
            dao.clearAllSubjects()
            dao.clearAllStudents()
            dao.clearAllSections()
            dao.clearAllClasses()
            dao.clearSettings()

            // إعادة إنشاء الصفوف الـ 12
            val classNames = listOf(
                "الأول", "الثاني", "الثالث", "الرابع", "الخامس", "السادس",
                "السابع", "الثامن", "التاسع",
                "الأول الثانوي", "الثاني الثانوي", "الثالث الثانوي"
            )
            classNames.forEachIndexed { index, name ->
                dao.insertClass(
                    SchoolClass(
                        id = "class_${index + 1}",
                        name = name,
                        order = index + 1,
                        sectionIds = "",
                        homeroomTeachers = ""
                    )
                )
            }

            onDone()

            snackbar.showSnackbar(
                message = "✅ تم حذف جميع البيانات",
                duration = SnackbarDuration.Long
            )
        } catch (e: Exception) {
            snackbar.showSnackbar(
                message = "❌ فشل الحذف: ${e.message ?: "خطأ"}",
                duration = SnackbarDuration.Long
            )
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
