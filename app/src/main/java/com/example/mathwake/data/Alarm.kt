package com.example.mathwake.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isActive: Boolean = true,
    val difficulty: MathDifficulty = MathDifficulty.EASY,
    val repeatDays: String = ""
)

enum class MathDifficulty {
    EASY, MEDIUM, HARD
}