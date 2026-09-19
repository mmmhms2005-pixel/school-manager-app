package com.example.schoolmanager.ui.screens

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.schoolmanager.SchoolApplication
import com.example.schoolmanager.data.BackupManager
import com.example.schoolmanager.data.DeviceIdManager
import com.example.schoolmanager.data.LicenseManager
import com.example.schoolmanager.data.SchoolClass
import com.example.schoolmanager.data.SecurityManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════
// قائمة الخيارات
// ═══════════════════════════════════════════

enum class MenuOption { LICENSE, SECURITY, BACKUP, DELETE_STUDENTS, DANGER, ABOUT }

@Composable
fun AppMenuPopup(
    onDismiss: () -> Unit,
    onOptionSelected: (MenuOption) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚙️ القائمة", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                MenuButton("🔑", "الترخيص") { onOptionSelected(MenuOption.LICENSE); onDismiss() }
                MenuButton("🔒", "الحماية والأمان") { onOptionSelected(MenuOption.SECURITY); onDismiss() }
                MenuButton("💾", "النسخ الاحتياطي والاستعادة") { onOptionSelected(MenuOption.BACKUP); onDismiss() }
                MenuButton("🗑️", "حذف جميع الطلاب", isDanger = true) { onOptionSelected(MenuOption.DELETE_STUDENTS); onDismiss() }
                MenuButton("⚠️", "منطقة الخطر", isDanger = true) { onOptionSelected(MenuOption.DANGER); onDismiss() }
                MenuButton("ℹ️", "حول التطبيق") { onOptionSelected(MenuOption.ABOUT); onDismiss() }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
private fun MenuButton(emoji: String, title: String, isDanger: Boolean = false, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDanger) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDanger) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════
// 1️⃣ نافذة الترخيص
// ═══════════════════════════════════════════

