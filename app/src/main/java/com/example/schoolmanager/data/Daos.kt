package com.example.schoolmanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {

    // Students
    @Query("SELECT * FROM students ORDER BY CAST(number AS INTEGER), name")
    fun students(): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(s: Student)

    @Update
    suspend fun updateStudent(s: Student)

    @Delete
    suspend fun deleteStudent(s: Student)

    // Classes
    @Query("SELECT * FROM school_classes ORDER BY `order`")
    fun classes(): Flow<List<SchoolClass>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(c: SchoolClass)

    @Update
    suspend fun updateClass(c: SchoolClass)

    @Delete
    suspend fun deleteClass(c: SchoolClass)

    // Sections
    @Query("SELECT * FROM sections")
    fun sections(): Flow<List<Section>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(s: Section)

    @Update
    suspend fun updateSection(s: Section)

    @Delete
    suspend fun deleteSection(s: Section)

    // Subjects
    @Query("SELECT * FROM subjects ORDER BY name")
    fun subjects(): Flow<List<Subject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(s: Subject)

    @Update
    suspend fun updateSubject(s: Subject)

    @Delete
    suspend fun deleteSubject(s: Subject)

    // Teachers
    @Query("SELECT * FROM teachers ORDER BY name")
    fun teachers(): Flow<List<Teacher>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(t: Teacher)

    @Update
    suspend fun updateTeacher(t: Teacher)

    @Delete
    suspend fun deleteTeacher(t: Teacher)

    // Grades
    @Query("SELECT * FROM grades")
    fun grades(): Flow<List<Grade>>

    @Query("SELECT * FROM grades WHERE studentId = :sid AND subjectId = :subId AND period = :period LIMIT 1")
    suspend fun findGrade(sid: String, subId: String, period: Int): Grade?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(g: Grade)

    @Update
    suspend fun updateGrade(g: Grade)

    @Delete
    suspend fun deleteGrade(g: Grade)

    // Settings
    @Query("SELECT * FROM school_settings WHERE id = 1")
    fun settings(): Flow<SchoolSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(s: SchoolSettings)
}
