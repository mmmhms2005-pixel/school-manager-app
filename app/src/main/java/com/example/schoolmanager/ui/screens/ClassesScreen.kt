package com.example.schoolmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.schoolmanager.data.HomeroomTeacherHelper
import com.example.schoolmanager.data.SchoolClass
import com.example.schoolmanager.data.Section
import com.example.schoolmanager.data.Teacher
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassesScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SchoolApplication
    val dao = app.database.dao()
    val scope = rememberCoroutineScope()

    val classes by dao.classes().collectAsState(initial = emptyList())
    val sections by dao.sections().collectAsState(initial = emptyList())
    val teachers by dao.teachers().collectAsState(initial = emptyList())
    val students by dao.students().collectAsState(initial = emptyList())

    var showClassDialog by remember { mutableStateOf(false) }
    var showSectionsDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SchoolClass?>(null) }
    var deleting by remember { mutableStateOf<SchoolClass?>(null) }
    var newSectionName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الصفوف والشعب", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showSectionsDialog = true }) {
                        Text("📋", fontSize = 20.sp)
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
            ExtendedFloatingActionButton(
                onClick = {
                    if (teachers.isEmpty()) {
                        scope.launch {
                            // سيُنبَّه المستخدم داخل النافذة
                        }
                    }
                    editing = null
                    showClassDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة صف") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Card(
                Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    "📌 اضغط 📋 في الأعلى لإضافة أو حذف الشعب. ثم حدد لكل صف شعبه ومربيها (إجباري).",
                    Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp, 0.dp, 12.dp, 90.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(classes.sortedBy { it.order }, key = { it.id }) { cls ->
                    ClassCard(
                        cls = cls,
                        sections = sections,
                        teachers = teachers,
                        studentCount = students.count { it.classId == cls.id },
                        onEdit = { editing = cls; showClassDialog = true },
                        onDelete = { deleting = cls }
                    )
                }
            }
        }
    }

    if (showClassDialog) {
        ClassDialog(
            cls = editing,
            sections = sections,
            teachers = teachers,
            onDismiss = { showClassDialog = false },
            onSave = { c ->
                scope.launch {
                    if (editing == null) dao.insertClass(c)
                    else dao.updateClass(c)
                }
                showClassDialog = false
            }
        )
    }

    if (showSectionsDialog) {
        AlertDialog(
            onDismissRequest = { showSectionsDialog = false },
            title = { Text("إدارة الشعب") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text("أضف الشعب التي تحتاجها مدرستك.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newSectionName,
                            onValueChange = { newSectionName = it },
                            label = { Text("اسم الشعبة") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val n = newSectionName.trim()
                                if (n.isNotBlank() && sections.none { it.name == n }) {
                                    scope.launch {
                                        dao.insertSection(Section(id = UUID.randomUUID().toString(), name = n))
                                    }
                                    newSectionName = ""
                                }
                            },
                            enabled = newSectionName.isNotBlank() && sections.none { it.name == newSectionName.trim() }
                        ) { Text("إضافة") }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("الشعب الحالية (${sections.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))

                    if (sections.isEmpty()) {
                        Text("لا توجد شعب بعد.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    } else {
                        sections.forEach { s ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(s.name, Modifier.weight(1f), fontSize = 14.sp)
                                IconButton(onClick = {
                                    scope.launch { dao.deleteSection(s) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف",
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSectionsDialog = false }) { Text("إغلاق") }
            }
        )
    }

    deleting?.let { c ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل تريد حذف الصف ${c.name}؟") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.deleteClass(c) }
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
fun ClassCard(
    cls: SchoolClass,
    sections: List<Section>,
    teachers: List<Teacher>,
    studentCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val allowedSections = cls.sectionIds.split(",").filter { it.isNotBlank() }
    val classSections = sections.filter { it.id in allowedSections }
    val homeroomMap = remember(cls.homeroomTeachers) {
        HomeroomTeacherHelper.parse(cls.homeroomTeachers)
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(cls.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text("$studentCount طالب", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(8.dp))

            if (classSections.isEmpty()) {
                val classWideT = homeroomMap[HomeroomTeacherHelper.classWideKey()]
                val tName = teachers.find { it.id == classWideT }?.name
                Text(
                    if (tName != null) "مربي الصف: $tName" else "مربي الصف: غير محدد",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                classSections.forEach { s ->
                    val tId = homeroomMap[s.id]
                    val tName = teachers.find { it.id == tId }?.name
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(s.name, Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (tName != null) "← $tName" else "← غير محدد",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ترتيب: ${cls.order}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDialog(
    cls: SchoolClass?,
    sections: List<Section>,
    teachers: List<Teacher>,
    onDismiss: () -> Unit,
    onSave: (SchoolClass) -> Unit
) {
    var name by remember { mutableStateOf(cls?.name ?: "") }
    var order by remember { mutableStateOf((cls?.order ?: 1).toString()) }
    var selectedSections by remember {
        mutableStateOf(cls?.sectionIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList())
    }
    var homeroomMap by remember {
        mutableStateOf<Map<String, String>>(
            HomeroomTeacherHelper.parse(cls?.homeroomTeachers ?: "")
        )
    }

    // ★ التحقق من الإجبارية
    val hasTeachers = teachers.isNotEmpty()
    val allSectionsHaveTeacher = if (selectedSections.isEmpty()) {
        // بلا شعب: يجب تحديد مربي الصف كاملاً
        !homeroomMap[HomeroomTeacherHelper.classWideKey()].isNullOrBlank()
    } else {
        // مع شعب: كل شعبة يجب أن يكون لها مربي
        selectedSections.all { sid ->
            !homeroomMap[sid].isNullOrBlank()
        }
    }
    val canSave = name.isNotBlank() && order.isNotBlank() && hasTeachers && allSectionsHaveTeacher

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (cls == null) "إضافة صف" else "تعديل الصف") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                if (!hasTeachers) {
                    Card(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            "⚠️ لا يوجد معلمون. أضف معلمين أولاً من شاشة المعلمين.",
                            Modifier.padding(10.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الصف *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = order,
                    onValueChange = { order = it.filter { c -> c.isDigit() } },
                    label = { Text("ترتيب الصف *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(Modifier.height(12.dp))
                Text("الشعب المتاحة في هذا الصف:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))

                if (sections.isEmpty()) {
                    Text("لا توجد شعب — أضف من أيقونة 📋",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(12.dp))
                } else {
                    sections.forEach { s ->
                        val isChecked = s.id in selectedSections
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedSections = if (checked)
                                            selectedSections + s.id
                                        else
                                            selectedSections - s.id
                                    }
                                )
                                Text(s.name, fontSize = 14.sp)
                            }
                            if (isChecked) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "مربي شعبة ${s.name} *",
                                    fontSize = 11.sp,
                                    color = if (homeroomMap[s.id].isNullOrBlank())
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                TeacherDropdown(
                                    teachers = teachers,
                                    selectedId = homeroomMap[s.id] ?: "",
                                    onSelect = { tid ->
                                        homeroomMap = homeroomMap + (s.id to tid)
                                    },
                                    label = "اختر المربي"
                                )
                            }
                        }
                    }
                }

                if (selectedSections.isEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("مربي الصف (للصف كاملاً) *:",
                        fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        color = if (homeroomMap[HomeroomTeacherHelper.classWideKey()].isNullOrBlank())
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    TeacherDropdown(
                        teachers = teachers,
                        selectedId = homeroomMap[HomeroomTeacherHelper.classWideKey()] ?: "",
                        onSelect = { tid ->
                            homeroomMap = homeroomMap + (HomeroomTeacherHelper.classWideKey() to tid)
                        },
                        label = "اختر مربي الصف"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    // تنظيف: حذف المربين للشعب غير المحددة
                    val cleanedMap = homeroomMap.filterKeys { key ->
                        key == HomeroomTeacherHelper.classWideKey() || key in selectedSections
                    }
                    onSave(SchoolClass(
                        id = cls?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        order = order.toIntOrNull() ?: 1,
                        sectionIds = selectedSections.joinToString(","),
                        homeroomTeachers = HomeroomTeacherHelper.serialize(cleanedMap)
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
fun TeacherDropdown(
    teachers: List<Teacher>,
    selectedId: String,
    onSelect: (String) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = teachers.find { it.id == selectedId }?.name

    Box(Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedName ?: label,
                onValueChange = {}, readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                singleLine = true,
                isError = selectedId.isBlank()
            )
            ExposedDropdownMenu(expanded, { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("— بدون —") },
                    onClick = { onSelect(""); expanded = false }
                )
                if (teachers.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("لا يوجد معلمون") },
                        onClick = { expanded = false }
                    )
                }
                teachers.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(t.name) },
                        onClick = { onSelect(t.id); expanded = false }
                    )
                }
            }
        }
    }
}
