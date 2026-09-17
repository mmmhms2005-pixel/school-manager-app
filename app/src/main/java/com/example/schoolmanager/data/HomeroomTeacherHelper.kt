package com.example.schoolmanager.data

/**
 * أداة للتعامل مع مربي الصفوف.
 * الصيغة المحفوظة في SchoolClass.homeroomTeachers:
 *   "secId1:tId1,secId2:tId2,_class:tId0"
 * - secId → شعبة محددة
 * - "_class" → مربي الصف كاملاً (يُستخدم إن لم توجد شعب)
 */
object HomeroomTeacherHelper {

    private const val CLASS_WIDE_KEY = "_class"

    /**
     * يعيد اسم مربي الصف بناءً على الصف والشعبة المختارة.
     */
    fun getName(
        classId: String,
        sectionId: String,
        classes: List<SchoolClass>,
        teachers: List<Teacher>
    ): String {
        val cls = classes.find { it.id == classId } ?: return ""
        val map = parse(cls.homeroomTeachers)
        val key = if (sectionId.isNotBlank()) sectionId else CLASS_WIDE_KEY
        val teacherId = map[key] ?: return ""
        return teachers.find { it.id == teacherId }?.name ?: ""
    }

    /** يحوّل النص إلى Map */
    fun parse(raw: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (raw.isBlank()) return map
        raw.split(",").forEach { pair ->
            val parts = pair.split(":")
            if (parts.size >= 2) {
                val k = parts[0].trim()
                val v = parts.subList(1, parts.size).joinToString(":").trim()
                if (k.isNotBlank() && v.isNotBlank()) map[k] = v
            }
        }
        return map
    }

    /** يحوّل Map إلى نص */
    fun serialize(map: Map<String, String>): String {
        return map.entries
            .filter { it.value.isNotBlank() }
            .joinToString(",") { "${it.key}:${it.value}" }
    }

    fun classWideKey(): String = CLASS_WIDE_KEY
}
