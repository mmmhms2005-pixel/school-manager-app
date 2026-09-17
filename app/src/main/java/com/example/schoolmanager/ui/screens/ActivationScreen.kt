package com.example.schoolmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schoolmanager.data.DeviceIdManager
import com.example.schoolmanager.data.LicenseManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivationScreen(onActivated: () -> Unit) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val deviceId = remember { DeviceIdManager.getDeviceId(ctx) }

    var licenseKey by remember { mutableStateOf("") }
    var isActivating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(20.dp))

            // ═══ الأيقونة ═══
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.size(90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "تفعيل التطبيق",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "لتشغيل التطبيق، أرسل كود الجهاز للبائع\nواستلم كود التفعيل منه.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(28.dp))

            // ═══ كود الجهاز ═══
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "📱 كود جهازك",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Surface(
                        Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            deviceId,
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(deviceId))
                            // إشعار النسخ
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("📋 نسخ الكود")
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "أرسل هذا الكود إلى البائع عبر واتساب أو أي وسيلة تواصل.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ═══ حقل كود التفعيل ═══
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "🔑 كود التفعيل",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = licenseKey,
                        onValueChange = {
                            licenseKey = it.uppercase().filter { c ->
                                c.isLetterOrDigit() || c == '-'
                            }
                            errorMessage = ""
                        },
                        label = { Text("أدخل الكود هنا") },
                        placeholder = { Text("SM26-XXXX-XXXX-XXXX-XXXX") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = errorMessage.isNotBlank(),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii
                        )
                    )

                    if (errorMessage.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                "❌ $errorMessage",
                                Modifier.padding(10.dp),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ═══ زر التفعيل ═══
            Button(
                enabled = licenseKey.trim().length >= 10 && !isActivating,
                onClick = {
                    isActivating = true
                    errorMessage = ""

                    val result = LicenseManager.verifyAndActivate(ctx, licenseKey)

                    when (result) {
                        is LicenseManager.ActivationResult.Success -> {
                            // نجح التفعيل
                            onActivated()
                        }
                        is LicenseManager.ActivationResult.Failure -> {
                            errorMessage = result.reason
                        }
                    }
                    isActivating = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isActivating) {
                    CircularProgressIndicator(
                        Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "🔓 تفعيل التطبيق",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            // ═══ تلميح ═══
            Surface(
                Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "💡 ملاحظة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "• كود التفعيل مرتبط بهذا الجهاز فقط.\n" +
                        "• لا يمكن استخدامه على جهاز آخر.\n" +
                        "• إذا فقدت الكود، تواصل مع البائع.",
                        fontSize = 11.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
