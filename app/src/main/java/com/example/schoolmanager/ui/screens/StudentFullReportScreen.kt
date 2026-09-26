package com.example.schoolmanager.ui.screens

import android.content.Intent
import androidx.navigation.NavController
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.schoolmanager.SchoolApplication
import com.example.schoolmanager.data.GradeCalculator
import com.example.schoolmanager.data.HomeroomTeacherHelper
import com.example.schoolmanager.data.PdfGenerator
import com.example.schoolmanager.data.SchoolDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFullReportScreen(
    nav: NavController,
    dao: SchoolDao,
    scope: CoroutineScope,
    snackbar: SnackbarHostState
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SchoolApplication

    val classes by dao.classes().collectAsState(initial = emptyList())
    val sections by dao.sections().collectAsState(initial = emptyList())
    val subjects by dao.subjects().collectAsState(initial = emptyList())
    val students by dao.students().collectAsState(initial = emptyList())
    val allGrades by dao.grades().collectAsState(initial = emptyList())
    val settings by dao.settings().collectAsState(initial = null)
    val allTeachers by dao.teachers().collectAsState(initial = emptyList())

    var classId by remember { mutableStateOf("") }
    var sectionId by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(1) }

    var classExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }
    var studentExpanded by remember { mutableStateOf(false) }
    var periodExpanded by remember { mutableStateOf(false) }

    val availableSections = remember(classId, classes, sections) {
        val cls = classes.find { it.id == classId }
        val ids = cls?.sectionIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        sections.filter { it.id in ids }
    }

    val availableStudents = students.filter { s ->
        (classId.isBlank() || s.classId == classId) &&
        (sectionId.isBlank() || s.sectionId == sectionId)
    }.sortedBy { it.number.toIntOrNull() ?: 0 }

    val selectedStudent = students.find { it.id == studentId }
    val isExam = GradeCalculator.isExam(period)

    val availableSubjects = subjects.filter { subj ->
        classId.isBlank() ||
        subj.classIds.isBlank() ||
        subj.classIds.split(",").map { it.trim() }.contains(classId)
    }

    val canGenerate = classId.isNotBlank() && studentId.isNotBlank() && availableSubjects.isNotEmpty()

    Column(Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState())) {

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
                                onClick = { classId = c.id; sectionId = ""; studentId = ""; classExpanded = false }
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
                            onClick = { sectionId = ""; studentId = ""; sectionExpanded = false }
                        )
                        availableSections.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = { sectionId = s.id; studentId = ""; sectionExpanded = false }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.weight(1f)) {
                ExposedDropdownMenuBox(studentExpanded, { studentExpanded = !studentExpanded }) {
                    OutlinedTextField(
                        value = selectedStudent?.name ?: "الطالب",
                        onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(studentExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        singleLine = true
                    )
                    ExposedDropdownMenu(studentExpanded, { studentExpanded = false }) {
                        if (availableStudents.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("اختر الصف أولاً") },
                                onClick = { studentExpanded = false }
                            )
                        }
                        availableStudents.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.number} - ${s.name}") },
                                onClick = { studentId = s.id; studentExpanded = false }
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

        Spacer(Modifier.height(12.dp))

        Button(
            enabled = canGenerate,
            onClick = {
                scope.launch {
                    try {
                        val rows = buildStudentFullRows(
                            selectedStudent!!, availableSubjects, allGrades, period
                        )
                        val cols: List<PdfGenerator.Column>
                        val redCols: Set<Int>
                        if (isExam) {
                            cols = listOf(
                                PdfGenerator.Column("#", 0.06f),
                                PdfGenerator.Column("المادة", 0.34f),
                                PdfGenerator.Column("تحريري", 0.20f),
                                PdfGenerator.Column("المجموع", 0.20f),
                                PdfGenerator.Column("التقدير", 0.20f)
                            )
                            redCols = setOf(3)
                        } else {
                            cols = listOf(
                                PdfGenerator.Column("#", 0.05f),
                                PdfGenerator.Column("المادة", 0.22f),
                                PdfGenerator.Column("واجب", 0.10f),
                                PdfGenerator.Column("شفهي", 0.10f),
                                PdfGenerator.Column("غياب", 0.08f),
                                PdfGenerator.Column("مواظبة", 0.10f),
                                PdfGenerator.Column("تحريري", 0.10f),
                                PdfGenerator.Column("المجموع", 0.10f),
                                PdfGenerator.Column("التقدير", 0.15f)
                            )
                            redCols = setOf(7)
                        }

                        val homeroomName = HomeroomTeacherHelper.getName(
                            classId = classId,
                            sectionId = sectionId,
                            classes = classes,
                            teachers = allTeachers
                        )

                        val schoolInfo = PdfGenerator.SchoolInfo(
                            schoolName = settings?.schoolName ?: "",
                            academicYear = settings?.academicYear ?: "",
                            principalName = settings?.principalName ?: "",
                            homeroomTeacherName = homeroomName,
                            logoBase64 = settings?.logoBase64 ?: ""
                        )

                        val reportData = PdfGenerator.ReportData(
                            title = "كشف الطالب — ${GradeCalculator.periodName(period)}",
                            meta = buildString {
                                append("الصف: ${classes.find { it.id == classId }?.name ?: ""}")
                                append("  |  الشعبة: ${sections.find { it.id == sectionId }?.name ?: "الكل"}")
                                append("  |  الطالب: ${selectedStudent.name}")
                                append("  |  الرقم: ${selectedStudent.number}")
                            },
                            columns = cols,
                            rows = rows,
                            isLandscape = false,
                            redColumnIndices = redCols,
                            isMultiSubject = true,
                            photoBase64 = selectedStudent?.photoBase64 ?: "",
                        )

                        val file = withContext(Dispatchers.IO) {
                            PdfGenerator.generate(
                                context = ctx,
                                reports = listOf(reportData),
                                school = schoolInfo,
                                fileName = "student_full_${System.currentTimeMillis()}.pdf"
                            )
                        }

                        // ★ فتح PDF مباشرة
                        nav.navigate("pdf_preview/${file.name}")

                        snackbar.showSnackbar(
                            message = "✅ تم توليد كشف الطالب",
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
            Text("طباعة PDF", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        if (canGenerate && selectedStudent != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "معاينة — ${selectedStudent.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(8.dp))

                    availableSubjects.forEach { subj ->
                        val g = allGrades.find {
                            it.studentId == studentId &&
                            it.subjectId == subj.id &&
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
                            Text(subj.name, Modifier.weight(1f), fontSize = 12.sp)
                            Text("المجموع: ${computed.total}", fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error)
                        }
                        Divider()
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("اختر الصف والطالب لعرض المعاينة",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

private fun buildStudentFullRows(
    student: com.example.schoolmanager.data.Student,
    subjects: List<com.example.schoolmanager.data.Subject>,
    allGrades: List<com.example.schoolmanager.data.Grade>,
    period: Int
): List<List<String>> {
    val isExam = GradeCalculator.isExam(period)
    return subjects.mapIndexed { idx, subj ->
        val g = allGrades.find {
            it.studentId == student.id &&
            it.subjectId == subj.id &&
            it.period == period
        }
        val computed = GradeCalculator.compute(
            period = period,
            homework = g?.homework ?: 0,
            oral = g?.oral ?: 0,
            absence = g?.absence ?: 0,
            written = g?.written ?: 0
        )
        val rating = ratingFor(computed.total, isExam)
        if (isExam) {
            listOf(
                "${idx + 1}",
                subj.name,
                "${computed.written}",
                "${computed.total}",
                rating
            )
        } else {
            listOf(
                "${idx + 1}",
                subj.name,
                "${computed.homework}",
                "${computed.oral}",
                "${computed.absence}",
                "${computed.attendance}",
                "${computed.written}",
                "${computed.total}",
                rating
            )
        }
    }
}

private fun ratingFor(score: Int, isExam: Boolean): String {
    val max = if (isExam) 30 else 20
    val percent = (score * 100) / max
    return when {
        percent >= 90 -> "ممتاز"
        percent >= 80 -> "جيد جداً"
        percent >= 70 -> "جيد"
        percent >= 60 -> "مقبول"
        percent >= 50 -> "ضعيف"
        else -> "راسب"
    }
}
