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
import com.example.schoolmanager.data.HomeroomTeacherHelper
import com.example.schoolmanager.data.PdfGenerator
import com.example.schoolmanager.data.SchoolDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Analytics(
    val studentCount: Int,
    val subjectCount: Int,
    val avgPerSubject: Map<String, Double>,
    val highestPerSubject: Map<String, Pair<String, Int>>,
    val lowestPerSubject: Map<String, Pair<String, Int>>,
    val passCount: Int,
    val failCount: Int,
    val topStudents: List<Triple<Int, String, Int>>,
    val gradeDistribution: Map<String, Int>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
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
    val teachers by dao.teachers().collectAsState(initial = emptyList())

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

    val analytics = remember(classId, sectionId, allGrades, availableSubjects) {
        if (!canGenerate) null
        else computeAnalytics(filteredStudents, availableSubjects, allGrades)
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

        Button(
            enabled = canGenerate,
            onClick = {
                scope.launch {
                    try {
                        val a = analytics ?: return@launch

                        val cols = listOf(
                            PdfGenerator.Column("المادة", 0.30f),
                            PdfGenerator.Column("المتوسط", 0.15f),
                            PdfGenerator.Column("الأعلى", 0.18f),
                            PdfGenerator.Column("الأدنى", 0.18f),
                            PdfGenerator.Column("نسبة النجاح", 0.19f)
                        )

                        val rows = availableSubjects.map { subj ->
    val avg = a.avgPerSubject[subj.id] ?: 0.0
    val high = a.highestPerSubject[subj.id]
    val low = a.lowestPerSubject[subj.id]
    listOf(
        subj.name,
        String.format("%.1f", avg),
        high?.let { "${it.second}" } ?: "-",
        low?.let { "${it.second}" } ?: "-",
        "_"
    )
}
                        val homeroomName = HomeroomTeacherHelper.getName(
                            classId = classId,
                            sectionId = sectionId,
                            classes = classes,
                            teachers = teachers
                        )

                        val schoolInfo = PdfGenerator.SchoolInfo(
                            schoolName = settings?.schoolName ?: "",
                            academicYear = settings?.academicYear ?: "",
                            principalName = settings?.principalName ?: "",
                            homeroomTeacherName = homeroomName,
                            logoBase64 = settings?.logoBase64 ?: ""
                        )

                        val page1Rows = mutableListOf<List<String>>()
                        page1Rows.add(listOf("عدد الطلاب", "${a.studentCount}"))
                        page1Rows.add(listOf("عدد المواد", "${a.subjectCount}"))
                        page1Rows.add(listOf("الناجحون", "${a.passCount}"))
                        page1Rows.add(listOf("الراسبون", "${a.failCount}"))
                        page1Rows.add(listOf("نسبة النجاح", "${(a.passCount * 100 / a.studentCount.coerceAtLeast(1))}%"))

                        val page1Cols = listOf(
                            PdfGenerator.Column("البند", 0.50f),
                            PdfGenerator.Column("القيمة", 0.50f)
                        )

                        val page2Rows = a.topStudents.map { (rank, name, total) ->
                            listOf("$rank", name, "$total")
                        }
                        val page2Cols = listOf(
                            PdfGenerator.Column("الترتيب", 0.15f),
                            PdfGenerator.Column("اسم الطالب", 0.60f),
                            PdfGenerator.Column("المجموع", 0.25f)
                        )

                        val reports = mutableListOf<PdfGenerator.ReportData>()
                        reports.add(
                            PdfGenerator.ReportData(
                                title = "لوحة الإحصائيات — ملخص عام",
                                meta = "الصف: ${classes.find { it.id == classId }?.name ?: ""}  |  الشعبة: ${sections.find { it.id == sectionId }?.name ?: "الكل"}",
                                columns = page1Cols,
                                rows = page1Rows,
                                isLandscape = false,
                                isA5 = true,
                                isMultiSubject = true
                            )
                        )
                        reports.add(
                            PdfGenerator.ReportData(
                                title = "تحليل المواد",
                                meta = "متوسط وأعلى وأدنى درجة لكل مادة",
                                columns = cols,
                                rows = rows,
                                isLandscape = false,
                                isA5 = true,
                                isMultiSubject = true
                            )
                        )
                        if (a.topStudents.isNotEmpty()) {
                            reports.add(
                                PdfGenerator.ReportData(
                                    title = "الأوائل",
                                    meta = "أفضل 10 طلاب في الصف",
                                    columns = page2Cols,
                                    rows = page2Rows,
                                    isLandscape = false,
                                    isA5 = true,
                                    redColumnIndices = setOf(2),
                                    isMultiSubject = true
                                )
                            )
                        }

                        val file = withContext(Dispatchers.IO) {
                            PdfGenerator.generate(
                                context = ctx,
                                reports = reports,
                                school = schoolInfo,
                                fileName = "analytics_${System.currentTimeMillis()}.pdf"
                            )
                        }

                        // ★ فتح PDF مباشرة
                        openPdfDirectly(ctx, file)

                        snackbar.showSnackbar(
                            message = "✅ تم توليد لوحة الإحصائيات",
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

        if (analytics != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("📊 ملخص عام",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatBox("الطلاب", "${analytics.studentCount}")
                        StatBox("المواد", "${analytics.subjectCount}")
                        StatBox("ناجح", "${analytics.passCount}")
                        StatBox("راسب", "${analytics.failCount}")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("📚 متوسط الدرجات لكل مادة",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    availableSubjects.forEach { subj ->
                        val avg = analytics.avgPerSubject[subj.id] ?: 0.0
                        val high = analytics.highestPerSubject[subj.id]
                        val low = analytics.lowestPerSubject[subj.id]
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(subj.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(String.format("%.1f", avg), fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(2.dp))
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("أعلى: ${high?.first ?: "-"} (${high?.second ?: 0})",
                                    fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                Text("أدنى: ${low?.first ?: "-"} (${low?.second ?: 0})",
                                    fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Divider()
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (analytics.topStudents.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("🏆 الأوائل",
                            fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Divider()
                        analytics.topStudents.forEach { (rank, name, total) ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#$rank", Modifier.width(36.dp),
                                    fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary)
                                Text(name, Modifier.weight(1f), fontSize = 12.sp)
                                Text("$total", fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error)
                            }
                            Divider()
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("📈 توزيع التقديرات",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    analytics.gradeDistribution.forEach { (rating, count) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(rating, fontSize = 12.sp)
                            Text("$count طالب", fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Divider()
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().height(150.dp),
                contentAlignment = Alignment.Center) {
                Text("اختر الصف لعرض الإحصائيات",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp,
            color = MaterialTheme.colorScheme.secondary)
    }
}

private fun computeAnalytics(
    students: List<com.example.schoolmanager.data.Student>,
    subjects: List<com.example.schoolmanager.data.Subject>,
    allGrades: List<com.example.schoolmanager.data.Grade>
): Analytics {

    val studentCount = students.size
    val subjectCount = subjects.size

    val studentScores: Map<String, Map<String, Int>> = students.associate { student ->
        student.id to subjects.associate { subj ->
            subj.id to computeFinalSubjectScore(student.id, subj.id, allGrades)
        }
    }

    val avgPerSubject = mutableMapOf<String, Double>()
    val highestPerSubject = mutableMapOf<String, Pair<String, Int>>()
    val lowestPerSubject = mutableMapOf<String, Pair<String, Int>>()

    subjects.forEach { subj ->
        val scores = students.mapNotNull { student ->
            studentScores[student.id]?.get(subj.id)?.let { score ->
                student to score
            }
        }
        if (scores.isNotEmpty()) {
            avgPerSubject[subj.id] = scores.map { it.second }.average()
            val highest = scores.maxByOrNull { it.second }!!
            val lowest = scores.minByOrNull { it.second }!!
            highestPerSubject[subj.id] = highest.first.name to highest.second
            lowestPerSubject[subj.id] = lowest.first.name to lowest.second
        }
    }

    var passCount = 0
    var failCount = 0
    val gradeDistribution = mutableMapOf<String, Int>()

    val studentTotals = students.map { student ->
        val subjectScores = studentScores[student.id] ?: emptyMap()
        val total = subjectScores.values.sum()
        val passedAll = subjectScores.values.all { it >= 50 }
        if (passedAll) passCount++ else failCount++

        val avg = if (subjectScores.isNotEmpty())
            subjectScores.values.average() else 0.0
        val rating = when {
            avg >= 90 -> "ممتاز"
            avg >= 80 -> "جيد جداً"
            avg >= 70 -> "جيد"
            avg >= 60 -> "مقبول"
            avg >= 50 -> "ضعيف"
            else -> "راسب"
        }
        gradeDistribution[rating] = (gradeDistribution[rating] ?: 0) + 1

        Triple(student.name, student.id, total)
    }.sortedByDescending { it.third }

    val topStudents = studentTotals.take(10).mapIndexed { idx, t ->
        Triple(idx + 1, t.first, t.third)
    }

    val orderedDistribution = linkedMapOf<String, Int>()
    listOf("ممتاز", "جيد جداً", "جيد", "مقبول", "ضعيف", "راسب").forEach { r ->
        if (gradeDistribution.containsKey(r)) {
            orderedDistribution[r] = gradeDistribution[r]!!
        }
    }

    return Analytics(
        studentCount = studentCount,
        subjectCount = subjectCount,
        avgPerSubject = avgPerSubject,
        highestPerSubject = highestPerSubject,
        lowestPerSubject = lowestPerSubject,
        passCount = passCount,
        failCount = failCount,
        topStudents = topStudents,
        gradeDistribution = orderedDistribution
    )
}

private fun computeFinalSubjectScore(
    studentId: String,
    subjectId: String,
    allGrades: List<com.example.schoolmanager.data.Grade>
): Int {
    fun periodTotal(p: Int): Int {
        val g = allGrades.find {
            it.studentId == studentId &&
            it.subjectId == subjectId &&
            it.period == p
        }
        return GradeCalculator.compute(
            period = p,
            homework = g?.homework ?: 0,
            oral = g?.oral ?: 0,
            absence = g?.absence ?: 0,
            written = g?.written ?: 0
        ).total
    }

    val avg1 = (periodTotal(1) + periodTotal(2) + periodTotal(3)) / 3.0
    val sem1 = (avg1 + periodTotal(4)).toInt()

    val avg2 = (periodTotal(5) + periodTotal(6) + periodTotal(7)) / 3.0
    val finalScore = (sem1 + avg2 + periodTotal(8)).toInt()

    return finalScore.coerceIn(0, 100)
}
