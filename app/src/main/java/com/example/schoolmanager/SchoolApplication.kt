package com.example.schoolmanager

import android.app.Application
import com.example.schoolmanager.data.AppDatabase

class SchoolApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
