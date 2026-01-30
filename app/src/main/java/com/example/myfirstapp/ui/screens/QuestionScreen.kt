package com.example.myfirstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myfirstapp.data.QuestionRepository

@Composable
fun QuestionListScreen(
    currentPage: Int,
    category: String, // 增加这个参数
    onPageChange: (Int) -> Unit
) {
    val pageSize = 25
    val totalPages = 6
    val allQuestions = QuestionRepository.getQuestionsByCategory(category)
    // 根据页码过滤题目数据
    val questionsOnPage = allQuestions.chunked(pageSize).getOrNull(currentPage - 1) ?: emptyList()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            items(questionsOnPage) { question ->
                ListItem(
                    headlineContent = { Text("第 ${question.id} 题：${question.title}") },
                    supportingContent = { Text("难度：${question.difficulty}  |  通过率：${question.passRate}") },
                    leadingContent = { Text("${question.id}", color = Color(0xFF00C091), fontWeight = FontWeight.Bold) }
                )
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
            }
        }

        // 分页控制
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { onPageChange(currentPage - 1) }, enabled = currentPage > 1) {
                Text("上一页")
            }
            Text("第 $currentPage / $totalPages 页", fontWeight = FontWeight.Bold)
            Button(onClick = { onPageChange(currentPage + 1) }, enabled = currentPage < totalPages) {
                Text("下一页")
            }
        }
    }
}