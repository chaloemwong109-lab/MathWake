package com.example.mathwake.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val isActive: Boolean = true,
    val difficulty: MathDifficulty = MathDifficulty.EASY,
    val repeatDays: String = "" // e.g. "1,2,3,4,5" for Mon-Fri
)

enum class MathDifficulty {
    EASY, MEDIUM, HARD
}