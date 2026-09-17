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

data class FinalResult(
    val rank: Int,
    val number: String,
    val name: String,
    val subjectScores: List<Int>,
    val total: Int,
    val average: Double,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalResultsScreen(
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

    var classId by remember { mutableStateOf("") }
    var sectionId by remember { mutableStateOf("") }

    var classExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }

    val availableSections = remember(classId, classes, sections) {
        val cls = classes.find { it.id == classId }
        val ids = cls?.sectionIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        sections.filter { it.id in ids }
    }

    val availableSubjects = remember(classId, subjects) {
        subjects.filter { subj ->
            subj.classIds.isBlank() ||
            subj.classIds.split(",").map { it.trim() }.contains(classId)
        }
    }

    val filteredStudents = students.filter { s ->
        (classId.isBlank() || s.classId == classId) &&
        (sectionId.isBlank() || s.sectionId == sectionId)
    }

    val canGenerate = classId.isNotBlank() && filteredStudents.isNotEmpty() && availableSubjects.isNotEmpty()

    val results = remember(classId, sectionId, allGrades, availableSubjects) {
        if (!canGenerate) emptyList()
        else computeFinalResults(filteredStudents, availableSubjects, allGrades)
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
                                onClick = { classId = c.id; sectionId = ""; classExpanded = false }
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

        Spacer(Modifier.height(12.dp))

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Text(
                "📌 حساب النتيجة: النصف الأول (من 50) + النصف الثاني (من 50) = النتيجة النهائية (من 100) لكل مادة. ثم يُرتّب الطلاب حسب المجموع الكلي.",
                Modifier.padding(10.dp),
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        Button(
            enabled = canGenerate,
            onClick = {
                scope.launch {
                    try {
                        // ★ حساب عروض الأعمدة بدقة (المجموع = 1.0 بالضبط)
                        val cols = mutableListOf<PdfGenerator.Column>()

                        // عروض ثابتة
                        val rankNoW = 0.030f       // #
                        val numberW = 0.055f       // رقم
                        val nameW = 0.160f         // الاسم
                        val totalW = 0.080f        // المجموع
                        val avgW = 0.070f          // المعدل
                        val rankW = 0.070f         // الترتيب
                        val statusW = 0.070f       // الحالة

                        val fixedSum = rankNoW + numberW + nameW + totalW + avgW + rankW + statusW
                        val remainingForSubjects = 1.0f - fixedSum
                        val subjectW = remainingForSubjects / availableSubjects.size

                        cols.add(PdfGenerator.Column("#", rankNoW))
                        cols.add(PdfGenerator.Column("رقم", numberW))
                        cols.add(PdfGenerator.Column("اسم الطالب", nameW))

                        availableSubjects.forEach { subj ->
                            cols.add(PdfGenerator.Column(subj.name, subjectW))
                        }

                        cols.add(PdfGenerator.Column("المجموع", totalW))
                        cols.add(PdfGenerator.Column("المعدل", avgW))
                        cols.add(PdfGenerator.Column("الترتيب", rankW))
                        cols.add(PdfGenerator.Column("الحالة", statusW))

                        val rows = results.mapIndexed { idx, r ->
                            val row = mutableListOf<String>()
                            row.add("${idx + 1}")
                            row.add(r.number)
                            row.add(r.name)
                            r.subjectScores.forEach { row.add("$it") }
                            row.add("${r.total}")
                            row.add(String.format("%.1f", r.average))
                            row.add("${r.rank}")
                            row.add(r.status)
                            row
                        }

                        val schoolInfo = PdfGenerator.SchoolInfo(
                            schoolName = settings?.schoolName ?: "",
                            academicYear = settings?.academicYear ?: "",
                            principalName = settings?.principalName ?: "",
                            logoBase64 = settings?.logoBase64 ?: ""
                        )

                        // ★ أفقي دائماً إذا كان عدد المواد 5 أو أكثر
                        val isLandscape = availableSubjects.size >= 5

                        val reportData = PdfGenerator.ReportData(
                            title = "النتيجة النهائية والترتيب",
                            meta = buildString {
                                append("الصف: ${classes.find { it.id == classId }?.name ?: ""}")
                                append("  |  الشعبة: ${sections.find { it.id == sectionId }?.name ?: "الكل"}")
                                append("  |  عدد الطلاب: ${filteredStudents.size}")
                                append("  |  عدد المواد: ${availableSubjects.size}")
                            },
                            columns = cols,
                            rows = rows,
                            isLandscape = isLandscape,
                            redColumnIndices = setOf(
                                cols.size - 4,  // المجموع
                                cols.size - 1   // الحالة
                            )
                        )

                        val file = withContext(Dispatchers.IO) {
                            PdfGenerator.generate(
                                context = ctx,
                                reports = listOf(reportData),
                                school = schoolInfo,
                                fileName = "final_results_${System.currentTimeMillis()}.pdf"
                            )
                        }

                        val uri = FileProvider.getUriForFile(
                            ctx,
                            "${ctx.packageName}.fileprovider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            ctx.startActivity(intent)
                        } catch (e: Exception) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            ctx.startActivity(
                                Intent.createChooser(shareIntent, "افتح PDF").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }

                        snackbar.showSnackbar(
                            message = "✅ تم توليد النتيجة النهائية",
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
                        "معاينة النتائج (${results.size} طالب)",
                        fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(6.dp))

                    Text("🏆 الأوائل:",
                        fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))

                    results.take(5).forEach { r ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${r.rank}", Modifier.width(36.dp), fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                            Text(r.name, Modifier.weight(1f), fontSize = 12.sp)
                            Text("${r.total}", fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("(${String.format("%.1f", r.average)})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary)
                        }
                        Divider()
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("✅ ناجح: ${results.count { it.status == "ناجح" }}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary)
                        Text("❌ راسب: ${results.count { it.status == "راسب" }}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text("اختر الصف لعرض المعاينة",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

private fun computeFinalResults(
    students: List<com.example.schoolmanager.data.Student>,
    subjects: List<com.example.schoolmanager.data.Subject>,
    allGrades: List<com.example.schoolmanager.data.Grade>
): List<FinalResult> {

    data class Intermediate(
        val student: com.example.schoolmanager.data.Student,
        val scores: List<Int>,
        val total: Int
    )

    val intermediate = students.map { student ->
        val subjectScores = subjects.map { subj ->
            val month1 = getPeriodTotal(student.id, subj.id, 1, allGrades)
            val month2 = getPeriodTotal(student.id, subj.id, 2, allGrades)
            val month3 = getPeriodTotal(student.id, subj.id, 3, allGrades)
            val exam1 = getPeriodTotal(student.id, subj.id, 4, allGrades)
            val avg1 = (month1 + month2 + month3) / 3.0
            val sem1 = (avg1 + exam1).toInt()

            val month5 = getPeriodTotal(student.id, subj.id, 5, allGrades)
            val month6 = getPeriodTotal(student.id, subj.id, 6, allGrades)
            val month7 = getPeriodTotal(student.id, subj.id, 7, allGrades)
            val exam2 = getPeriodTotal(student.id, subj.id, 8, allGrades)
            val avg2 = (month5 + month6 + month7) / 3.0
            val finalScore = (sem1 + avg2 + exam2).toInt()

            finalScore.coerceIn(0, 100)
        }
        Intermediate(
            student = student,
            scores = subjectScores,
            total = subjectScores.sum()
        )
    }

    val sorted = intermediate.sortedByDescending { it.total }

    return sorted.mapIndexed { index, item ->
        val passedSubjects = item.scores.count { it >= 50 }
        val status = if (passedSubjects == item.scores.size) "ناجح" else "راسب"
        FinalResult(
            rank = index + 1,
            number = item.student.number,
            name = item.student.name,
            subjectScores = item.scores,
            total = item.total,
            average = if (item.scores.isEmpty()) 0.0 else item.total.toDouble() / item.scores.size,
            status = status
        )
    }
}

private fun getPeriodTotal(
    studentId: String,
    subjectId: String,
    period: Int,
    allGrades: List<com.example.schoolmanager.data.Grade>
): Int {
    val g = allGrades.find {
        it.studentId == studentId &&
        it.subjectId == subjectId &&
        it.period == period
    }
    return GradeCalculator.compute(
        period = period,
        homework = g?.homework ?: 0,
        oral = g?.oral ?: 0,
        absence = g?.absence ?: 0,
        written = g?.written ?: 0
    ).total
}
