package com.example.myfirstapp.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.myfirstapp.data.CompanyData

import androidx.compose.foundation.lazy.rememberLazyListState

// 数据模型：包含拼音首字母用于排序
data class Company(val name: String, val pinyinFirstLetter: Char)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanySearchScreen(onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedIndustry by remember { mutableStateOf<String?>(null) }
    
    // 获取行业和公司数据
    val industries = remember { CompanyData.industries }
    val allCompanies = remember { CompanyData.getCompanyData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搜索公司...", color = Color.Gray) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp), // 移除了固定高度，防止文字被遮挡
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F8F8))
        ) {
            // 行业标签栏
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(industries) { industry ->
                    val isSelected = selectedIndustry == industry
                    FilterChip(
                        selected = isSelected,
                        onClick = { 
                            // 点击已选中的标签则折叠（取消选中），否则选中
                            selectedIndustry = if (isSelected) null else industry 
                        },
                        label = { Text(industry) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE8F5E9),
                            selectedLabelColor = Color(0xFF2E7D32)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color.LightGray,
                            selectedBorderColor = Color(0xFF2E7D32),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        ),
                        shape = CircleShape
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // 公司列表（使用 Crossfade 实现平滑切换，同时解决滚动位置重置问题）
            Crossfade(
                targetState = selectedIndustry,
                label = "CompanyListTransition"
            ) { industry ->
                if (industry == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("请选择上方行业标签查看知名企业", color = Color.Gray)
                    }
                } else {
                    // 为每个行业创建独立的 ListState，保证切换时位置在顶部
                    val listState = rememberLazyListState()
                    
                    // 计算当前行业的显示数据
                    val displayCompanies = remember(industry, searchQuery) {
                        val list = allCompanies[industry] ?: emptyList()
                        if (searchQuery.isNotEmpty()) {
                            list.filter { it.name.contains(searchQuery, ignoreCase = true) }
                                .sortedBy { it.pinyinFirstLetter }
                        } else {
                            list // 已经是排好序的
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        if (displayCompanies.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                    Text("该分类下暂无收录数据", color = Color.Gray)
                                }
                            }
                        } else {
                            // 按首字母分组显示
                            val grouped = displayCompanies.groupBy { it.pinyinFirstLetter }
                            
                            grouped.toSortedMap().forEach { (letter, companies) ->
                                item(key = "header_$letter") {
                                    Text(
                                        text = letter.toString(),
                                        color = Color.Gray,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                                // 添加 key 优化滑动性能
                                items(companies, key = { "${industry}_${it.name}" }) { company ->
                                    CompanyItemCard(company)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompanyItemCard(company: Company) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 模拟公司Logo占位符
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = company.name.take(1),
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = company.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}