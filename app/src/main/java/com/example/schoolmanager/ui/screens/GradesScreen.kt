package com.example.schoolmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.schoolmanager.SchoolApplication
import com.example.schoolmanager.data.GradeCalculator
import com.example.schoolmanager.data.Grade
import com.example.schoolmanager.data.Student
import kotlinx.coroutines.launch
import java.util.UUID

object GradeRowStateHolder {
    private val map = mutableStateMapOf<String, GradeRowState>()
    fun update(studentId: String, state: GradeRowState) { map[studentId] = state }
    fun snapshot(): Map<String, GradeRowState> = map.toMap()
    fun clear() = map.clear()
}

data class GradeRowState(
    val homework: Int, val oral: Int, val absence: Int, val written: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SchoolApplication
    val dao = app.database.dao()
    val scope = rememberCoroutineScope()

    val classes by dao.classes().collectAsState(initial = emptyList())
    val sections by dao.sections().collectAsState(initial = emptyList())
    val subjects by dao.subjects().collectAsState(initial = emptyList())
    val students by dao.students().collectAsState(initial = emptyList())
    val allGrades by dao.grades().collectAsState(initial = emptyList())

    var classId by remember { mutableStateOf("") }
    var sectionId by remember { mutableStateOf("") }
    var subjectId by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(1) }

    var classExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }
    var periodExpanded by remember { mutableStateOf(false) }

    val filteredStudents = students.filter { s ->
        (classId.isBlank() || s.classId == classId) &&
        (sectionId.isBlank() || s.sectionId == sectionId)
    }.sortedBy { it.number.toIntOrNull() ?: 0 }

    val availableSubjects = subjects.filter { subj ->
        subj.classIds.isBlank() || subj.classIds.split(",").contains(classId)
    }

    val isExam = GradeCalculator.isExam(period)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدخال الدرجات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
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
        Column(Modifier.fillMaxSize().padding(padding).padding(10.dp)) {

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
                            if (availableSubjects.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("اختر الصف أولاً") },
                                    onClick = { subjectExpanded = false }
                                )
                            }
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

            Spacer(Modifier.height(8.dp))

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExam)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    if (isExam)
                        "📌 فترة امتحان: الدرجة من 30 (التحريري فقط)."
                    else
                        "📌 الواجبات(20) + الشفهي(20) + المواظبة(20) + التحريري(40) ÷ 5. المواظبة = 20 − الغياب.",
                    Modifier.padding(10.dp),
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            if (classId.isBlank() || subjectId.isBlank() || filteredStudents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📝", fontSize = 56.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (filteredStudents.isEmpty() && classId.isNotBlank())
                                "لا يوجد طلاب في هذا الصف"
                            else "اختر الصف والمادة",
                            fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            saveAllGrades(dao, filteredStudents, subjectId, period)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("حفظ الدرجات", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))

                Card(Modifier.weight(1f)) {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier.fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HeaderCell("#", 28.dp)
                            HeaderCell("الاسم", 120.dp)
                            if (isExam) {
                                HeaderCell("تحريري/30", 75.dp)
                                HeaderCell("المجموع", 60.dp)
                            } else {
                                HeaderCell("واجب/20", 70.dp)
                                HeaderCell("شفهي/20", 70.dp)
                                HeaderCell("غياب", 55.dp)
                                HeaderCell("مواظبة", 60.dp)
                                HeaderCell("تحريري/40", 75.dp)
                                HeaderCell("المجموع", 60.dp)
                            }
                        }
                        Divider()
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(filteredStudents, key = { it.id }) { student ->
                                GradeRow(
                                    student = student,
                                    period = period,
                                    existing = allGrades.find {
                                        it.studentId == student.id &&
                                        it.subjectId == subjectId &&
                                        it.period == period
                                    }
                                )
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Box(Modifier.width(width), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            maxLines = 2, textAlign = TextAlign.Center)
    }
}

