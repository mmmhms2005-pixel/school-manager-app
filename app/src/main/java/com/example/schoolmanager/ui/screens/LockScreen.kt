package com.example.schoolmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schoolmanager.data.SecurityManager

@Composable
fun LockScreen() {
    val ctx = LocalContext.current
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val pinLength = 4

    LaunchedEffect(entered) {
        if (entered.length == pinLength) {
            if (SecurityManager.verifyPin(ctx, entered)) {
                error = false
                SecurityManager.unlock()
            } else {
                error = true
            }
            entered = ""
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(50.dp))

            Text("🔒", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "أدخل الرقم السري",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "نظام المدرسة الذكي",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.height(40.dp))

            // Dots
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                repeat(pinLength) { i ->
                    val filled = i < entered.length
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (error) {
                Text(
                    "❌ الرقم السري غير صحيح",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Spacer(Modifier.height(20.dp))
            }

            Spacer(Modifier.weight(1f))

            // Number pad
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "⌫")
                ).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier.weight(1f).aspectRatio(1.7f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (key.isNotBlank()) {
                                    Button(
                                        onClick = {
                                            when (key) {
                                                "⌫" -> {
                                                    if (entered.isNotEmpty()) {
                                                        entered = entered.dropLast(1)
                                                        error = false
                                                    }
                                                }
                                                else -> {
                                                    if (entered.length < pinLength) {
                                                        entered += key
                                                        error = false
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        shape = MaterialTheme.shapes.large
                                    ) {
                                        Text(key, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
