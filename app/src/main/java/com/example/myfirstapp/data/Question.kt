package com.example.myfirstapp.data

data class Question(
    val id: Int,
    val title: String,
    val answer: String,
    val category: String,
    val difficulty: String = "中等",
    val passRate: String = "65%"
)

object QuestionRepository {
    fun getQuestionsByCategory(category: String): List<Question> {
        return (1..150).map {
            Question(id = it, title = "[$category] 第 $it 题", answer = "", category = category, passRate = "${(60..95).random()}%")
        }
    }
}