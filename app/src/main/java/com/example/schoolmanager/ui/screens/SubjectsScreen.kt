package com.example.schoolmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.schoolmanager.SchoolApplication
import com.example.schoolmanager.data.SchoolClass
import com.example.schoolmanager.data.Subject
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SchoolApplication
    val dao = app.database.dao()
    val scope = rememberCoroutineScope()

    val subjects by dao.subjects().collectAsState(initial = emptyList())
    val classes by dao.classes().collectAsState(initial = emptyList())

    var search by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Subject?>(null) }
    var deleting by remember { mutableStateOf<Subject?>(null) }

    val filtered = subjects.filter {
        search.isBlank() ||
        it.name.contains(search, true) ||
        it.code.contains(search, true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المواد الدراسية", fontWeight = FontWeight.Bold) },
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
            FloatingActionButton(
                onClick = { editing = null; showDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة مادة")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("ابحث باسم المادة أو الرمز") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📚", fontSize = 56.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("لا توجد مواد", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("اضغط على زر الإضافة لإضافة مادة جديدة",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filtered, key = { it.id }) { subject ->
                        SubjectCard(
                            subject = subject,
                            className = subject.classIds
                                .split(",")
                                .filter { it.isNotBlank() }
                                .mapNotNull { cid -> classes.find { it.id == cid }?.name }
                                .joinToString("، "),
                            onEdit = { editing = subject; showDialog = true },
                            onDelete = { deleting = subject }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        SubjectDialog(
            subject = editing,
            classes = classes,
            onDismiss = { showDialog = false },
            onSave = { s ->
                scope.launch {
                    if (editing == null) dao.insertSubject(s)
                    else dao.updateSubject(s)
                }
                showDialog = false
            }
        )
    }

    deleting?.let { s ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل تريد حذف المادة ${s.name}؟") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.deleteSubject(s) }
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
fun SubjectCard(
    subject: Subject,
    className: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(subject.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (subject.code.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("الرمز: ${subject.code}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary)
                }
                if (className.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("الصفوف: $className", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary)
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
fun SubjectDialog(
    subject: Subject?,
    classes: List<SchoolClass>,
    onDismiss: () -> Unit,
    onSave: (Subject) -> Unit
) {
    var name by remember { mutableStateOf(subject?.name ?: "") }
    var code by remember { mutableStateOf(subject?.code ?: "") }
    var selectedClasses by remember {
        mutableStateOf(subject?.classIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (subject == null) "إضافة مادة" else "تعديل المادة") },
        text = {
            Column(Modifier.fillMaxWidth().padding(4.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المادة *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("رمز المادة (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Text("الصفوف التي تُدرّس فيها:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text("اتركها فارغة لتدريس المادة في جميع الصفوف.",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(6.dp))

                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(classes.sortedBy { it.order }, key = { it.id }) { c ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = c.id in selectedClasses,
                                onCheckedChange = { checked ->
                                    selectedClasses = if (checked)
                                        selectedClasses + c.id
                                    else
                                        selectedClasses - c.id
                                }
                            )
                            Text(c.name, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(Subject(
                        id = subject?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        code = code.trim(),
                        classIds = selectedClasses.joinToString(",")
                    ))
                }
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
