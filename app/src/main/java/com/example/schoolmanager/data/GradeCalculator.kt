package com.example.schoolmanager.data

/**
 * نفس قواعد الحساب في التطبيق الأصلي:
 * - الفترات 1,2,3,5,6,7 (شهرية): الواجبات/20 + الشفهي/20 + المواظبة/20 + التحريري/40 ÷ 5
 * - الفترات 4 و 8 (امتحانات): التحريري فقط /30
 * - المواظبة = 20 - الغياب
 */
object GradeCalculator {

    val PERIODS = listOf(
        1 to "الشهر الأول", 2 to "الشهر الثاني", 3 to "الشهر الثالث",
        4 to "نصف العام", 5 to "الشهر الرابع", 6 to "الشهر الخامس",
        7 to "الشهر السادس", 8 to "نهاية العام"
    )

    fun isExam(period: Int) = period == 4 || period == 8

    fun periodName(period: Int) = PERIODS.firstOrNull { it.first == period }?.second ?: ""

    fun maxFor(period: Int) = if (isExam(period)) 30 else 20

    fun compute(
        period: Int,
        homework: Int, oral: Int, absence: Int, written: Int
    ): Grade {
        return if (isExam(period)) {
            val w = written.coerceIn(0, 30)
            Grade(
                id = "", studentId = "", subjectId = "", period = period,
                homework = 0, oral = 0, absence = 0, attendance = 0,
                written = w, total = w
            )
        } else {
            val h = homework.coerceIn(0, 20)
            val o = oral.coerceIn(0, 20)
            val ab = absence.coerceIn(0, 20)
            val at = (20 - ab).coerceAtLeast(0)
            val w = written.coerceIn(0, 40)
            val total = Math.floor((h + o + at + w) / 5.0 + 0.5).toInt()
            Grade(
                id = "", studentId = "", subjectId = "", period = period,
                homework = h, oral = o, absence = ab, attendance = at,
                written = w, total = total
            )
        }
    }
}
