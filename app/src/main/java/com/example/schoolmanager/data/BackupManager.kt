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
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * إدارة النسخ الاحتياطي والاستعادة.
 * يدعم النسخ العادي (JSON) والمشفّر (AES-256).
 */
object BackupManager {

    private const val FORMAT = "school-management-backup"
    private const val VERSION = 1
    private const val ENCRYPTED_PREFIX = "ENCRYPTED:v1:"

    // ═══════════════════════════════════
    // التصدير
    // ═══════════════════════════════════

    suspend fun exportBackup(
        context: Context,
        uri: Uri,
        password: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val dao = AppDatabase.getDatabase(context).dao()
            val root = buildJsonObject(dao)
            val json = root.toString(2)

            val outputText = if (password.isNullOrBlank()) {
                json
            } else {
                ENCRYPTED_PREFIX + encrypt(json, password)
            }

            context.contentResolver.openOutputStream(uri)?.use { out ->
                OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                    writer.write(outputText)
                }
            } ?: return@withContext Result.failure(Exception("تعذر فتح الملف"))

            val count = countRecords(root)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════
    // الاستيراد
    // ═══════════════════════════════════

    suspend fun importBackup(
        context: Context,
        uri: Uri,
        password: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val dao = AppDatabase.getDatabase(context).dao()

            val rawText = context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            } ?: return@withContext Result.failure(Exception("تعذر قراءة الملف"))

            val json: String = if (rawText.startsWith(ENCRYPTED_PREFIX)) {
                if (password.isNullOrBlank()) {
                    return@withContext Result.failure(
                        Exception("الملف مشفّر — يجب إدخال كلمة المرور")
                    )
                }
                try {
                    decrypt(rawText.removePrefix(ENCRYPTED_PREFIX), password)
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception("كلمة المرور غير صحيحة"))
                }
            } else {
                rawText
            }

            val root = JSONObject(json)
            val format = root.optString("format", "")
            if (format != FORMAT) {
                return@withContext Result.failure(Exception("الملف ليس نسخة احتياطية صالحة"))
            }
            val version = root.optInt("version", 0)
            if (version > VERSION) {
                return@withContext Result.failure(Exception("الملف من إصدار أحدث"))
            }

            dao.clearAllGrades()
            dao.clearAllTeachers()
            dao.clearAllSubjects()
            dao.clearAllStudents()
            dao.clearAllSections()
            dao.clearAllClasses()
            dao.clearSettings()

            var count = 0

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
            root.optJSONArray("sections")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.insertSection(Section(id = o.getString("id"), name = o.getString("name")))
                    count++
                }
            }
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

    // ═══════════════════════════════════
    // أدوات مساعدة
    // ═══════════════════════════════════

    fun suggestFileName(): String {
        val now = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US)
        return "school_backup_${now.format(java.util.Date())}.json"
    }

    private suspend fun buildJsonObject(dao: SchoolDao): JSONObject {
        val root = JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("createdAt", System.currentTimeMillis())
        }
        val classesArr = JSONArray()
        dao.classes().first().forEach { c ->
            classesArr.put(JSONObject().apply {
                put("id", c.id); put("name", c.name); put("order", c.order)
                put("sectionIds", c.sectionIds); put("homeroomTeachers", c.homeroomTeachers)
            })
        }
        root.put("classes", classesArr)

        val sectionsArr = JSONArray()
        dao.sections().first().forEach { s ->
            sectionsArr.put(JSONObject().apply { put("id", s.id); put("name", s.name) })
        }
        root.put("sections", sectionsArr)

        val studentsArr = JSONArray()
dao.students().first().forEach { s ->
    studentsArr.put(JSONObject().apply {
        put("id", s.id); put("nu...")   // ← الأسطر الموجودة
        put("classId", s.classId...)
        put("guardian", s.guard...)
        put("photoBase64", s.photoBase64)   // ★★★ السطر الجديد ★★★
    })
}
root.put("students", studentsArr)

        val subjectsArr = JSONArray()
        dao.subjects().first().forEach { s ->
            subjectsArr.put(JSONObject().apply {
                put("id", s.id); put("name", s.name); put("code", s.code); put("classIds", s.classIds)
            })
        }
        root.put("subjects", subjectsArr)

        val teachersArr = JSONArray()
        dao.teachers().first().forEach { t ->
            teachersArr.put(JSONObject().apply {
                put("id", t.id); put("name", t.name); put("phone", t.phone)
                put("specialization", t.specialization); put("notes", t.notes)
                put("sectionIds", t.sectionIds); put("assignments", t.assignments)
            })
        }
        root.put("teachers", teachersArr)

        val gradesArr = JSONArray()
        dao.grades().first().forEach { g ->
            gradesArr.put(JSONObject().apply {
                put("id", g.id); put("studentId", g.studentId); put("subjectId", g.subjectId)
                put("period", g.period); put("homework", g.homework); put("oral", g.oral)
                put("absence", g.absence); put("attendance", g.attendance)
                put("written", g.written); put("total", g.total)
            })
        }
        root.put("grades", gradesArr)

        dao.settings().first()?.let { s ->
            root.put("settings", JSONObject().apply {
                put("schoolName", s.schoolName); put("academicYear", s.academicYear)
                put("principalName", s.principalName); put("logoBase64", s.logoBase64)
            })
        }
        return root
    }

    private fun countRecords(root: JSONObject): Int {
        var c = 0
        listOf("classes", "sections", "students", "subjects", "teachers", "grades").forEach { k ->
            c += root.optJSONArray(k)?.length() ?: 0
        }
        if (root.optJSONObject("settings") != null) c++
        return c
    }

    // ═══════════════════════════════════
    // التشفير AES-256
    // ═══════════════════════════════════

    private fun encrypt(plainText: String, password: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val b64Salt = Base64.encodeToString(salt, Base64.NO_WRAP)
        val b64Iv = Base64.encodeToString(iv, Base64.NO_WRAP)
        val b64Data = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        return "$b64Salt:$b64Iv:$b64Data"
    }

    private fun decrypt(payload: String, password: String): String {
        val parts = payload.split(":")
        if (parts.size != 3) throw IllegalArgumentException("صيغة مشفّرة غير صحيحة")
        val salt = Base64.decode(parts[0], Base64.NO_WRAP)
        val iv = Base64.decode(parts[1], Base64.NO_WRAP)
        val data = Base64.decode(parts[2], Base64.NO_WRAP)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val decrypted = cipher.doFinal(data)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}
