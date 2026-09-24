package com.example.schoolmanager.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(nav: NavController, filePath: String) {
    val context = LocalContext.current
    val file = File(context.cacheDir, "reports/$filePath")
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // تحميل صفحات الـ PDF عند فتح الشاشة
    LaunchedEffect(filePath) {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)
            val bitmaps = mutableListOf<Bitmap>()
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }
            renderer.close()
            fileDescriptor.close()
            pages = bitmaps
        } catch (e: Exception) {
            errorMessage = e.message ?: "خطأ غير معروف"
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("معاينة الطباعة") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    // زر الطباعة في الأعلى
                    Button(
                        onClick = {
                            openPdfDirectly(context, file)
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = !isLoading && pages.isNotEmpty()
                    ) {
                        Text("طباعة")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                errorMessage != null -> {
                    Text("❌ خطأ: $errorMessage", color = MaterialTheme.colorScheme.error)
                }
                pages.isEmpty() -> {
                    Text("لا توجد صفحات لعرضها")
                }
                else -> {
                    var scale by remember { mutableStateOf(1f) }
var offset by remember { mutableStateOf(Offset.Zero) }

Box(
    modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(0.5f, 5f)
                offset += pan
            }
        },
    contentAlignment = Alignment.Center
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            ),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(pages) { bitmap ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "صفحة PDF",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
                    }
                }
            }
        }
    }
}
