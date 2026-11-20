package com.example.mathwake.logic

import com.example.mathwake.data.MathDifficulty
import kotlin.random.Random

data class MathProblem(
    val question: String,
    val answer: Int
)

object MathProblemGenerator {
    fun generateProblem(difficulty: MathDifficulty): MathProblem {
        return when (difficulty) {
            MathDifficulty.EASY -> generateEasy()
            MathDifficulty.MEDIUM -> generateMedium()
            MathDifficulty.HARD -> generateHard()
        }
    }

    private fun generateEasy(): MathProblem {
        // 1-digit addition or subtraction (positive answer)
        val isAddition = Random.nextBoolean()
        val a = Random.nextInt(1, 10)
        val b = Random.nextInt(1, 10)
        
        return if (isAddition) {
            MathProblem("$a + $b = ?", a + b)
        } else {
            val max = maxOf(a, b)
            val min = minOf(a, b)
            MathProblem("$max - $min = ?", max - min)
        }
    }

    private fun generateMedium(): MathProblem {
        // 2-digit addition or subtraction
        val isAddition = Random.nextBoolean()
        val a = Random.nextInt(10, 100)
        val b = Random.nextInt(10, 100)

        return if (isAddition) {
            MathProblem("$a + $b = ?", a + b)
        } else {
            val max = maxOf(a, b)
            val min = minOf(a, b)
            MathProblem("$max - $min = ?", max - min)
        }
    }

    private fun generateHard(): MathProblem {
        // Mixed: 2-digit multiplication, division (whole numbers) or complex addition
        val op = Random.nextInt(0, 3)
        return when (op) {
            0 -> { // Multiplication 1-digit by 2-digit
                val a = Random.nextInt(2, 10)
                val b = Random.nextInt(11, 30)
                MathProblem("$a × $b = ?", a * b)
            }
            1 -> { // Division (ensure whole number)
                val b = Random.nextInt(2, 15)
                val answer = Random.nextInt(5, 20)
                val a = answer * b
                MathProblem("$a ÷ $b = ?", answer)
            }
            else -> { // Complex addition 3 items
                val a = Random.nextInt(10, 50)
                val b = Random.nextInt(10, 50)
                val c = Random.nextInt(10, 50)
                MathProblem("$a + $b + $c = ?", a + b + c)
            }
        }
    }
}