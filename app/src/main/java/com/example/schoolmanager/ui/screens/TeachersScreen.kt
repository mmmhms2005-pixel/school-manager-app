package com.example.schoolmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.example.schoolmanager.data.Subject
import com.example.schoolmanager.data.Teacher
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TeachersScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SchoolApplication
    val dao = app.database.dao()
    val scope = rememberCoroutineScope()

    val teachers by dao.teachers().collectAsState(initial = emptyList())
    val subjects by dao.subjects().collectAsState(initial = emptyList())
    val classes by dao.classes().collectAsState(initial = emptyList())
    val sections by dao.sections().collectAsState(initial = emptyList())

    var search by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Teacher?>(null) }
    var deleting by remember { mutableStateOf<Teacher?>(null) }

    val filtered = teachers.filter { t ->
        search.isBlank() ||
        t.name.contains(search, true) ||
        t.specialization.contains(search, true) ||
        t.phone.contains(search, true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المعلمون", fontWeight = FontWeight.Bold) },
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة معلم") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("ابحث بالاسم أو التخصص") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👨‍🏫", fontSize = 56.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("لا يوجد معلمون", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("اضغط على زر الإضافة لإضافة معلم جديد",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { teacher ->
                        TeacherCard(
                            teacher = teacher,
                            subjects = subjects,
                            classes = classes,
                            sectionNames = teacher.sectionIds
                                .split(",").filter { it.isNotBlank() }
                                .mapNotNull { sid -> sections.find { it.id == sid }?.name },
                            onEdit = { editing = teacher; showDialog = true },
                            onDelete = { deleting = teacher }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        TeacherDialog(
            teacher = editing,
            subjects = subjects,
            classes = classes,
            sections = sections,
            onDismiss = { showDialog = false },
            onSave = { t ->
                scope.launch {
                    if (editing == null) dao.insertTeacher(t)
                    else dao.updateTeacher(t)
                }
                showDialog = false
            }
        )
    }

    deleting?.let { t ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل تريد حذف المعلم ${t.name}؟") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.deleteTeacher(t) }
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
fun TeacherCard(
    teacher: Teacher,
    subjects: List<Subject>,
    classes: List<SchoolClass>,
    sectionNames: List<String>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val bySubject = remember(teacher.assignments, subjects, classes) {
        val map = mutableMapOf<String, MutableList<String>>()
        teacher.assignments.split(",").forEach { pair ->
            val parts = pair.split(":")
            if (parts.size == 2) {
                val sid = parts[0].trim()
                val cid = parts[1].trim()
                val className = classes.find { it.id == cid }?.name ?: return@forEach
                map.getOrPut(sid) { mutableListOf() }.add(className)
            }
        }
        map
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👨‍🏫", fontSize = 32.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(teacher.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (teacher.specialization.isNotBlank())
                        Text(teacher.specialization, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary)
                    if (teacher.phone.isNotBlank())
                        Text("📞 ${teacher.phone}", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل",
                        tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.error)
                }
            }

            if (bySubject.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                bySubject.forEach { (sid, classNames) ->
                    val subjectName = subjects.find { it.id == sid }?.name ?: return@forEach
                    Text(
                        "📚 $subjectName: ${classNames.joinToString("، ")}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }

            if (sectionNames.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("🧩 الشعب: ${sectionNames.joinToString("، ")}",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
            }

            if (teacher.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("📝 ${teacher.notes}", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TeacherDialog(
    teacher: Teacher?,
    subjects: List<Subject>,
    classes: List<SchoolClass>,
    sections: List<Section>,
    onDismiss: () -> Unit,
    onSave: (Teacher) -> Unit
) {
    var name by remember { mutableStateOf(teacher?.name ?: "") }
    var phone by remember { mutableStateOf(teacher?.phone ?: "") }
    var specialization by remember { mutableStateOf(teacher?.specialization ?: "") }
    var notes by remember { mutableStateOf(teacher?.notes ?: "") }
    var selectedSections by remember {
        mutableStateOf(teacher?.sectionIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList())
    }

    var assignments by remember {
        mutableStateOf<Map<String, Set<String>>>(
            parseAssignments(teacher?.assignments ?: "")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (teacher == null) "إضافة معلم" else "تعديل المعلم") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المعلم *") },
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
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = specialization,
                    onValueChange = { specialization = it },
                    label = { Text("التخصص") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (subjects.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("📚 المواد والصفوف التي يدرّسها:",
                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("اختر المادة، ثم الصفوف التي يدرّسها فيها.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)

                    subjects.forEach { subject ->
                        val selectedClasses = assignments[subject.id] ?: emptySet()
                        val isSelected = selectedClasses.isNotEmpty()

                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            assignments = if (checked) {
                                                assignments + (subject.id to emptySet<String>())
                                            } else {
                                                assignments - subject.id
                                            }
                                        }
                                    )
                                    Text(subject.name, fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp)
                                }

                                if (isSelected) {
                                    Spacer(Modifier.height(6.dp))
                                    FlowRow(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        classes.sortedBy { it.order }.forEach { cls ->
                                            val classSelected = selectedClasses.contains(cls.id)
                                            FilterChip(
                                                selected = classSelected,
                                                onClick = {
                                                    val newSet = if (classSelected)
                                                        selectedClasses - cls.id
                                                    else
                                                        selectedClasses + cls.id
                                                    assignments = assignments + (subject.id to newSet)
                                                },
                                                label = { Text(cls.name, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (sections.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("🧩 الشعب:",
                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    sections.forEach { s ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = s.id in selectedSections,
                                onCheckedChange = { checked ->
                                    selectedSections = if (checked)
                                        selectedSections + s.id
                                    else
                                        selectedSections - s.id
                                }
                            )
                            Text(s.name, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val assignmentsStr = assignments.flatMap { (sid, cids) ->
                        cids.map { cid -> "$sid:$cid" }
                    }.joinToString(",")

                    onSave(Teacher(
                        id = teacher?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        phone = phone.trim(),
                        specialization = specialization.trim(),
                        notes = notes.trim(),
                        sectionIds = selectedSections.joinToString(","),
                        assignments = assignmentsStr
                    ))
                }
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

private fun parseAssignments(raw: String): Map<String, Set<String>> {
    val map = mutableMapOf<String, MutableSet<String>>()
    raw.split(",").forEach { pair ->
        val parts = pair.split(":")
        if (parts.size == 2) {
            val sid = parts[0].trim()
            val cid = parts[1].trim()
            if (sid.isNotBlank() && cid.isNotBlank()) {
                map.getOrPut(sid) { mutableSetOf() }.add(cid)
            }
        }
    }
    return map
}
