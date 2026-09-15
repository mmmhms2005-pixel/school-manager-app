package com.example.schoolmanager

import android.app.Application
import com.example.schoolmanager.data.AppDatabase
import com.example.schoolmanager.data.SchoolClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

            // إضافة الصفوف الـ 12 فقط — بدون شعب افتراضية
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
                        sectionIds = ""  // كل صف يبدأ بدون شعب
                    )
                )
            }
        }
    }
}
