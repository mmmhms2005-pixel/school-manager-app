package com.example.schoolmanager.ui.screens

import android.content.Intent
import com.example.schoolmanager.data.GradeCalculator
import com.example.schoolmanager.data.WhatsAppHelper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.schoolmanager.data.HomeroomTeacherHelper
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
import com.example.schoolmanager.data.Student
import com.example.schoolmanager.data.Subject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyCertificateScreen(
    nav: NavController,
    dao: SchoolDao,
    scope: CoroutineScope,
    snackbar: SnackbarHostState
) {
    val allTeachers by dao.teachers().collectAsState(initial = emptyList())
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
    var period by remember { mutableStateOf(1) }

    var classExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }
    var periodExpanded by remember { mutableStateOf(false) }

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
    }.sortedBy { it.number.toIntOrNull() ?: 0 }

    val canGenerate = classId.isNotBlank() &&
            availableSubjects.isNotEmpty() &&
            filteredStudents.isNotEmpty()
var showWhatsAppDialog by remember { mutableStateOf(false) }
var whatsAppEntries by remember { mutableStateOf<List<MonthlyCertEntry>>(emptyList()) }
var currentStudentIndex by remember { mutableStateOf(0) }
    val pageCount = (filteredStudents.size + 1) / 2 // شهادتان في كل صفحة

    Column(
        Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState())
    ) {
        // ═══ الصف + الشعبة ═══
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

        Spacer(Modifier.height(6.dp))

        // ═══ الشهر ═══
        ExposedDropdownMenuBox(periodExpanded, { periodExpanded = !periodExpanded }) {
            OutlinedTextField(
                value = GradeCalculator.periodName(period),
                onValueChange = {}, readOnly = true,
                label = { Text("الشهر") },
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

        Spacer(Modifier.height(12.dp))

        // ═══ ملاحظة توضيحية ═══
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "📌 الشهادات تُولّد بحجم A5 (شهادتان في كل ورقة A4)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "• خط قص متقطع في منتصف كل صفحة.\n" +
                    "• يمكن قصها وتوزيعها على الطلاب.\n" +
                    "• كل شهادة تحتوي: درجات جميع المواد + المجموع + المتوسط + التقدير.",
                    fontSize = 11.sp
                )
                if (canGenerate) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "📄 عدد الطلاب: ${filteredStudents.size}  |  عدد الأوراق: $pageCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ═══ زر التوليد ═══
        Button(
            enabled = canGenerate,
            onClick = {
                scope.launch {
                    try {
                        val entries = buildMonthlyCertificates(
                            filteredStudents = filteredStudents,
                            subjects = availableSubjects,
                            allGrades = allGrades,
                            period = period,
                            classes = classes,
                            sections = sections
                        )
                      
                        val homeroomName = HomeroomTeacherHelper.getName(
   classes = classes,    
    teachers = allTeachers,
classId = classId,
sectionId = sectionId
)

                        val schoolInfo = PdfGenerator.SchoolInfo(
                            schoolName = settings?.schoolName ?: "",
                            academicYear = settings?.academicYear ?: "",
                            principalName = settings?.principalName ?: "",
                            homeroomTeacherName = homeroomName,
                            logoBase64 = settings?.logoBase64 ?: ""
                        )

                        val file = withContext(Dispatchers.IO) {
                            PdfGenerator.generateMonthlyCertificates(
                                context = ctx,
                                entries = entries,
                                school = schoolInfo,
                                month = GradeCalculator.periodName(period),
                                fileName = "monthly_certs_${System.currentTimeMillis()}.pdf"
                            )
                        }

                        nav.navigate("pdf_preview/${file.name}")

                        snackbar.showSnackbar(
                            message = "✅ تم توليد ${entries.size} شهادة",
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
            Text(
                "🎓 توليد شهادات الطلاب (PDF)",
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))

// ★★★ زر إرسال واتساب ★★★
Button(
    enabled = canGenerate,
    onClick = {
        scope.launch {
            try {
                val entries = buildMonthlyCertificates(
                    filteredStudents = filteredStudents,
                    subjects = availableSubjects,
                    allGrades = allGrades,
                    period = period,
                    classes = classes,
                    sections = sections
                )
                whatsAppEntries = entries
                currentStudentIndex = 0
                showWhatsAppDialog = true
            } catch (e: Exception) {
                snackbar.showSnackbar("❌ فشل: ${e.message}")
            }
        }
    },
    modifier = Modifier.fillMaxWidth()
) {
    Icon(Icons.Default.Email, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text("إرسال عبر واتساب", fontWeight = FontWeight.Bold)
}

        Spacer(Modifier.height(16.dp))

        // ═══ المعاينة ═══
        if (canGenerate) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        "معاينة (${filteredStudents.size} طالب)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(6.dp))

                    filteredStudents.take(5).forEachIndexed { idx, student ->
                        val total = availableSubjects.sumOf { subj ->
                            val g = allGrades.find {
                                it.studentId == student.id &&
                                it.subjectId == subj.id &&
                                it.period == period
                            }
                            GradeCalculator.compute(
                                period = period,
                                homework = g?.homework ?: 0,
                                oral = g?.oral ?: 0,
                                absence = g?.absence ?: 0,
                                written = g?.written ?: 0
                            ).total
                        }
                        val avg = if (availableSubjects.isNotEmpty())
                            total.toDouble() / availableSubjects.size else 0.0

                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${idx + 1}", Modifier.width(24.dp), fontSize = 11.sp)
                            Text(student.name, Modifier.weight(1f), fontSize = 12.sp)
                            Text("المجموع: $total", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                String.format("(%.1f)", avg),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Divider()
                    }
                    if (filteredStudents.size > 5) {
                        Text(
                            "... و ${filteredStudents.size - 5} طالب آخر",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        } else {
            Box(
                Modifier.fillMaxWidth().height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎓", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "اختر الصف لعرض المعاينة",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
    // ★★★ نافذة إرسال واتساب ★★★
if (showWhatsAppDialog && whatsAppEntries.isNotEmpty()) {
    val entry = whatsAppEntries[currentStudentIndex]
    val student = filteredStudents.find { it.name == entry.studentName }

    AlertDialog(
        onDismissRequest = { showWhatsAppDialog = false },
        title = { Text("إرسال شهادات الطلاب عبر واتساب") },
        text = {
            Column {
                Text("الطالب ${currentStudentIndex + 1} من ${whatsAppEntries.size}",
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text("👤 الطالب: ${entry.studentName}")
                Text("👨‍👦 ولي الأمر: ${student?.guardian ?: "غير محدد"}")
                Text("📞 الهاتف: ${student?.phone ?: "غير محدد"}")
                Spacer(Modifier.height(8.dp))
                Text("سيتم فتح واتساب مع الرسالة جاهزة للإرسال",
                    fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(onClick = {
                val phone = student?.phone ?: ""
                if (phone.isNotBlank()) {
                    WhatsAppHelper.sendViaWhatsApp(
                        ctx,
                        phone,
                        WhatsAppHelper.buildMonthlyCertMessage(entry, schoolInfo, GradeCalculator.periodName(period))
                    )
                }
            }) {
                Text("📤 إرسال")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    if (currentStudentIndex > 0) currentStudentIndex--
                }) {
                    Text("◀ السابق")
                }
                TextButton(onClick = {
                    if (currentStudentIndex < whatsAppEntries.size - 1) {
                        currentStudentIndex++
                    } else {
                        showWhatsAppDialog = false
                    }
                }) {
                    Text("التالي ▶")
                }
                TextButton(onClick = { showWhatsAppDialog = false }) {
                    Text("إغلاق")
                }
            }
        }
    )
}
}

/**
 * بناء بيانات الشهادات الشهرية لكل طالب.
 */
private fun buildMonthlyCertificates(
    filteredStudents: List<Student>,
    subjects: List<Subject>,
    allGrades: List<com.example.schoolmanager.data.Grade>,
    period: Int,
    classes: List<com.example.schoolmanager.data.SchoolClass>,
    sections: List<com.example.schoolmanager.data.Section>
): List<PdfGenerator.MonthlyCertEntry> {
    return filteredStudents.map { student ->
        val grades = subjects.map { subj ->
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
            val maxScore = GradeCalculator.maxFor(period)
            PdfGenerator.SubjectGrade(
                subjectName = subj.name,
                score = computed.total,
                maxScore = maxScore,
                rating = ratingFor(computed.total, maxScore)
            )
        }

        val className = classes.find { it.id == student.classId }?.name ?: "—"
        val sectionName = sections.find { it.id == student.sectionId }?.name ?: "—"

        PdfGenerator.MonthlyCertEntry(
            studentNumber = student.number,
            studentName = student.name,
            className = className,
            sectionName = sectionName,
            grades = grades
        )
    }
}

private fun ratingFor(score: Int, max: Int): String {
    if (max == 0) return "-"
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
