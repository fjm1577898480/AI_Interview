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
fun IntentionScreen(onNavigate: (String) -> Unit) {
    val options = listOf(
        IntentionOption("出国留学", Icons.Default.AirplaneTicket, Color(0xFF64B5F6)),
        IntentionOption("保研考研", Icons.Default.School, Color(0xFFFFB74D)),
        IntentionOption("校招社招", Icons.Default.Work, Color(0xFF81C784)),
        IntentionOption("学校社团", Icons.Default.Groups, Color(0xFFBA68C8))
    )

    // Column 意味着把里面的组件按列排布
    Column(

        // 修饰Column容器
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

        // 使用网格布局（懒加载），不会一开始就全部画出内部组件，而是先画出当前屏幕里面那几个，划走一个再画一个
        LazyVerticalGrid(
            // 固定只有两列
            columns = GridCells.Fixed(2),

            // 水平间距12
            horizontalArrangement = Arrangement.spacedBy(12.dp),

            // 垂直间距12
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // 类比java中的for循环，遍历options数组，将每一个元素替换到IntentionCard()的括号中
            items(options) { option ->
                IntentionCard(option, onClick = {
                    if (option.title == "校招社招") {
                        onNavigate("company_search")
                    }
                })
            }
        }
    }
}

@Composable

// 类比java方法内的参数，变量名：数据类型
fun IntentionCard(option: IntentionOption, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },

        // 圆角边框
        shape = RoundedCornerShape(12.dp),

        // 默认卡片颜色
        colors = CardDefaults.cardColors(containerColor = Color.White),

        // 设置阴影
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                // 使用矢量图形，边界永远不会出现马赛克
                imageVector = option.icon,

                // 为视力障碍用户设计，点击图标会读出此内容
                contentDescription = null,

                // 图标颜色
                tint = option.color,

                // 设置宽度与高度
                modifier = Modifier.size(40.dp)
            )

            // 间隔标签与文本
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = option.title,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        }
    }
}