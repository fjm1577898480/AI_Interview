package com.example.myfirstapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myfirstapp.ui.components.*
import com.example.myfirstapp.viewmodel.UserViewModel

@Composable
fun ProfileScreen(
    viewModel: UserViewModel,
    onEditClick: () -> Unit,
    onImageClick: () -> Unit
) {
    val context = LocalContext.current
    var isAnalyzing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 图片选择器：用于上传或更新简历
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.saveResumeUri(it) }
    }

    // 雷达图数据动画
    val animatedData = viewModel.aiChartData.map { target ->
        animateFloatAsState(
            targetValue = target,
            animationSpec = tween(1000),
            label = "radarAnimation"
        ).value
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 1. 个人信息头部
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clickable { onEditClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.userAvatarUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(viewModel.userAvatarUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = viewModel.userName.ifEmpty { "超级面试者" }, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = viewModel.signature.ifEmpty { "点击修改个人信息" }, fontSize = 12.sp, color = Color.Gray)
            }
        }

        // 2. 简历管理模块
        ProfileSectionCard("我的简历") {
            if (viewModel.resumeUri != null) {
                // 已上传状态：显示预览图和删除/更新按钮
                Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    AsyncImage(
                        model = viewModel.resumeUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick() },
                        contentScale = ContentScale.Crop
                    )

                    // 右上角删除按钮
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(0.5f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                // 更新按钮
                TextButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("更换简历图片", fontSize = 14.sp, color = Color(0xFF00C091))
                }
            } else {
                // 未上传状态：显示上传引导块
                OutlinedButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Text("点击上传简历图片", color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. AI 能力画像（如果没简历，这里显示默认状态）
        ProfileSectionCard("AI能力画像") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("综合竞争力", color = Color.Gray)
                Text(viewModel.aiScore, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = if(viewModel.resumeUri != null) Color(0xFF00C091) else Color.LightGray)
            }
            Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                EnhancedRadarChart(animatedData, Modifier.size(180.dp))
                RadarLabels(viewModel.aiDimensions)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. AI 建议
        ProfileSectionCard("AI建议") {
            Text("适合公司", color = Color(0xFF00C091), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(if(viewModel.aiCompanies.isEmpty()) "上传简历后获取建议" else viewModel.aiCompanies, fontSize = 14.sp)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("岗位差距", color = Color(0xFF00C091), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(if(viewModel.aiGap.isEmpty()) "上传简历后获取分析" else viewModel.aiGap, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. 分析按钮（有图片且没在分析中才可用）
        Button(
            onClick = {
                isAnalyzing = true
                viewModel.startResumeAnalysis(context) { isAnalyzing = false }
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            enabled = viewModel.resumeUri != null && !isAnalyzing,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C091))
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("开始 AI 简历分析")
            }
        }
        Spacer(modifier = Modifier.height(50.dp))
    }

    // 确认删除弹窗
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除简历？") },
            text = { Text("删除后简历图片及相关的AI评分、建议将全部被清除。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteResume()
                    showDeleteDialog = false
                }) {
                    Text("确认删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}