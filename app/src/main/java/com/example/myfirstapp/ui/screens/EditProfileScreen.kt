package com.example.myfirstapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.myfirstapp.viewmodel.UserViewModel

@Composable
fun EditProfileScreen(viewModel: UserViewModel, onBack: () -> Unit) {
    // 使用 ViewModel 中的持久化字段初始化临时状态
    var tempName by remember { mutableStateOf(viewModel.userName) }
    var tempRealName by remember { mutableStateOf(viewModel.realName) }
    var tempAge by remember { mutableStateOf(viewModel.age) }
    var tempSign by remember { mutableStateOf(viewModel.signature) }
    var tempAvatar by remember { mutableStateOf(viewModel.userAvatarUri) }

    var showCropDialog by remember { mutableStateOf(false) }
    var rawUri by remember { mutableStateOf<Uri?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            rawUri = uri
            showCropDialog = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 头像部分
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F0))
                .clickable { photoLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (tempAvatar != null) {
                AsyncImage(model = tempAvatar, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = Color.LightGray)
            }
        }
        Text("点击更换头像", color = Color(0xFF00C091), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(24.dp))

        // 输入框部分
        EditInputField("账号名", tempName, "超级面试者") { tempName = it }
        EditInputField("真名", tempRealName, "请输入真名") { tempRealName = it }
        EditInputField("年龄", tempAge, "请输入年龄") { tempAge = it }
        EditInputField("个性签名", tempSign, "请输入个性签名") { tempSign = it }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.saveProfile(
                    name = tempName,
                    rName = tempRealName,
                    uAge = tempAge,
                    sign = tempSign,
                    avatar = tempAvatar // 这里传入选好的头像 Uri
                )
                onBack()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C091)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("确认修改并保存", color = Color.White, fontSize = 16.sp)
        }
    }

    if (showCropDialog && rawUri != null) {
        CropDialog(rawUri!!, { showCropDialog = false }, { tempAvatar = it; showCropDialog = false })
    }
}

@Composable
fun CropDialog(uri: Uri, onDismiss: () -> Unit, onConfirm: (Uri) -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = maxOf(1f, scale), scaleY = maxOf(1f, scale), translationX = offset.x, translationY = offset.y)
                    .pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> scale *= zoom; offset += pan } },
                contentScale = ContentScale.Fit
            )
            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
                drawRect(Color.Black.copy(alpha = 0.7f))
                drawCircle(color = Color.Transparent, radius = size.width * 0.4f, center = center, blendMode = BlendMode.Clear)
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color.White.copy(alpha = 0.3f), radius = size.width * 0.4f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
            }
            Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.BottomStart)) { Text("取消", color = Color.White) }
                Button(onClick = { onConfirm(uri) }, modifier = Modifier.align(Alignment.BottomEnd), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C091))) { Text("确定") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInputField(label: String, value: String, hint: String, onValueChange: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold)
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color(0xFF00C091)
            ),
            placeholder = { Text(hint, color = Color.LightGray) },
            singleLine = true
        )
    }
}