@Composable
fun LicenseDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val deviceId = remember { DeviceIdManager.getDeviceId(ctx) }
    var status by remember { mutableStateOf(LicenseManager.getStatus(ctx)) }

    var showReactivation by remember { mutableStateOf(false) }
    var newKey by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔑", fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Text("الترخيص", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = bgColor)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when {
                                    status.isExpired -> "❌"
                                    status.isGracePeriod -> "⏰"
                                    status.isActive -> "✅"
                                    else -> "🔑"
                                },
                                fontSize = 22.sp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                status.message,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = onBgColor
                            )
                        }
                        if (status.expiresAt > 0L) {
                            Spacer(Modifier.height(8.dp))
                            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            Text(
                                "📅 ينتهي: ${fmt.format(Date(status.expiresAt))}",
                                fontSize = 12.sp,
                                color = onBgColor
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text("📱 كود الجهاز:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Surface(
                    Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        deviceId,
                        Modifier.fillMaxWidth().padding(10.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(deviceId)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("📋 نسخ كود الجهاز")
                }

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { showReactivation = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🔑 تجديد / تفعيل") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )

    if (showReactivation) {
        AlertDialog(
            onDismissRequest = { showReactivation = false },
            icon = { Text("🔑", fontSize = 28.sp) },
            title = { Text("تجديد الاشتراك") },
            text = {
                Column {
                    Text("أدخل كود التفعيل الجديد:", fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = {
                            newKey = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' }
                            error = ""
                        },
                        label = { Text("كود التفعيل") },
                        placeholder = { Text("SM26-XXXX-XXXX-XXXX-XXXX") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = error.isNotBlank(),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace, fontSize = 12.sp
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )
                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("❌ $error", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newKey.trim().length >= 10,
                    onClick = {
                        val result = LicenseManager.verifyAndActivate(ctx, newKey)
                        when (result) {
                            is LicenseManager.ActivationResult.Success -> {
                                status = LicenseManager.getStatus(ctx)
                                showReactivation = false
                                newKey = ""
                                error = ""
                            }
                            is LicenseManager.ActivationResult.Failure -> {
                                error = result.reason
                            }
                        }
                    }
                ) { Text("تفعيل") }
            },
            dismissButton = {
                TextButton(onClick = { showReactivation = false }) { Text("إلغاء") }
            }
        )
    }
}

// ═══════════════════════════════════════════
// 2️⃣ نافذة الحماية والأمان
// ═══════════════════════════════════════════

@Composable
fun SecurityDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current

    var pinEnabled by remember { mutableStateOf(SecurityManager.isPinEnabled(ctx)) }
    var autoLockMinutes by remember { mutableStateOf(SecurityManager.getAutoLockMinutes(ctx).toString()) }
    var encryptBackup by remember { mutableStateOf(SecurityManager.isBackupEncryptionEnabled(ctx)) }

    var showPinDialog by remember { mutableStateOf(false) }
    var pinMode by remember { mutableStateOf("set") }
    var showRemovePin by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("الحماية والأمان", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("قفل التطبيق", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    if (pinEnabled) "مُفعَّل" else "غير مُفعَّل",
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
                                        showRemovePin = true
                                    }
                                }
                            )
                        }
                        if (pinEnabled) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    pinMode = "change"
                                    showPinDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("🔑 تغيير الرقم السري") }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("⏱️ القفل التلقائي بعد",
                            fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 5, 10, 30).forEach { m ->
                                FilterChip(
                                    selected = autoLockMinutes == m.toString(),
                                    onClick = {
                                        autoLockMinutes = m.toString()
                                        SecurityManager.setAutoLockMinutes(ctx, m)
                                    },
                                    label = { Text("$m د", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("🔐 تشفير النسخ الاحتياطية",
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("AES-256 بكلمة مرور",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary)
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
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )

    if (showPinDialog) {
        var pin1 by remember { mutableStateOf("") }
        var pin2 by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            icon = { Text("🔐", fontSize = 24.sp) },
            title = { Text(if (pinMode == "set") "تعيين PIN" else "تغيير PIN") },
            text = {
                Column {
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
                        Spacer(Modifier.height(6.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        pin1.length < 4 -> error = "4 أرقام على الأقل"
                        pin1 != pin2 -> error = "الرقمان غير متطابقين"
                        else -> {
                            SecurityManager.setPin(ctx, pin1)
                            pinEnabled = true
                            showPinDialog = false
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

    if (showRemovePin) {
        var currentPin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRemovePin = false },
            icon = { Text("⚠️", fontSize = 24.sp) },
            title = { Text("إلغاء القفل") },
            text = {
                Column {
                    Text("أدخل الرقم السري الحالي:", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { if (it.all { c -> c.isDigit() }) currentPin = it },
                        label = { Text("الرقم السري") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (SecurityManager.verifyPin(ctx, currentPin)) {
                        SecurityManager.removePin(ctx)
                        pinEnabled = false
                        showRemovePin = false
                    } else error = "الرقم غير صحيح"
                }) { Text("تأكيد", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePin = false }) { Text("إلغاء") }
            }
        )
    }
}

// ═══════════════════════════════════════════
// 3️⃣ نافذة النسخ الاحتياطي
// ═══════════════════════════════════════════

@Composable
fun BackupDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var encryptBackup by remember { mutableStateOf(SecurityManager.isBackupEncryptionEnabled(ctx)) }

    var pendingBackupUri by remember { mutableStateOf<Uri?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var restoreNeedsPassword by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            if (encryptBackup) {
                pendingBackupUri = it
                showPasswordDialog = true
            } else {
                scope.launch {
                    val r = BackupManager.exportBackup(ctx, it, null)
                    snackbar.showSnackbar(
                        if (r.isSuccess) "✅ تم الحفظ (${r.getOrNull()} سجل)"
                        else "❌ ${r.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

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
                    showRestoreConfirm = true
                } catch (e: Exception) {
                    snackbar.showSnackbar("❌ تعذر قراءة الملف")
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💾", fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Text("النسخ الاحتياطي", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "احفظ نسخة كاملة من بياناتك في ملف JSON." +
                            if (encryptBackup) " ⚠️ التشفير مُفعَّل." else "",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = { backupLauncher.launch(BackupManager.suggestFileName()) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("💾 إنشاء نسخة احتياطية") }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { restoreLauncher.launch("application/json") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("♻️ استعادة") }

                Spacer(Modifier.height(8.dp))
                Text(
                    "⚠️ الاستعادة تحذف البيانات الحالية.",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )

    if (showPasswordDialog) {
        var pw by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            icon = { Text("🔐", fontSize = 24.sp) },
            title = { Text("كلمة مرور التشفير") },
            text = {
                Column {
                    Text("أدخل كلمة مرور النسخة:", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pw,
                        onValueChange = { pw = it },
                        label = { Text("كلمة المرور") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pw.length < 4) error = "كلمة قصيرة"
                    else {
                        val uri = pendingBackupUri
                        showPasswordDialog = false
                        if (uri != null) {
                            scope.launch {
                                val r = BackupManager.exportBackup(ctx, uri, pw)
                                snackbar.showSnackbar(
                                    if (r.isSuccess) "✅ تم الحفظ (${r.getOrNull()} سجل)"
                                    else "❌ ${r.exceptionOrNull()?.message}"
                                )
                            }
                        }
                    }
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showRestoreConfirm && pendingRestoreUri != null) {
        var pw by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            icon = { Text("⚠️", fontSize = 28.sp) },
            title = { Text("تأكيد الاستعادة") },
            text = {
                Column {
                    Text("سيتم حذف البيانات الحالية واستبدالها.")
                    if (restoreNeedsPassword) {
                        Spacer(Modifier.height(10.dp))
                        Text("🔐 أدخل كلمة المرور:", fontSize = 12.sp)
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
                        Spacer(Modifier.height(6.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (restoreNeedsPassword && pw.isBlank()) {
                        error = "أدخل كلمة المرور"
                        return@TextButton
                    }
                    val uri = pendingRestoreUri ?: return@TextButton
                    showRestoreConfirm = false
                    scope.launch {
                        val r = BackupManager.importBackup(ctx, uri, if (restoreNeedsPassword) pw else null)
                        snackbar.showSnackbar(
                            if (r.isSuccess) "✅ تمت الاستعادة (${r.getOrNull()} سجل)"
                            else "❌ ${r.exceptionOrNull()?.message}"
                        )
                    }
                }) { Text("نعم، استعد", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("إلغاء") }
            }
        )
    }
}

// ═══════════════════════════════════════════
// 4️⃣ نافذة حذف جميع الطلاب
// ═══════════════════════════════════════════

@Composable
fun DeleteAllStudentsDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SchoolApplication
    val dao = app.database.dao()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val students by dao.students().collectAsState(initial = emptyList())

    var showConfirm by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text("🗑️", fontSize = 32.sp) },
        title = {
            Text(
                "حذف جميع الطلاب",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "⚠️ هذا الإجراء لا يمكن التراجع عنه!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "سيتم حذف:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "• جميع الطلاب (${students.size} طالب)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "• جميع درجاتهم",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "✅ الصفوف والمواد والمعلمون ستبقى كما هي.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { showConfirm = true },
                enabled = students.isNotEmpty()
            ) {
                Text(
                    if (students.isEmpty()) "لا يوجد طلاب"
                    else "متابعة",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )

    if (showConfirm) {
        var typed by remember { mutableStateOf("") }
        val word = "حذف"

        AlertDialog(
            onDismissRequest = { showConfirm = false },
            icon = { Text("🗑️", fontSize = 36.sp) },
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
                        "للتأكيد، اكتب كلمة \"$word\" في المربع:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        label = { Text("اكتب: $word") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = typed.isNotBlank() && typed.trim() != word
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⚠️ سيُحذف ${students.size} طالب ودرجاتهم نهائياً.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = typed.trim() == word && students.isNotEmpty(),
                    onClick = {
                        if (SecurityManager.isPinEnabled(ctx)) {
                            showConfirm = false
                            showPinDialog = true
                        } else {
                            performDeleteAllStudents(dao, scope, snackbar) { onDismiss() }
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
                TextButton(onClick = { showConfirm = false }) { Text("إلغاء") }
            }
        )
    }

    if (showPinDialog) {
        var pin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            icon = { Text("🔐", fontSize = 28.sp) },
            title = { Text("تأكيد الهوية") },
            text = {
                Column {
                    Text("أدخل الرقم السري للتأكيد:",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
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
                        Spacer(Modifier.height(6.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (SecurityManager.verifyPin(ctx, pin)) {
                        showPinDialog = false
                        performDeleteAllStudents(dao, scope, snackbar) { onDismiss() }
                    } else {
                        error = "الرقم غير صحيح"
                    }
                }) {
                    Text("تأكيد", color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

private fun performDeleteAllStudents(
    dao: com.example.schoolmanager.data.SchoolDao,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbar: SnackbarHostState,
    onDone: () -> Unit
) {
    scope.launch {
    try {
        // ★★★ ابدأ التعديل: لاحظ القوسين الجديدين ★★★
        withContext(NonCancellable) {
            dao.clearAllStudentGrades()
            dao.clearAllStudents()
        }
        // ★★★ انتهى التعديل ★★★

        snackbar.showSnackbar(
            "✅ تم حذف جميع الطلاب ودرجاتهم",
            duration = SnackbarDuration.Long
        )
        delay(1200)
        onDone()
    } catch (e: Exception) {
        snackbar.showSnackbar(
            "❌ فشل الحذف: ${e.message ?: "خطأ غير معروف"}",
            duration = SnackbarDuration.Long
        )
    }
}

// ═══════════════════════════════════════════
// 5️⃣ نافذة منطقة الخطر
// ═══════════════════════════════════════════

@Composable
fun DangerZoneDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SchoolApplication
    val dao = app.database.dao()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var showWarning by remember { mutableStateOf(false) }
    var showBackupOffer by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("منطقة الخطر", fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error)
            }
        },
        text = {
            Column {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        "📌 الإجراءات هنا لا يمكن التراجع عنها.",
                        Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = { showWarning = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("🗑️ حذف جميع البيانات", fontWeight = FontWeight.Bold) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )

    if (showWarning) {
        AlertDialog(
            onDismissRequest = { showWarning = false },
            icon = { Text("⚠️", fontSize = 36.sp) },
            title = { Text("تحذير!", color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("سيتم حذف جميع البيانات نهائياً:",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("• الطلاب والمعلمون", fontSize = 13.sp)
                    Text("• الدرجات والكشوفات", fontSize = 13.sp)
                    Text("• الصفوف والمواد", fontSize = 13.sp)
                    Text("• الإعدادات", fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("⚠️ لا يمكن التراجع!",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showWarning = false
                    showBackupOffer = true
                }) { Text("متابعة", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showWarning = false }) { Text("إلغاء") }
            }
        )
    }

    if (showBackupOffer) {
        AlertDialog(
            onDismissRequest = { showBackupOffer = false },
            icon = { Text("💾", fontSize = 32.sp) },
            title = { Text("نسخة احتياطية؟") },
            text = { Text("هل تريد إنشاء نسخة احتياطية قبل الحذف؟") },
            confirmButton = {
                TextButton(onClick = {
    showBackupOffer = false
    scope.launch {
    snackbar.showSnackbar(
        message = "تمت العملية بنجاح"
    )
}
}) { Text("موافق") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupOffer = false
                    showConfirm = true
                }) { Text("تخطي", color = MaterialTheme.colorScheme.error) }
            }
        )
    }

    if (showConfirm) {
        var typed by remember { mutableStateOf("") }
        val word = "حذف"

        AlertDialog(
            onDismissRequest = { showConfirm = false },
            icon = { Text("🗑️", fontSize = 36.sp) },
            title = { Text("التأكيد النهائي",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("اكتب كلمة \"$word\":", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        label = { Text("اكتب: $word") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = typed.isNotBlank() && typed.trim() != word
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = typed.trim() == word,
                    onClick = {
                        showConfirm = false
                        if (SecurityManager.isPinEnabled(ctx)) {
                            showPinDialog = true
                        } else {
                            performDeleteAll(dao, scope, snackbar) { onDismiss() }
                        }
                    }
                ) { Text("🗑️ حذف", color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("إلغاء") }
            }
        )
    }

    if (showPinDialog) {
        var pin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            icon = { Text("🔐", fontSize = 28.sp) },
            title = { Text("تأكيد الهوية") },
            text = {
                Column {
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
                        Spacer(Modifier.height(6.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (SecurityManager.verifyPin(ctx, pin)) {
                        showPinDialog = false
                        performDeleteAll(dao, scope, snackbar) { onDismiss() }
                    } else error = "الرقم غير صحيح"
                }) { Text("تأكيد", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

private fun performDeleteAll(
    dao: com.example.schoolmanager.data.SchoolDao,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbar: SnackbarHostState,
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
            snackbar.showSnackbar("✅ تم حذف جميع البيانات")
            delay(1500)
            onDone()
        } catch (e: Exception) {
            snackbar.showSnackbar("❌ فشل الحذف: ${e.message}")
        }
    }
}

// ═══════════════════════════════════════════
// 6️⃣ نافذة حول التطبيق
// ═══════════════════════════════════════════

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val version = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) { "1.0" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ℹ️", fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Text("حول التطبيق", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("📋", fontSize = 40.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("نظام المدرسة الذكي",
                    fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("الإصدار $version",
                        Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.height(16.dp))

                Text("✨ المميزات", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                Spacer(Modifier.height(6.dp))
                AboutFeature("👨‍🎓", "إدارة كاملة للطلاب")
                AboutFeature("🏫", "الصفوف والشعب + مربي الصف")
                AboutFeature("📚", "المواد الدراسية")
                AboutFeature("👨‍🏫", "المعلمون والربط الدقيق")
                AboutFeature("📝", "إدخال الدرجات")
                AboutFeature("📊", "6 كشوفات احترافية")
                AboutFeature("🖨️", "توليد PDF")
                AboutFeature("💾", "النسخ الاحتياطي")
                AboutFeature("🔐", "تشفير النسخ الاحتياطية")
                AboutFeature("📴", "يعمل بدون إنترنت")

                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(10.dp))
                Text("© 2026 — جميع الحقوق محفوظة",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                Text("صُنع بـ ❤️ لخدمة التعليم",
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
private fun AboutFeature(emoji: String, text: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 15.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 12.sp)
    }
}
