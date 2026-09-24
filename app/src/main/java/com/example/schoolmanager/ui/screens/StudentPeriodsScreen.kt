package com.example.schoolmanager.ui.screens

import android.content.Intent
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
import com.example.schoolmanager.data.PdfGenerator
import com.example.schoolmanager.data.SchoolDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentPeriodsScreen(
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
    var subjectId by remember { mutableStateOf("") }

    var classExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }
    var studentExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }

    val availableSections = remember(classId, classes, sections) {
        val cls = classes.find { it.id == classId }
        val ids = cls?.sectionIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        sections.filter { it.id in ids }
    }

    val availableStudents = students.filter { s ->
        (classId.isBlank() || s.classId == classId) &&
        (sectionId.isBlank() || s.sectionId == sectionId)
    }.sortedBy { it.number.toIntOrNull() ?: 0 }

    val availableSubjects = subjects.filter { subj ->
        classId.isBlank() ||
        subj.classIds.isBlank() ||
        subj.classIds.split(",").map { it.trim() }.contains(classId)
    }

    val selectedStudent = students.find { it.id == studentId }
    val selectedSubject = subjects.find { it.id == subjectId }

    val canGenerate = classId.isNotBlank() && studentId.isNotBlank() && subjectId.isNotBlank()

    val results = remember(studentId, subjectId, allGrades) {
        if (studentId.isBlank() || subjectId.isBlank()) emptyList()
        else buildPeriodResults(studentId, subjectId, allGrades)
    }

    Column(
        Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState())
    ) {
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
                                onClick = { classId = c.id; sectionId = ""; studentId = ""; subjectId = ""; classExpanded = false }
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
                ExposedDropdownMenuBox(subjectExpanded, { subjectExpanded = !subjectExpanded }) {
                    OutlinedTextField(
                        value = selectedSubject?.name ?: "المادة",
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
        }

        Spacer(Modifier.height(12.dp))

        Button(
            enabled = canGenerate,
            onClick = {
                scope.launch {
                    try {
                        val rows = buildPeriodRows(results)
                        val cols = listOf(
                            PdfGenerator.Column("#", 0.05f),
                            PdfGenerator.Column("الفترة", 0.18f),
                            PdfGenerator.Column("واجب", 0.09f),
                            PdfGenerator.Column("شفهي", 0.09f),
                            PdfGenerator.Column("غياب", 0.09f),
                            PdfGenerator.Column("مواظبة", 0.10f),
                            PdfGenerator.Column("تحريري", 0.10f),
                            PdfGenerator.Column("المجموع", 0.11f),
                            PdfGenerator.Column("التقدير", 0.19f)
                        )

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
                            title = "كشف الطالب عبر الفترات — ${selectedSubject?.name ?: ""}",
                            meta = buildString {
                                append("الصف: ${classes.find { it.id == classId }?.name ?: ""}")
                                append("  |  الشعبة: ${sections.find { it.id == sectionId }?.name ?: "الكل"}")
                                append("  |  الطالب: ${selectedStudent?.name ?: ""}")
                                append("  |  الرقم: ${selectedStudent?.number ?: ""}")
                            },
                            columns = cols,
                            rows = rows,
                            isLandscape = false,
                            redColumnIndices = setOf(7)
                        )

                        val file = withContext(Dispatchers.IO) {
                            PdfGenerator.generate(
                                context = ctx,
                                reports = listOf(reportData),
                                school = schoolInfo,
                                fileName = "student_periods_${System.currentTimeMillis()}.pdf"
                            )
                        }

                        // ★ فتح PDF مباشرة
                        nav.navigate("pdf_preview/${file.name}")

                        snackbar.showSnackbar(
                            message = "✅ تم توليد كشف الطالب عبر الفترات",
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
            Text("توليد PDF", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        if (canGenerate && results.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        "معاينة — ${selectedStudent?.name} — ${selectedSubject?.name}",
                        fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(6.dp))

                    results.forEach { r ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(r.periodName, Modifier.weight(1f), fontSize = 12.sp)
                            Text("${r.total}", fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (r.isSummary) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(r.rating, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary)
                        }
                        if (r.isSummary) Divider(thickness = 2.dp)
                        else Divider()
                    }
                }
            }
        }
    }
}

data class PeriodResult(
    val periodName: String,
    val homework: String,
    val oral: String,
    val absence: String,
    val attendance: String,
    val written: String,
    val total: Int,
    val rating: String,
    val isSummary: Boolean = false
)

private fun buildPeriodResults(
    studentId: String,
    subjectId: String,
    allGrades: List<com.example.schoolmanager.data.Grade>
): List<PeriodResult> {
    val out = mutableListOf<PeriodResult>()
    val totals = mutableMapOf<Int, Int>()

    GradeCalculator.PERIODS.forEach { (p, name) ->
        val g = allGrades.find {
            it.studentId == studentId &&
            it.subjectId == subjectId &&
            it.period == p
        }
        val isExam = GradeCalculator.isExam(p)
        val computed = GradeCalculator.compute(
            period = p,
            homework = g?.homework ?: 0,
            oral = g?.oral ?: 0,
            absence = g?.absence ?: 0,
            written = g?.written ?: 0
        )
        totals[p] = computed.total

        if (isExam) {
            out.add(PeriodResult(
                periodName = name,
                homework = "-",
                oral = "-",
                absence = "-",
                attendance = "-",
                written = "${computed.written}",
                total = computed.total,
                rating = ratingFor(computed.total, true)
            ))
        } else {
            out.add(PeriodResult(
                periodName = name,
                homework = "${computed.homework}",
                oral = "${computed.oral}",
                absence = "${computed.absence}",
                attendance = "${computed.attendance}",
                written = "${computed.written}",
                total = computed.total,
                rating = ratingFor(computed.total, false)
            ))
        }
    }

    val avg1 = ((totals[1] ?: 0) + (totals[2] ?: 0) + (totals[3] ?: 0)) / 3.0
    val exam1 = totals[4] ?: 0
    val sem1 = (avg1 + exam1).toInt()

    val avg2 = ((totals[5] ?: 0) + (totals[6] ?: 0) + (totals[7] ?: 0)) / 3.0
    val exam2 = totals[8] ?: 0
    val final = (sem1 + avg2 + exam2).toInt()

    out.add(PeriodResult(
        periodName = "النصف الأول (من 50)",
        homework = "-", oral = "-", absence = "-", attendance = "-", written = "-",
        total = sem1,
        rating = ratingFor(sem1, false, 50),
        isSummary = true
    ))
    out.add(PeriodResult(
        periodName = "نهاية العام (من 100)",
        homework = "-", oral = "-", absence = "-", attendance = "-", written = "-",
        total = final,
        rating = ratingFor(final, false, 100),
        isSummary = true
    ))

    return out
}

private fun buildPeriodRows(results: List<PeriodResult>): List<List<String>> {
    return results.mapIndexed { idx, r ->
        listOf(
            "${idx + 1}",
            r.periodName,
            r.homework,
            r.oral,
            r.absence,
            r.attendance,
            r.written,
            "${r.total}",
            r.rating
        )
    }
}

private fun ratingFor(score: Int, isExam: Boolean, customMax: Int = 0): String {
    val max = when {
        customMax > 0 -> customMax
        isExam -> 30
        else -> 20
    }
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