@Composable
private fun GradeRow(student: Student, period: Int, existing: Grade?) {
    val isExam = GradeCalculator.isExam(period)

    var homework by remember(student.id, period) {
        mutableStateOf(existing?.homework?.takeIf { it > 0 }?.toString() ?: "")
    }
    var oral by remember(student.id, period) {
        mutableStateOf(existing?.oral?.takeIf { it > 0 }?.toString() ?: "")
    }
    var absence by remember(student.id, period) {
        mutableStateOf(existing?.absence?.takeIf { it > 0 }?.toString() ?: "")
    }
    var written by remember(student.id, period) {
        mutableStateOf(existing?.written?.takeIf { it > 0 }?.toString() ?: "")
    }

    LaunchedEffect(homework, oral, absence, written) {
        GradeRowStateHolder.update(student.id, GradeRowState(
            homework = homework.toIntOrNull() ?: 0,
            oral = oral.toIntOrNull() ?: 0,
            absence = absence.toIntOrNull() ?: 0,
            written = written.toIntOrNull() ?: 0
        ))
    }

    val computed = GradeCalculator.compute(
        period = period,
        homework = homework.toIntOrNull() ?: 0,
        oral = oral.toIntOrNull() ?: 0,
        absence = absence.toIntOrNull() ?: 0,
        written = written.toIntOrNull() ?: 0
    )

    Row(
        Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
            Text(student.number, fontSize = 10.sp)
        }
        Box(Modifier.width(120.dp)) {
            Text(student.name, fontSize = 11.sp, maxLines = 1)
        }
        if (isExam) {
            NumberInput(written, { written = it }, 75.dp, 30, ImeAction.Done)
            Box(Modifier.width(60.dp), contentAlignment = Alignment.Center) {
                Text("${computed.total}", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error)
            }
        } else {
            NumberInput(homework, { homework = it }, 70.dp, 20, ImeAction.Next)
            NumberInput(oral, { oral = it }, 70.dp, 20, ImeAction.Next)
            NumberInput(absence, { absence = it }, 55.dp, 20, ImeAction.Next)
            Box(Modifier.width(60.dp), contentAlignment = Alignment.Center) {
                Text("${computed.attendance}", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary)
            }
            NumberInput(written, { written = it }, 75.dp, 40, ImeAction.Done)
            Box(Modifier.width(60.dp), contentAlignment = Alignment.Center) {
                Text("${computed.total}", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun NumberInput(
    value: String,
    onValueChange: (String) -> Unit,
    width: androidx.compose.ui.unit.Dp,
    max: Int,
    imeAction: ImeAction
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            val filtered = new.filter(Char::isDigit)
            val intVal = filtered.toIntOrNull() ?: 0
            onValueChange(if (intVal > max) max.toString() else filtered)
        },
        modifier = Modifier.width(width).height(50.dp),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            fontSize = 12.sp, textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        )
    )
}

private suspend fun saveAllGrades(
    dao: com.example.schoolmanager.data.SchoolDao,
    students: List<Student>,
    subjectId: String,
    period: Int
) {
    val snapshot = GradeRowStateHolder.snapshot()
    students.forEach { student ->
        val state = snapshot[student.id] ?: return@forEach
        val computed = GradeCalculator.compute(
            period = period,
            homework = state.homework,
            oral = state.oral,
            absence = state.absence,
            written = state.written
        )
        val existing = dao.findGrade(student.id, subjectId, period)
        val grade = Grade(
            id = existing?.id ?: UUID.randomUUID().toString(),
            studentId = student.id,
            subjectId = subjectId,
            period = period,
            homework = computed.homework,
            oral = computed.oral,
            absence = computed.absence,
            attendance = computed.attendance,
            written = computed.written,
            total = computed.total
        )
        if (existing == null) dao.insertGrade(grade) else dao.updateGrade(grade)
    }
    GradeRowStateHolder.clear()
}
