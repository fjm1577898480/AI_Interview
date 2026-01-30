package com.example.myfirstapp.ui.screens

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
fun ProfileScreen(viewModel: UserViewModel, onEditClick: () -> Unit, onUploadClick: () -> Unit, onImageClick: () -> Unit) {
    val context = LocalContext.current
    var isAnalyzing by remember { mutableStateOf(false) }

    val animatedData = viewModel.aiChartData.map { target ->
        animateFloatAsState(targetValue = target, animationSpec = tween(1000), label = "").value
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F8F8)).verticalScroll(rememberScrollState()).padding(16.dp)) {
        // 1. 头部
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).clickable { onEditClick() }, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFE0E0E0)), contentAlignment = Alignment.Center) {
                if (viewModel.userAvatarUri != null) {
                    AsyncImage(model = ImageRequest.Builder(context).data(viewModel.userAvatarUri).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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

        // 2. 简历
        // 找到 ProfileSectionCard("我的简历") 这一块，替换为以下代码
        ProfileSectionCard("我的简历") {
            Column {
                if (viewModel.resumeUri != null) {
                    // 简历图片容器
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick() } // 点击图片：放大预览
                    ) {
                        AsyncImage(
                            model = viewModel.resumeUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // 右下角悬浮提示（可选，增加提示感）
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "点击放大",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 专门的更新按钮
                    TextButton(
                        onClick = { onUploadClick() }, // 点击按钮：触发上传
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("更新简历图片", fontSize = 14.sp, color = Color(0xFF00C091))
                    }
                } else {
                    // 还没有简历时的占位状态
                    OutlinedButton(
                        onClick = { onUploadClick() },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
                            Text("上传简历图片开启 AI 分析", color = Color.Gray)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. AI 画像
        ProfileSectionCard("AI能力画像") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("综合竞争力", color = Color.Gray)
                Text(viewModel.aiScore, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00C091))
            }
            Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                EnhancedRadarChart(animatedData, Modifier.size(180.dp))
                RadarLabels(viewModel.aiDimensions)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 建议
        ProfileSectionCard("AI建议") {
            Text("适合公司", color = Color(0xFF00C091), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(viewModel.aiCompanies, fontSize = 14.sp)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("岗位差距", color = Color(0xFF00C091), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(viewModel.aiGap, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { isAnalyzing = true; viewModel.startResumeAnalysis(context) { isAnalyzing = false } },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            enabled = viewModel.resumeUri != null && !isAnalyzing,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C091))
        ) {
            if (isAnalyzing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            else Text("开始 AI 简历分析")
        }
    }
}