package com.example.schoolmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SchoolClass::class, Section::class, Student::class,
        Subject::class, Teacher::class, Grade::class, SchoolSettings::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): SchoolDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // v1 → v2: إضافة assignments للـ teachers
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE teachers_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        specialization TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        sectionIds TEXT NOT NULL,
                        assignments TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                val cursor = db.query(
                    "SELECT id, name, phone, specialization, notes, subjectIds, classIds, sectionIds FROM teachers"
                )
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val name = cursor.getString(1)
                    val phone = cursor.getString(2)
                    val spec = cursor.getString(3)
                    val notes = cursor.getString(4)
                    val subjects = cursor.getString(5)
                        .split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val classes = cursor.getString(6)
                        .split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val sections = cursor.getString(7)
                    val assignments = mutableListOf<String>()
                    subjects.forEach { s -> classes.forEach { c -> assignments.add("$s:$c") } }
                    db.execSQL(
                        "INSERT INTO teachers_new (id, name, phone, specialization, notes, sectionIds, assignments) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        arrayOf(id, name, phone, spec, notes, sections, assignments.joinToString(","))
                    )
                }
                cursor.close()
                db.execSQL("DROP TABLE teachers")
                db.execSQL("ALTER TABLE teachers_new RENAME TO teachers")
            }
        }

        // v2 → v3: إضافة homeroomTeachers للـ school_classes
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE school_classes ADD COLUMN homeroomTeachers TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "school_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
