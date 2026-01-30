package com.example.myfirstapp.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.myfirstapp.viewmodel.UserViewModel

@Composable
fun HomeContent(viewModel: UserViewModel, onImageClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HomeBanner()
        if (viewModel.resumeUri != null) {
            ImagePreviewCard(viewModel.resumeUri) { onImageClick() }
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("上传简历后即可开启 AI 分析", color = Color.Gray)
            }
        }
    }
}

@Composable
fun HomeBanner() {
    Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(Color(0xFF00BFA5)), contentAlignment = Alignment.Center) {
        Text("AI面试 | 解锁你的新未来", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
    }
}

@Composable
fun ImagePreviewCard(uri: Uri?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp).height(240.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
fun FullScreenImageDialog(uri: Uri, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
        }
    }
}