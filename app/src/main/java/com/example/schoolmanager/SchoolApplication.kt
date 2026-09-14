package com.example.schoolmanager

import android.app.Application
import com.example.schoolmanager.data.AppDatabase
import com.example.schoolmanager.data.SchoolClass
import com.example.schoolmanager.data.Section
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class SchoolApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        seedDefaults()
    }

    private fun seedDefaults() {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = database.dao()
            val existingClasses = dao.classes().first()
            if (existingClasses.isNotEmpty()) return@launch

            // إضافة الشعب الأربعة
            val sectionNames = listOf("أ", "ب", "ج", "د")
            val sectionIds = mutableListOf<String>()
            val existingSections = dao.sections().first()
            if (existingSections.isEmpty()) {
                sectionNames.forEach { name ->
                    val id = UUID.randomUUID().toString()
                    sectionIds.add(id)
                    dao.insertSection(Section(id = id, name = name))
                }
            } else {
                existingSections.forEach { sectionIds.add(it.id) }
            }

            val allSections = sectionIds.joinToString(",")

            // إضافة الصفوف الـ 12
            val classNames = listOf(
                "الأول", "الثاني", "الثالث", "الرابع", "الخامس", "السادس",
                "السابع", "الثامن", "التاسع",
                "الأول الثانوي", "الثاني الثانوي", "الثالث الثانوي"
            )
            classNames.forEachIndexed { index, name ->
                dao.insertClass(
                    SchoolClass(
                        id = "class_${index + 1}",
                        name = name,
                        order = index + 1,
                        sectionIds = allSections
                    )
                )
            }
        }
    }
}
