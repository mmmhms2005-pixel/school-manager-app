package com.example.schoolmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.schoolmanager.SchoolApplication
import com.example.schoolmanager.data.SchoolClass
import com.example.schoolmanager.data.Section
import com.example.schoolmanager.data.Student
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SchoolApplication
    val dao = app.database.dao()
    val scope = rememberCoroutineScope()

    val students by dao.students().collectAsState(initial = emptyList())
    val classes by dao.classes().collectAsState(initial = emptyList())
    val sections by dao.sections().collectAsState(initial = emptyList())

    var search by remember { mutableStateOf("") }
    var filterClass by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var showBulkDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Student?>(null) }
    var deleting by remember { mutableStateOf<Student?>(null) }

    val filtered = students.filter { s ->
        (search.isBlank() || s.name.contains(search, true) || s.number.contains(search)) &&
        (filterClass.isBlank() || s.classId == filterClass)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الطلاب", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showBulkDialog = true }) {
                        Text("📋+", fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editing = null; showDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة طالب")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("ابحث باسم الطالب أو الرقم") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))

            ScrollableTabRow(selectedTabIndex = 0, edgePadding = 0.dp) {
                FilterChip(
                    selected = filterClass.isEmpty(),
                    onClick = { filterClass = "" },
                    label = { Text("الكل") },
                    modifier = Modifier.padding(4.dp)
                )
                classes.forEach { c ->
                    FilterChip(
                        selected = filterClass == c.id,
                        onClick = { filterClass = c.id },
                        label = { Text(c.name) },
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👨‍🎓", fontSize = 56.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("لا يوجد طلاب", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("اضغط على زر الإضافة لإضافة طالب جديد",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filtered, key = { it.id }) { student ->
                        StudentCard(
                            student = student,
                            className = classes.find { it.id == student.classId }?.name ?: "—",
                            sectionName = sections.find { it.id == student.sectionId }?.name ?: "—",
                            onEdit = { editing = student; showDialog = true },
                            onDelete = { deleting = student }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        StudentDialog(
            student = editing,
            classes = classes,
            sections = sections,
            existingStudents = students,
            onDismiss = { showDialog = false },
            onSave = { s ->
                scope.launch {
                    if (editing == null) dao.insertStudent(s)
                    else dao.updateStudent(s)
                }
                showDialog = false
            }
        )
    }

    if (showBulkDialog) {
        BulkStudentsDialog(
            classes = classes,
            sections = sections,
            existingStudents = students,
            onDismiss = { showBulkDialog = false },
            onSave = { newStudents ->
                scope.launch {
                    newStudents.forEach { dao.insertStudent(it) }
                }
                showBulkDialog = false
            }
        )
    }

    deleting?.let { s ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل تريد حذف الطالب ${s.name}؟") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.deleteStudent(s) }
                    deleting = null
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun StudentCard(
    student: Student,
    className: String,
    sectionName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(student.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text("الرقم: ${student.number} | $className | $sectionName",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                if (student.guardian.isNotBlank() || student.phone.isNotBlank()) {
                    Text("ولي الأمر: ${student.guardian} - ${student.phone}",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "تعديل",
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDialog(
    student: Student?,
    classes: List<SchoolClass>,
    sections: List<Section>,
    existingStudents: List<Student>,
    onDismiss: () -> Unit,
    onSave: (Student) -> Unit
) {
    var number by remember { mutableStateOf(student?.number ?: "") }
    var name by remember { mutableStateOf(student?.name ?: "") }
    var classId by remember { mutableStateOf(student?.classId ?: "") }
    var sectionId by remember { mutableStateOf(student?.sectionId ?: "") }
    var guardian by remember { mutableStateOf(student?.guardian ?: "") }
    var phone by remember { mutableStateOf(student?.phone ?: "") }

    var classExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }

    val allowedSections = remember(classId, classes, sections) {
        val cls = classes.find { it.id == classId }
        val ids = cls?.sectionIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        sections.filter { it.id in ids }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (student == null) "إضافة طالب" else "تعديل بيانات الطالب") },
        text = {
            Column(Modifier.fillMaxWidth().padding(4.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("رقم الطالب (اتركه فارغاً للتوليد التلقائي)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الطالب *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = classExpanded,
                    onExpandedChange = { classExpanded = !classExpanded }
                ) {
                    OutlinedTextField(
                        value = classes.find { it.id == classId }?.name ?: "اختر الصف",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الصف *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = classExpanded, onDismissRequest = { classExpanded = false }) {
                        classes.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    classId = c.id
                                    sectionId = ""
                                    classExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = sectionExpanded,
                    onExpandedChange = { sectionExpanded = !sectionExpanded }
                ) {
                    OutlinedTextField(
                        value = sections.find { it.id == sectionId }?.name ?: "بدون شعبة",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الشعبة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = sectionExpanded, onDismissRequest = { sectionExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("بدون شعبة") },
                            onClick = { sectionId = ""; sectionExpanded = false }
                        )
                        allowedSections.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = { sectionId = s.id; sectionExpanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = guardian,
                    onValueChange = { guardian = it },
                    label = { Text("ولي الأمر") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && classId.isNotBlank(),
                onClick = {
                    val usedNumbers = existingStudents
                        .filter { it.id != student?.id }
                        .map { it.number }
                        .toSet()

                    val trimmedInput = number.trim()
                    val finalNumber: String = when {
                        trimmedInput.isBlank() -> generateUniqueNumber(usedNumbers)
                        usedNumbers.contains(trimmedInput) -> generateUniqueNumber(usedNumbers)
                        else -> trimmedInput
                    }

                    onSave(Student(
                        id = student?.id ?: UUID.randomUUID().toString(),
                        number = finalNumber,
                        name = name.trim(),
                        classId = classId,
                        sectionId = sectionId,
                        guardian = guardian.trim(),
                        phone = phone.trim()
                    ))
                }
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkStudentsDialog(
    classes: List<SchoolClass>,
    sections: List<Section>,
    existingStudents: List<Student>,
    onDismiss: () -> Unit,
    onSave: (List<Student>) -> Unit
) {
    var classId by remember { mutableStateOf("") }
    var sectionId by remember { mutableStateOf("") }
    var guardian by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bulkText by remember { mutableStateOf("") }

    var classExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }

    val nameLines = bulkText.split("\n").map { it.trim() }.filter { it.isNotBlank() }

    val allowedSections = remember(classId, classes, sections) {
        val cls = classes.find { it.id == classId }
        val ids = cls?.sectionIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        sections.filter { it.id in ids }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📋 إضافة عدة طلاب") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                Text(
                    "الصق الأسماء (كل سطر = طالب واحد). سيتم توليد رقم تسلسلي تلقائياً وفريد لكل طالب.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = classExpanded,
                    onExpandedChange = { classExpanded = !classExpanded }
                ) {
                    OutlinedTextField(
                        value = classes.find { it.id == classId }?.name ?: "اختر الصف *",
                        onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(classExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
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
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = sectionExpanded,
                    onExpandedChange = { sectionExpanded = !sectionExpanded }
                ) {
                    OutlinedTextField(
                        value = sections.find { it.id == sectionId }?.name ?: "بدون شعبة",
                        onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sectionExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(sectionExpanded, { sectionExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("بدون شعبة") },
                            onClick = { sectionId = ""; sectionExpanded = false }
                        )
                        allowedSections.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = { sectionId = s.id; sectionExpanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = guardian,
                    onValueChange = { guardian = it },
                    label = { Text("ولي الأمر (اختياري - للجميع)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف (اختياري - للجميع)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = bulkText,
                    onValueChange = { bulkText = it },
                    label = { Text("الأسماء (كل سطر = طالب)") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                    minLines = 8
                )

                Spacer(Modifier.height(8.dp))
                if (nameLines.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth()) {
                        Text(
                            "✅ سيتم إضافة ${nameLines.size} طالب",
                            Modifier.padding(10.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = classId.isNotBlank() && nameLines.isNotEmpty(),
                onClick = {
                    val usedNumbers = existingStudents.map { it.number }.toMutableSet()
                    val newStudents = nameLines.map { line ->
                        val num = generateUniqueNumber(usedNumbers)
                        usedNumbers.add(num)
                        Student(
                            id = UUID.randomUUID().toString(),
                            number = num,
                            name = line,
                            classId = classId,
                            sectionId = sectionId,
                            guardian = guardian.trim(),
                            phone = phone.trim()
                        )
                    }
                    onSave(newStudents)
                }
            ) { Text("إضافة الكل") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

private fun generateUniqueNumber(usedNumbers: Set<String>): String {
    var n = 1
    while (usedNumbers.contains(n.toString())) n++
    return n.toString()
}
