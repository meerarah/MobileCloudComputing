package com.university.smartcampuspantry.model

data class StudentProfile(
    val success: Boolean = false,
    val studentId: String = "",
    val name: String = "",
    val eligible: Boolean = false,
    val impactPoints: Int = 0,
    val claimsThisWeek: Int = 0,
    val maxWeeklyClaims: Int = 3,
    val message: String? = null
)
