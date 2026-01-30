package com.example.myfirstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 定义意向选项的数据结构
data class IntentionOption(val title: String, val icon: ImageVector, val color: Color)

@Composable
fun IntentionScreen() {
    val options = listOf(
        IntentionOption("出国留学", Icons.Default.AirplaneTicket, Color(0xFF64B5F6)),
        IntentionOption("保研考研", Icons.Default.School, Color(0xFFFFB74D)),
        IntentionOption("校招社招", Icons.Default.Work, Color(0xFF81C784)),
        IntentionOption("学校社团", Icons.Default.Groups, Color(0xFFBA68C8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "请选择你的发展意向",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // 使用网格布局，每行 2 个卡片
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(options) { option ->
                IntentionCard(option)
            }
        }
    }
}

@Composable
fun IntentionCard(option: IntentionOption) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { /* 点击逻辑留空 */ },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = option.color,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = option.title,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        }
    }
}