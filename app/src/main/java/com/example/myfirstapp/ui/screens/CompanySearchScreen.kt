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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.myfirstapp.data.model.Company
import com.example.myfirstapp.data.model.CompanyListItem
import com.example.myfirstapp.ui.viewmodels.CompanySearchViewModel

import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanySearchScreen(
    onBack: (() -> Unit)? = null,
    showSearchBar: Boolean = true,
    viewModel: CompanySearchViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedIndustry by viewModel.selectedIndustry.collectAsState()
    val uiList by viewModel.uiList.collectAsState()

    // 获取行业数据
    val industries = remember { CompanyData.industries }

    Scaffold(
        topBar = {
            if (showSearchBar) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("搜索公司...", color = Color.Gray) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            },
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
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
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
                            viewModel.onIndustrySelected(if (isSelected) null else industry)
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
                // UI 逻辑大大简化，只负责根据 viewModel 提供的数据进行渲染

                if (industry == null && searchQuery.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("请选择上方行业标签查看知名企业", color = Color.Gray)
                    }
                } else if (uiList.isEmpty() && searchQuery.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("未找到相关企业", color = Color.Gray)
                    }
                } else if (uiList.isEmpty() && industry != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("该分类下暂无收录数据", color = Color.Gray)
                    }
                } else {
                    // 使用 key(industry) 强制重新创建 LazyListState 和 LazyColumn
                    // 这样可以彻底避免"旧滚动位置应用到新列表"导致的闪烁问题
                    key(industry) {
                        val listState = rememberLazyListState()

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(
                                items = uiList,
                                key = {
                                    when (it) {
                                        is CompanyListItem.Header -> it.key
                                        is CompanyListItem.Item -> it.key
                                    }
                                },
                                contentType = {
                                    when (it) {
                                        is CompanyListItem.Header -> "header"
                                        is CompanyListItem.Item -> "company"
                                    }
                                }
                            ) { item ->
                                when (item) {
                                    is CompanyListItem.Header -> {
                                        Text(
                                            text = item.title,
                                            color = if (industry == null) Color.Black else Color.Gray,
                                            fontSize = if (industry == null) 16.sp else 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .padding(
                                                    vertical = if (industry == null) 12.dp else 8.dp,
                                                    horizontal = 4.dp
                                                )
                                                .let {
                                                    if (industry == null) it.background(
                                                        Color(
                                                            0xFFF8F8F8
                                                        )
                                                    ).fillMaxWidth() else it
                                                }
                                        )
                                    }

                                    is CompanyListItem.Item -> {
                                        CompanyItemCard(item.company)
                                    }
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