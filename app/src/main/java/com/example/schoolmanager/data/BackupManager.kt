package com.example.schoolmanager.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * إدارة النسخ الاحتياطي والاستعادة.
 * يستخدم JSON لتصدير واستيراد جميع البيانات.
 */
object BackupManager {

    private const val FORMAT = "school-management-backup"
    private const val VERSION = 1

    /**
     * تصدير جميع البيانات إلى ملف JSON.
     */
    suspend fun exportBackup(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val dao = AppDatabase.getDatabase(context).dao()

            val root = JSONObject().apply {
                put("format", FORMAT)
                put("version", VERSION)
                put("createdAt", System.currentTimeMillis())
            }

            // ★ Classes
            val classesArr = JSONArray()
            dao.classes().first().forEach { c ->
                classesArr.put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("order", c.order)
                    put("sectionIds", c.sectionIds)
                    put("homeroomTeachers", c.homeroomTeachers)
                })
            }
            root.put("classes", classesArr)

            // ★ Sections
            val sectionsArr = JSONArray()
            dao.sections().first().forEach { s ->
                sectionsArr.put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                })
            }
            root.put("sections", sectionsArr)

            // ★ Students
            val studentsArr = JSONArray()
            dao.students().first().forEach { s ->
                studentsArr.put(JSONObject().apply {
                    put("id", s.id)
                    put("number", s.number)
                    put("name", s.name)
                    put("classId", s.classId)
                    put("sectionId", s.sectionId)
                    put("guardian", s.guardian)
                    put("phone", s.phone)
                })
            }
            root.put("students", studentsArr)

            // ★ Subjects
            val subjectsArr = JSONArray()
            dao.subjects().first().forEach { s ->
                subjectsArr.put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("code", s.code)
                    put("classIds", s.classIds)
                })
            }
            root.put("subjects", subjectsArr)

            // ★ Teachers
            val teachersArr = JSONArray()
            dao.teachers().first().forEach { t ->
                teachersArr.put(JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                    put("phone", t.phone)
                    put("specialization", t.specialization)
                    put("notes", t.notes)
                    put("sectionIds", t.sectionIds)
                    put("assignments", t.assignments)
                })
            }
            root.put("teachers", teachersArr)

            // ★ Grades
            val gradesArr = JSONArray()
            dao.grades().first().forEach { g ->
                gradesArr.put(JSONObject().apply {
                    put("id", g.id)
                    put("studentId", g.studentId)
                    put("subjectId", g.subjectId)
                    put("period", g.period)
                    put("homework", g.homework)
                    put("oral", g.oral)
                    put("absence", g.absence)
                    put("attendance", g.attendance)
                    put("written", g.written)
                    put("total", g.total)
                })
            }
            root.put("grades", gradesArr)

            // ★ Settings
            val s = dao.settings().first()
            if (s != null) {
                root.put("settings", JSONObject().apply {
                    put("schoolName", s.schoolName)
                    put("academicYear", s.academicYear)
                    put("principalName", s.principalName)
                    put("logoBase64", s.logoBase64)
                })
            }

            // ── كتابة الملف
            context.contentResolver.openOutputStream(uri)?.use { out ->
                OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                    writer.write(root.toString(2))
                }
            } ?: return@withContext Result.failure(Exception("تعذر فتح الملف"))

            val totalRecords = classesArr.length() + sectionsArr.length() +
                    studentsArr.length() + subjectsArr.length() +
                    teachersArr.length() + gradesArr.length()

            Result.success(totalRecords)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * استعادة البيانات من ملف JSON.
     * ⚠️ يحذف البيانات الحالية أولاً!
     */
    suspend fun importBackup(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val dao = AppDatabase.getDatabase(context).dao()

            // ── قراءة الملف
            val text = context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            } ?: return@withContext Result.failure(Exception("تعذر قراءة الملف"))

            val root = JSONObject(text)

            // ── التحقق من الصيغة
            val format = root.optString("format", "")
            if (format != FORMAT) {
                return@withContext Result.failure(Exception("الملف ليس نسخة احتياطية صالحة"))
            }
            val version = root.optInt("version", 0)
            if (version > VERSION) {
                return@withContext Result.failure(Exception("الملف من إصدار أحدث، حدّث التطبيق"))
            }

            // ── حذف البيانات الحالية
            dao.clearAllGrades()
            dao.clearAllTeachers()
            dao.clearAllSubjects()
            dao.clearAllStudents()
            dao.clearAllSections()
            dao.clearAllClasses()
            dao.clearSettings()

            var count = 0

            // ★ Classes
            root.optJSONArray("classes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.insertClass(SchoolClass(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        order = o.optInt("order", 1),
                        sectionIds = o.optString("sectionIds", ""),
                        homeroomTeachers = o.optString("homeroomTeachers", "")
                    ))
                    count++
                }
            }

            // ★ Sections
            root.optJSONArray("sections")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.insertSection(Section(
                        id = o.getString("id"),
                        name = o.getString("name")
                    ))
                    count++
                }
            }

            // ★ Students
            root.optJSONArray("students")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.insertStudent(Student(
                        id = o.getString("id"),
                        number = o.optString("number", ""),
                        name = o.optString("name", ""),
                        classId = o.optString("classId", ""),
                        sectionId = o.optString("sectionId", ""),
                        guardian = o.optString("guardian", ""),
                        phone = o.optString("phone", "")
                    ))
                    count++
                }
            }

            // ★ Subjects
            root.optJSONArray("subjects")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.insertSubject(Subject(
                        id = o.getString("id"),
                        name = o.optString("name", ""),
                        code = o.optString("code", ""),
                        classIds = o.optString("classIds", "")
                    ))
                    count++
                }
            }

            // ★ Teachers
            root.optJSONArray("teachers")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.insertTeacher(Teacher(
                        id = o.getString("id"),
                        name = o.optString("name", ""),
                        phone = o.optString("phone", ""),
                        specialization = o.optString("specialization", ""),
                        notes = o.optString("notes", ""),
                        sectionIds = o.optString("sectionIds", ""),
                        assignments = o.optString("assignments", "")
                    ))
                    count++
                }
            }

            // ★ Grades
            root.optJSONArray("grades")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.insertGrade(Grade(
                        id = o.getString("id"),
                        studentId = o.optString("studentId", ""),
                        subjectId = o.optString("subjectId", ""),
                        period = o.optInt("period", 1),
                        homework = o.optInt("homework", 0),
                        oral = o.optInt("oral", 0),
                        absence = o.optInt("absence", 0),
                        attendance = o.optInt("attendance", 0),
                        written = o.optInt("written", 0),
                        total = o.optInt("total", 0)
                    ))
                    count++
                }
            }

            // ★ Settings
            root.optJSONObject("settings")?.let { o ->
                dao.saveSettings(SchoolSettings(
                    id = 1,
                    schoolName = o.optString("schoolName", ""),
                    academicYear = o.optString("academicYear", ""),
                    principalName = o.optString("principalName", ""),
                    logoBase64 = o.optString("logoBase64", "")
                ))
                count++
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * اسم ملف النسخة المقترح.
     */
    fun suggestFileName(): String {
        val now = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US)
        return "school_backup_${now.format(java.util.Date())}.json"
    }
}
