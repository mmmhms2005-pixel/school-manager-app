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
fun ClassReportScreen(
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
    var subjectId by remember { mutableStateOf("") }

    var classExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }

    val availableSections = remember(classId, classes, sections) {
        val cls = classes.find { it.id == classId }
        val ids = cls?.sectionIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        sections.filter { it.id in ids }
    }

    val availableSubjects = subjects.filter {
        it.classIds.isBlank() ||
        it.classIds.split(",").map { s -> s.trim() }.contains(classId)
    }.ifEmpty { subjects }

    val filteredStudents = students.filter { s ->
        (classId.isBlank() || s.classId == classId) &&
        (sectionId.isBlank() || s.sectionId == sectionId)
    }.sortedBy { it.number.toIntOrNull() ?: 0 }

    val canGenerate = classId.isNotBlank() && subjectId.isNotBlank() && filteredStudents.isNotEmpty()

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
                        availableSections.forEach { s ->
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

        Spacer(Modifier.height(12.dp))

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Text(
                "📌 هذا الكشف يُولّد بصيغة أفقية (Landscape) ويشمل كل الطلاب × الفترات الـ 8.",
                Modifier.padding(10.dp),
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        Button(
            enabled = canGenerate,
            onClick = {
                scope.launch {
                    try {
                        val data = buildClassReportData(
                            filteredStudents, subjectId, allGrades
                        )
                        val cols = mutableListOf<PdfGenerator.Column>()
                        cols.add(PdfGenerator.Column("#", 0.03f))
                        cols.add(PdfGenerator.Column("رقم", 0.06f))
                        cols.add(PdfGenerator.Column("اسم الطالب", 0.19f))
                        GradeCalculator.PERIODS.forEach { (_, name) ->
                            cols.add(PdfGenerator.Column(name, 0.072f))
                        }
                        cols.add(PdfGenerator.Column("النصف الأول", 0.09f))
                        cols.add(PdfGenerator.Column("نهاية العام", 0.10f))

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
                            title = "الكشف الشامل للصف — ${subjects.find { it.id == subjectId }?.name ?: ""}",
                            meta = buildString {
                                append("الصف: ${classes.find { it.id == classId }?.name ?: ""}")
                                append("  |  الشعبة: ${sections.find { it.id == sectionId }?.name ?: "الكل"}")
                                append("  |  عدد الطلاب: ${filteredStudents.size}")
                            },
                            columns = cols,
                            rows = data,
                            isLandscape = true,
                            redColumnIndices = setOf(cols.size - 1, cols.size - 2)
                        )

                        val file = withContext(Dispatchers.IO) {
                            PdfGenerator.generate(
                                context = ctx,
                                reports = listOf(reportData),
                                school = schoolInfo,
                                fileName = "class_report_${System.currentTimeMillis()}.pdf"
                            )
                        }

                        // ★ فتح PDF مباشرة
                        openPdfDirectly(ctx, file)

                        snackbar.showSnackbar(
                            message = "✅ تم توليد الكشف الشامل",
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
            Text("توليد PDF (أفقي)", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        if (canGenerate) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        "معاينة (${filteredStudents.size} طالب)",
                        fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(6.dp))

                    filteredStudents.take(5).forEach { student ->
                        val totals = (1..8).map { p ->
                            val g = allGrades.find {
                                it.studentId == student.id &&
                                it.subjectId == subjectId &&
                                it.period == p
                            }
                            GradeCalculator.compute(
                                period = p,
                                homework = g?.homework ?: 0,
                                oral = g?.oral ?: 0,
                                absence = g?.absence ?: 0,
                                written = g?.written ?: 0
                            ).total
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(student.name, Modifier.weight(1f), fontSize = 12.sp)
                            Text(totals.joinToString(" | "),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.secondary)
                        }
                        Divider()
                    }
                    if (filteredStudents.size > 5) {
                        Text(
                            "... و ${filteredStudents.size - 5} طالب آخر",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

private fun buildClassReportData(
    students: List<com.example.schoolmanager.data.Student>,
    subjectId: String,
    allGrades: List<com.example.schoolmanager.data.Grade>
): List<List<String>> {
    return students.mapIndexed { idx, student ->
        val row = mutableListOf<String>()
        row.add("${idx + 1}")
        row.add(student.number)
        row.add(student.name)

        val totals = mutableMapOf<Int, Int>()
        GradeCalculator.PERIODS.forEach { (p, _) ->
            val g = allGrades.find {
                it.studentId == student.id &&
                it.subjectId == subjectId &&
                it.period == p
            }
            val total = GradeCalculator.compute(
                period = p,
                homework = g?.homework ?: 0,
                oral = g?.oral ?: 0,
                absence = g?.absence ?: 0,
                written = g?.written ?: 0
            ).total
            totals[p] = total
            row.add("$total")
        }

        val avg1 = ((totals[1] ?: 0) + (totals[2] ?: 0) + (totals[3] ?: 0)) / 3.0
        val sem1 = (avg1 + (totals[4] ?: 0)).toInt()

        val avg2 = ((totals[5] ?: 0) + (totals[6] ?: 0) + (totals[7] ?: 0)) / 3.0
        val final = (sem1 + avg2 + (totals[8] ?: 0)).toInt()

        row.add("$sem1")
        row.add("$final")

        row
    }
}
