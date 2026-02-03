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


// 类：类（）是kotlin中继承父类的一种写法（必须要写括号），意味着继承父类的时候，调用父类的空参构造方法完成自己类的内部的初始化。
  // 类：类是kotlin中实现接口的写法

// MainActivity这个类本身就封装了main函数，所以明面上看不到main函数
class MainActivity : ComponentActivity() {

    // by viewModels：意味着将viewModel与Activity的生命周期绑定
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

    // 将先前的数据放在内存中，等下一次函数调用的时候，可以获取到上一次的值，不至于被重置为0
    var selectedTab by remember {

        // 将0放入了一个可记忆的整型容器
        mutableIntStateOf(0) }

    var currentScreen by remember {

        // 这个容器可以几乎放下所有数据类型
        mutableStateOf("main") }

    var selectedCategory by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(1) }
    var fullScreenImageUri by remember { mutableStateOf<Uri?>(null) }


    // launcher：拉起相册和获取被选择图片的工具
    // remember：记忆并保持这个launcher工具，不然拉起相册后这个工具就没了
    // ForActivityResult：针对这个返回的图片结果而设计
    val photoPickerLauncher = rememberLauncherForActivityResult(

        // 定义一个契约，用来拉起相册
        contract = ActivityResultContracts.GetContent()
    ) {

        // 传进来了一个Uri对象，也可能为空
        uri: Uri? ->

        // 如果不为空，则执行{}内的逻辑
        uri?.let { viewModel.saveResumeUri(it) }
    }

    Scaffold(
        bottomBar = {
            if (currentScreen == "main") {

                // 底部容器
                NavigationBar {
                    tabs.forEachIndexed { index, label ->

                        // 底部容器中每一个导航模块部分
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
                        1 -> IntentionScreen { route -> currentScreen = route }
                        2 -> QuestionCategoryScreen { category ->
                            selectedCategory = category
                            currentScreen = "question_list"
                        }
                        3 -> ProfileScreen(
                            viewModel = viewModel,
                            onEditClick = { currentScreen = "edit_profile" },
                            onImageClick = { fullScreenImageUri = viewModel.resumeUri }
                        )
                    }
                }
                "edit_profile" -> EditProfileScreen(viewModel) { currentScreen = "main" }
                "company_search" -> {
                    BackHandler { currentScreen = "main" }
                    CompanySearchScreen(onBack = { currentScreen = "main" })
                }
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

// 在kotlin语言中，可以出现“定义在后，使用在前”的情况
val tabs = listOf("首页", "发展意向", "题库", "我")
val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.Edit, Icons.Default.Person)