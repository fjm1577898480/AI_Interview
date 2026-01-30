package com.example.myfirstapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
// 确保导入了刚才建立的组件
import com.example.myfirstapp.ui.components.IntentionCard

@Composable
fun QuestionCategoryScreen(onCategoryClick: (String) -> Unit) {
    val categories = listOf("出国留学", "保研考研", "校招社招", "学校社团")
    val icons = listOf(Icons.Default.AirplaneTicket, Icons.Default.School, Icons.Default.Work, Icons.Default.Groups)
    val colors = listOf(Color(0xFF64B5F6), Color(0xFFFFB74D), Color(0xFF81C784), Color(0xFFBA68C8))

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("选择专业题库", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories.size) { index ->
                IntentionCard(
                    title = categories[index],   // 参数名要对准
                    icon = icons[index],
                    color = colors[index],       // 参数名要对准
                    onClick = { onCategoryClick(categories[index]) }
                )
            }
        }
    }
}