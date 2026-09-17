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

    @Query("DELETE FROM students")
    suspend fun clearAllStudents()

    // Classes
    @Query("SELECT * FROM school_classes ORDER BY `order`")
    fun classes(): Flow<List<SchoolClass>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(c: SchoolClass)

    @Update
    suspend fun updateClass(c: SchoolClass)

    @Delete
    suspend fun deleteClass(c: SchoolClass)

    @Query("DELETE FROM school_classes")
    suspend fun clearAllClasses()

    // Sections
    @Query("SELECT * FROM sections")
    fun sections(): Flow<List<Section>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(s: Section)

    @Update
    suspend fun updateSection(s: Section)

    @Delete
    suspend fun deleteSection(s: Section)

    @Query("DELETE FROM sections")
    suspend fun clearAllSections()

    // Subjects
    @Query("SELECT * FROM subjects ORDER BY name")
    fun subjects(): Flow<List<Subject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(s: Subject)

    @Update
    suspend fun updateSubject(s: Subject)

    @Delete
    suspend fun deleteSubject(s: Subject)

    @Query("DELETE FROM subjects")
    suspend fun clearAllSubjects()

    // Teachers
    @Query("SELECT * FROM teachers ORDER BY name")
    fun teachers(): Flow<List<Teacher>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(t: Teacher)

    @Update
    suspend fun updateTeacher(t: Teacher)

    @Delete
    suspend fun deleteTeacher(t: Teacher)

    @Query("DELETE FROM teachers")
    suspend fun clearAllTeachers()

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

    @Query("DELETE FROM grades")
    suspend fun clearAllGrades()

    // Settings
    @Query("SELECT * FROM school_settings WHERE id = 1")
    fun settings(): Flow<SchoolSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(s: SchoolSettings)

    @Query("DELETE FROM school_settings")
    suspend fun clearSettings()
}
