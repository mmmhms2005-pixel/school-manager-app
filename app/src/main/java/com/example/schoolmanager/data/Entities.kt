package com.example.schoolmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "school_classes")
data class SchoolClass(
    @PrimaryKey val id: String,
    val name: String,
    val order: Int,
    val sectionIds: String,
    val homeroomTeachers: String = ""
)

@Entity(tableName = "sections")
data class Section(
    @PrimaryKey val id: String,
    val name: String
)

@Entity(tableName = "students")
data class Student(
    @PrimaryKey val id: String,
    val number: String,
    val name: String,
    val classId: String,
    val sectionId: String,
    val guardian: String,
    val phone: String
)

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey val id: String,
    val name: String,
    val code: String,
    val classIds: String
)

@Entity(tableName = "teachers")
data class Teacher(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val specialization: String,
    val notes: String,
    val sectionIds: String,
    val assignments: String
)

@Entity(tableName = "grades")
data class Grade(
    @PrimaryKey val id: String,
    val studentId: String,
    val subjectId: String,
    val period: Int,
    val homework: Int,
    val oral: Int,
    val absence: Int,
    val attendance: Int,
    val written: Int,
    val total: Int
)

@Entity(tableName = "school_settings")
data class SchoolSettings(
    @PrimaryKey val id: Int = 1,
    val schoolName: String = "",
    val academicYear: String = "",
    val principalName: String = "",
    val logoBase64: String = ""
)
