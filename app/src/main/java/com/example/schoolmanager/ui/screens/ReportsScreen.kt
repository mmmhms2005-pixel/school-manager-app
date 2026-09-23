package com.example.schoolmanager.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.schoolmanager.SchoolApplication
import com.example.schoolmanager.data.GradeCalculator
import com.example.schoolmanager.data.PdfGenerator
import com.example.schoolmanager.data.Student
import com.example.schoolmanager.data.Grade
import com.example.schoolmanager.data.SchoolDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ★ دالة موحّدة لعرض PDF مباشرة في العارض.
 * تتجنب Chooser إن أمكن، وتفتح مباشرة.
 */
private fun savePdfToDownloads(context: android.content.Context, file: java.io.File) {
    try {
        val fileName = file.name
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destFile = java.io.File(downloadsDir, fileName)
            file.copyTo(destFile, overwrite = true)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
fun openPdfDirectly(context: android.content.Context, file: File) {
    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val viewIntent = Intent(Intent.ACTION_SEND).apply {
    type = "application/pdf"
    putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

// ★ حفظ الملف في مجلد التنزيلات ★
savePdfToDownloads(context, file)

// ★ فتح نافذة الطباعة أو المشاركة ★
context.startActivity(Intent.createChooser(viewIntent, "طباعة أو مشاركة الكشف"))
    
}

data class ReportTile(
    val id: String,
    val title: String,
    val emoji: String,
    val desc: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SchoolApplication
    val dao = app.database.dao()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val reports = listOf(
        ReportTile("annual_grades", "كشف درجات أعمال السنة", "📄", "لكل طالب في مادة وفترة"),
        ReportTile("student_report", "كشف الطالب الشامل", "👤", "كل المواد لطالب في فترة"),
        ReportTile("student_periods", "كشف الطالب عبر الفترات", "📅", "طالب × مادة × 8 فترات"),
        ReportTile("monthly_certificate", "شهادة شهرية", "📜", "طباعة الشهادات الشهرية"),
        ReportTile("class_report", "الكشف الشامل للصف", "👥", "كل الطلاب × كل الفترات"),
        ReportTile("final_results", "النتيجة النهائية والترتيب", "🏆", "النتيجة /100 + الأوائل"),
        ReportTile("analytics", "لوحة الإحصائيات", "📊", "تحليل درجات الصف")
    )

    var selectedReport by remember { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("الكشوفات والتقارير", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedReport != null) selectedReport = null
                        else nav.popBackStack()
                    }) {
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
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (selectedReport == null) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(reports, key = { it.id }) { report ->
                        ReportTileCard(report) { selectedReport = report.id }
                    }
                }
            } else {
                when (selectedReport) {
                    "annual_grades" -> AnnualGradesReportScreen(
                        dao = dao, app = app, scope = scope,
                        snackbar = snackbarHostState
                    )
                    "student_report" -> StudentFullReportScreen(
                        dao = dao, scope = scope,
                        snackbar = snackbarHostState
                    )
                    "student_periods" -> StudentPeriodsScreen(
                        dao = dao, scope = scope,
                        snackbar = snackbarHostState
                    )
                    "class_report" -> ClassReportScreen(
                        dao = dao, scope = scope,
                        snackbar = snackbarHostState
                    )
                    "final_results" -> FinalResultsScreen(
                        dao = dao, scope = scope,
                        snackbar = snackbarHostState
                    )
                    "analytics" -> AnalyticsScreen(
                        dao = dao, scope = scope,
                        snackbar = snackbarHostState
                    )
                    "monthly_certificate" -> MonthlyCertificateScreen(
    dao = dao,
    scope = scope,
    snackbar = snackbarHostState
)
                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🚧", fontSize = 56.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("قيد التطوير", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportTileCard(report: ReportTile, nav: NavController, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(140.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(report.emoji, fontSize = 38.sp)
            Spacer(Modifier.height(6.dp))
            Text(report.title, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(report.desc, fontSize = 10.sp, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnualGradesReportScreen(
    dao: SchoolDao,
    app: SchoolApplication,
    scope: CoroutineScope,
    snackbar: SnackbarHostState
) {
    val ctx = LocalContext.current
    val classes by dao.classes().collectAsState(initial = emptyList())
    val sections by dao.sections().collectAsState(initial = emptyList())
    val subjects by dao.subjects().collectAsState(initial = emptyList())
    val students by dao.students().collectAsState(initial = emptyList())
    val allGrades by dao.grades().collectAsState(initial = emptyList())
    val settings by dao.settings().collectAsState(initial = null)
    val allTeachers by dao.teachers().collectAsState(initial = emptyList())

    var classId by remember { mutableStateOf("") }
    var sectionId by remember { mutableStateOf("") }
    var subjectId by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(1) }

    var classExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }
    var periodExpanded by remember { mutableStateOf(false) }

    val availableSubjects = subjects.filter {
        it.classIds.isBlank() ||
        it.classIds.split(",").map { s -> s.trim() }.contains(classId)
    }.ifEmpty { subjects }

    val filteredStudents = students.filter { s ->
        (classId.isBlank() || s.classId == classId) &&
        (sectionId.isBlank() || s.sectionId == sectionId)
    }.sortedBy { it.number.toIntOrNull() ?: 0 }

    val isExam = GradeCalculator.isExam(period)
    val canGenerate = classId.isNotBlank() && subjectId.isNotBlank() && filteredStudents.isNotEmpty()

    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.weight(1f)) {
                ExposedDropdownMenuBox(classExpanded, { classExpanded = !classExpanded }) {
                    OutlinedTextField(
                        value = classes.find { it.id == classId }?.name ?: "الصف",
                        onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(classExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        singleLine = true
                    )
                    ExposedDropdownMenu(classExpanded, { classExpanded = false }) {
                        classes.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = { classId = c.id; sectionId = ""; subjectId = ""; classExpanded = false }
                            )
                        }
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                ExposedDropdownMenuBox(sectionExpanded, { sectionExpanded = !sectionExpanded }) {
                    OutlinedTextField(
                        value = sections.find { it.id == sectionId }?.name ?: "الشعبة",
                        onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sectionExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        singleLine = true
                    )
                    ExposedDropdownMenu(sectionExpanded, { sectionExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("الكل") },
                            onClick = { sectionId = ""; sectionExpanded = false }
                        )
                        sections.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = { sectionId = s.id; sectionExpanded = false }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.weight(1f)) {
                ExposedDropdownMenuBox(subjectExpanded, { subjectExpanded = !subjectExpanded }) {
                    OutlinedTextField(
                        value = subjects.find { it.id == subjectId }?.name ?: "المادة",
                        onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(subjectExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        singleLine = true
                    )
                    ExposedDropdownMenu(subjectExpanded, { subjectExpanded = false }) {
                        availableSubjects.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = { subjectId = s.id; subjectExpanded = false }
                            )
                        }
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                ExposedDropdownMenuBox(periodExpanded, { periodExpanded = !periodExpanded }) {
                    OutlinedTextField(
                        value = GradeCalculator.periodName(period),
                        onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(periodExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        singleLine = true
                    )
                    ExposedDropdownMenu(periodExpanded, { periodExpanded = false }) {
                        GradeCalculator.PERIODS.forEach { (p, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { period = p; periodExpanded = false }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            enabled = canGenerate,
            onClick = {
                scope.launch {
                    try {
                        val rows = buildAnnualGradesRows(filteredStudents, allGrades, subjectId, period)
                        val cols: List<PdfGenerator.Column>
                        val redCols: Set<Int>
                        if (isExam) {
                            cols = listOf(
                                PdfGenerator.Column("#", 0.06f),
                                PdfGenerator.Column("رقم الطالب", 0.14f),
                                PdfGenerator.Column("اسم الطالب", 0.40f),
                                PdfGenerator.Column("تحريري", 0.20f),
                                PdfGenerator.Column("المجموع", 0.20f)
                            )
                            redCols = setOf(4)
                        } else {
                            cols = listOf(
                                PdfGenerator.Column("#", 0.05f),
                                PdfGenerator.Column("رقم الطالب", 0.11f),
                                PdfGenerator.Column("اسم الطالب", 0.28f),
                                PdfGenerator.Column("واجب", 0.10f),
                                PdfGenerator.Column("شفهي", 0.10f),
                                PdfGenerator.Column("غياب", 0.07f),
                                PdfGenerator.Column("مواظبة", 0.08f),
                                PdfGenerator.Column("تحريري", 0.10f),
                                PdfGenerator.Column("المجموع", 0.11f)
                            )
                            redCols = setOf(8)
                        }

                        val selectedTeacher = allTeachers.firstOrNull { t ->
                            t.assignments.split(",")
                                .map { it.trim() }
                                .contains("$subjectId:$classId")
                        }

                        val schoolInfo = PdfGenerator.SchoolInfo(
                            schoolName = settings?.schoolName ?: "",
                            academicYear = settings?.academicYear ?: "",
                            principalName = settings?.principalName ?: "",
                            teacherName = selectedTeacher?.name ?: "",
                            logoBase64 = settings?.logoBase64 ?: ""
                        )

                        val reportData = PdfGenerator.ReportData(
                            title = "كشف درجات ${GradeCalculator.periodName(period)}",
                            meta = buildString {
                                append("الصف: ${classes.find { it.id == classId }?.name ?: ""}")
                                append("  |  الشعبة: ${sections.find { it.id == sectionId }?.name ?: "الكل"}")
                                append("  |  المادة: ${subjects.find { it.id == subjectId }?.name ?: ""}")
                            },
                            columns = cols,
                            rows = rows,
                            isLandscape = false,
                            redColumnIndices = redCols
                        )

                        val file = withContext(Dispatchers.IO) {
                            PdfGenerator.generate(
                                context = ctx,
                                reports = listOf(reportData),
                                school = schoolInfo,
                                fileName = "annual_grades_${System.currentTimeMillis()}.pdf"
                            )
                        }

                        // ★ فتح PDF مباشرة
                        nav.navigate("pdf_preview/${file.absolutePath}")

                        snackbar.showSnackbar(
                            message = "✅ تم توليد PDF بنجاح",
                            duration = SnackbarDuration.Short
                        )
                    } catch (e: Exception) {
                        snackbar.showSnackbar(
                            message = "❌ فشل التوليد: ${e.message ?: "خطأ"}",
                            duration = SnackbarDuration.Long
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Description, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("طباعة", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))

        if (canGenerate) {
            Card(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(8.dp)) {
                    Text(
                        "معاينة (${filteredStudents.size} طالب)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Divider()
                    Spacer(Modifier.height(6.dp))
                    filteredStudents.forEachIndexed { idx, student ->
                        val g = allGrades.find {
                            it.studentId == student.id &&
                            it.subjectId == subjectId &&
                            it.period == period
                        }
                        val computed = GradeCalculator.compute(
                            period = period,
                            homework = g?.homework ?: 0,
                            oral = g?.oral ?: 0,
                            absence = g?.absence ?: 0,
                            written = g?.written ?: 0
                        )
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${idx + 1}", Modifier.width(24.dp), fontSize = 11.sp)
                            Text(student.name, Modifier.weight(1f), fontSize = 12.sp)
                            Text("المجموع: ${computed.total}", fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error)
                        }
                        Divider()
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("اختر الصف والمادة لعرض المعاينة", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

private fun buildAnnualGradesRows(
    students: List<Student>,
    allGrades: List<Grade>,
    subjectId: String,
    period: Int
): List<List<String>> {
    val isExam = GradeCalculator.isExam(period)
    return students.mapIndexed { idx, student ->
        val g = allGrades.find {
            it.studentId == student.id &&
            it.subjectId == subjectId &&
            it.period == period
        }
        val computed = GradeCalculator.compute(
            period = period,
            homework = g?.homework ?: 0,
            oral = g?.oral ?: 0,
            absence = g?.absence ?: 0,
            written = g?.written ?: 0
        )
        if (isExam) {
            listOf(
                "${idx + 1}",
                student.number,
                student.name,
                "${computed.written}",
                "${computed.total}"
            )
        } else {
            listOf(
                "${idx + 1}",
                student.number,
                student.name,
                "${computed.homework}",
                "${computed.oral}",
                "${computed.absence}",
                "${computed.attendance}",
                "${computed.written}",
                "${computed.total}"
            )
        }
    }
}
