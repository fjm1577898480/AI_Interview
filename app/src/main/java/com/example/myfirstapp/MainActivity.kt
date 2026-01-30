package com.example.myfirstapp

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.myfirstapp.ui.screens.*
import com.example.myfirstapp.ui.theme.MyFirstAppTheme
import com.example.myfirstapp.viewmodel.UserViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyFirstAppTheme {
                MainAppContainer(viewModel)
            }
        }
    }
}

@Composable
fun MainAppContainer(viewModel: UserViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentScreen by remember { mutableStateOf("main") }
    var selectedCategory by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(1) }
    var fullScreenImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.saveResumeUri(it) }
    }

    Scaffold(
        bottomBar = {
            if (currentScreen == "main") {
                NavigationBar {
                    tabs.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            label = { Text(label) },
                            icon = { Icon(icons[index], contentDescription = null) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                "main" -> {
                    when (selectedTab) {
                        0 -> HomeContent(viewModel) { fullScreenImageUri = viewModel.resumeUri }
                        1 -> IntentionScreen()
                        2 -> QuestionCategoryScreen { category ->
                            selectedCategory = category
                            currentScreen = "question_list"
                        }
                        3 -> ProfileScreen(
                            viewModel = viewModel,
                            onEditClick = { currentScreen = "edit_profile" },
                            onUploadClick = { photoPickerLauncher.launch("image/*") },
                            onImageClick = { fullScreenImageUri = viewModel.resumeUri }
                        )
                    }
                }
                "edit_profile" -> EditProfileScreen(viewModel) { currentScreen = "main" }
                "question_list" -> {
                    BackHandler { currentScreen = "main" }
                    QuestionListScreen(currentPage, selectedCategory) { currentPage = it }
                }
            }
        }
    }

    fullScreenImageUri?.let { uri ->
        FullScreenImageDialog(uri) { fullScreenImageUri = null }
    }
}

val tabs = listOf("首页", "发展意向", "题库", "我")
val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.Edit, Icons.Default.Person)