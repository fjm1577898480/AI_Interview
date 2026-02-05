package com.example.myfirstapp.data.model

data class InterviewPost(
    val id: String,
    val title: String,
    val link: String,
    val category: String,
    val summary: String,
    val content: String,
    val tags: List<String>,
    val update_time: String
)