package com.example.myfirstapp.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfirstapp.data.model.*
import com.example.myfirstapp.data.network.RetrofitClient
import com.example.myfirstapp.utils.ImageUtils
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
/**
 * UserViewModel是一个可以根据需求收发数据的一个智能仓库
 *
 */
class UserViewModel(application: Application) : AndroidViewModel(application) {

    // 创建一个sharedPreference对象，表现为user_prefs.xml文件，只有这个软件有权读写这个xml文件
    private val prefs = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    // 从user_prefs文件中尝试获取user_name变量，并将其标记为“可观察”. 可观察：如果这个变量发生变化，就会刷新所有使用了这个变量的页面
    var userName by mutableStateOf(prefs.getString("user_name", "") ?: "")
    var realName by mutableStateOf(prefs.getString("user_real_name", "") ?: "")
    var age by mutableStateOf(prefs.getString("user_age", "") ?: "")

    var signature by mutableStateOf(prefs.getString("user_sign", "") ?: "")

    // ?.语法:如果前面的值不为null，则执行后面的逻辑语句，这个语法常见于兜底用
    // ?.语法：如果前面的值为null，则设置成后面的值
    var userAvatarUri by mutableStateOf<Uri?>(prefs.getString("avatar_uri", null)?.let { Uri.parse(it) })
    var resumeUri by mutableStateOf<Uri?>(prefs.getString("resume_uri", null)?.let { Uri.parse(it) })

    var aiScore by mutableStateOf(prefs.getString("ai_score", "0") ?: "0")
    var aiCompanies by mutableStateOf(prefs.getString("ai_companies", "暂无分析") ?: "暂无分析")
    var aiGap by mutableStateOf(prefs.getString("ai_gap", "暂无分析") ?: "暂无分析")
    var aiChartData by mutableStateOf(decodeChartData(prefs.getString("ai_chart_data", "0,0,0,0,0")))

    // 在 UserViewModel 类内部，其他变量定义处添加
    var aiDimensions by mutableStateOf(decodeDimensions(prefs.getString("ai_dimensions", "专业技能,项目经验,学历背景,沟通能力,行业匹配")))
    
    // 错误信息状态，用于 UI 展示
    var errorMessage by mutableStateOf<String?>(null)

    // 辅助工具函数：将存放在 Prefs 里的逗号分隔字符串转回列表

    // 接收一个字符串参数（可能为空），要求返回一个list类型的列表
    private fun decodeDimensions(data: String?): List<String> =

        // 如果不为空，则按逗号分割。如果分割后的结果为空，则设置成具有特定内容的列表
        data?.split(",") ?: listOf("专业技能", "项目经验", "学历背景", "沟通能力", "行业匹配")
    fun saveProfile(name: String, rName: String, uAge: String, sign: String, avatar: Uri?) {
        userName = name; realName = rName; age = uAge; signature = sign
        if (avatar != null && avatar.scheme != "file") {
            userAvatarUri = saveFileToInternal(avatar, "user_avatar.jpg")
        } else { userAvatarUri = avatar }

        prefs.edit().apply {
            putString("user_name", userName)
            putString("user_real_name", realName)
            putString("user_age", age)
            putString("user_sign", signature)

            // Uri是个复杂对象，于是先将其转换成字符串对象，再存入xml文件中，如果直接写.toString()可能会报错，因为有可能userAvatarUri不存在，所以使用?.，先检验是否为空
            putString("avatar_uri", userAvatarUri?.toString())
        }.apply()
    }

    fun saveResumeUri(uri: Uri) {
        val savedUri = saveFileToInternal(uri, "my_resume.jpg")

        // 关键点：在 Uri 后面加上查询参数（时间戳），确保 Uri 对象的唯一性
        // 这样 Compose 观察到 resumeUri 变化了，UI 就会立即刷新
        val refreshUri = Uri.parse("${savedUri}?time=${System.currentTimeMillis()}")

        resumeUri = refreshUri

        // 存入 Prefs 时存原始路径即可
        prefs.edit().putString("resume_uri", savedUri.toString()).apply()
    }


    /**
     * 将资源保存在本地，用于数据持久化
     */
    private fun saveFileToInternal(uri: Uri, fileName: String): Uri {

        // 获取真个程序目前的上下文，这个上下文生命周期和APP的进程一致
        val context = getApplication<Application>()
        val file = File(context.filesDir, fileName)


        // 获取资源管理器，开启获取方到目标资源的输入流
        context.contentResolver.openInputStream(uri)?.use {

            // input是openInputStream流在use语法中的别名，用以替换之 ->是说input接下来要干什么什么事
            input ->

            // output是FileOutputStream流在use语法中的别名，用以替换之
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        return Uri.fromFile(file)
    }

    private fun decodeChartData(data: String?): List<Float> =

        // 这里的map不再是java中的哈希表，而是一种函数，负责将原先列表中的每一个元素，按照一种函数映射成另一个元素
        data?.split(",")?.map { it.toFloatOrNull() ?: 0f } ?: listOf(0f,0f,0f,0f,0f)




// ...

    fun startResumeAnalysis(context: Context, onComplete: () -> Unit) {
        // 重置错误信息
        errorMessage = null
        
        viewModelScope.launch {
            try {
                errorMessage = "正在压缩图片..."
                
                // 切换到 IO 线程处理图片，防止 ANR
                val base64 = withContext(Dispatchers.IO) {
                    ImageUtils.uriToBase64(context, resumeUri!!)
                }
                
                if (base64 == null) {
                    errorMessage = "图片处理失败，请重试"
                    return@launch
                }

                // 极其严格的 Prompt
                val strictPrompt = """
                你是一名资深的 HR 专家。请按以下流程严格执行：
                1. 验证图片：如果图片内容不是个人简历，请将 isResume 设为 false，并结束分析。
                2. 识别行业：根据简历内容判断所属行业（如：互联网、金融、教育等）。
                3. 动态维度：针对该行业，提取 5 个最核心的评价指标（如程序员是：技术栈、算法、项目、学历、软实力）。
                
                【重要】其中“学历”维度的评分必须严格遵守以下标准：
                - 博士：95-100分
                - 硕士研究生：85-94分
                - 985/211/双一流本科：75-84分
                - 普通本科：60-74分
                - 专科：40-59分
                - 其他：40分以下
                
                4. 评分：给出每个指标的评分（0-100）。
                5. 岗位差距：如果没有识别到明确的求职意向或岗位，请在 gapToTarget 字段回答“请先在‘发展意向’中明确具体岗位”。
                
                请忽略图片中的个人隐私信息（如电话、地址），只分析专业能力。
                必须只返回以下格式的纯 JSON，绝对不要包含任何 markdown 标识（如 ```json），不要包含任何额外的解释文字或免责声明（例如“请注意...”）：
                {
                  "isResume": true,
                  "industry": "行业名称",
                  "score": 46666,
                  "dimensions": ["维度1", "维度2", "维度3", "维度4", "维度5"],
                  "chartData": [80, 70, 90, 60, 85],
                  "suitableCompanies": "建议公司",
                  "gapToTarget": "差距分析"
                }
            """.trimIndent()

                // 构建 Request 对象
                // 1. 系统提示词：设定 AI 身份和输出格式
                val systemMessage = ZhipuMessage("system", listOf(
                    ZhipuContent("text", "你是一个简历分析助手。你必须忽略图片中的个人隐私信息（如电话、地址），只分析专业能力。你必须且只能返回 JSON 格式的数据。")
                ))

                // 2. 用户消息：先发图片，再发具体的指令
                val userMessage = ZhipuMessage("user", listOf(
                    ZhipuContent("image_url", image_url = ZhipuImageUrl("data:image/jpeg;base64,$base64")),
                    ZhipuContent("text", strictPrompt)
                ))

                val request = ZhipuRequest(messages = listOf(systemMessage, userMessage))

                errorMessage = "正在上传并等待 AI 思考（可能需要1分钟）..."
                
                // 切换到 IO 线程发起网络请求
                val resp = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.analyzeResume("Bearer ecbb5887d4f4427e90671235f7819f85.WxcOv5MWV45ydbgb", request)
                }

                // 解析返回状态码，一般在200~299之间算成功
                if (resp.isSuccessful) {
                    errorMessage = "AI 分析完成，正在解析..."
                    // 解析响应的主体内容
                    val bodyString = resp.body()?.string()
                    
                    // 解析逻辑（如果在 parseAiResult 内部失败并抛出异常，会跳到下面的 catch 块）
                    parseAiResult(bodyString)
                    
                    // 只有完全成功解析后，才清除错误信息
                    errorMessage = null
                } else {
                    errorMessage = "AI 请求失败: Code ${resp.code()} - ${resp.errorBody()?.string()}"
                }

            } catch (t: Throwable) { // 捕获所有 Throwable，防止 OOM 等 Error 漏网
                t.printStackTrace()
                // 如果是 Timeout，给出更友好的提示
                if (t.message?.contains("timeout", ignoreCase = true) == true) {
                     errorMessage = "请求超时。可能是图片过大或网络拥堵，请重试。"
                } else {
                     errorMessage = "发生错误: ${t.javaClass.simpleName} - ${t.message}"
                }
            }
            finally { onComplete() }
        }
    }


    fun testApiConnection() {
        errorMessage = "正在测试 API 连接..."
        viewModelScope.launch {
            try {
                // 构造极简请求
                val request = ZhipuRequest(
                    model = "glm-4-flash", // 尝试使用更轻量的模型
                    messages = listOf(ZhipuMessage("user", listOf(
                        ZhipuContent("text", "Ping")
                    )))
                )

                val resp = RetrofitClient.instance.analyzeResume("Bearer ecbb5887d4f4427e90671235f7819f85.WxcOv5MWV45ydbgb", request)

                if (resp.isSuccessful) {
                    errorMessage = "连接成功！API 响应正常: ${resp.code()}"
                } else {
                    errorMessage = "连接失败: ${resp.code()} - ${resp.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "连接异常: ${e.javaClass.simpleName} - ${e.message}"
            }
        }
    }

    /**
     *
     * 解析返回来的json数据
     * 注意：此方法会抛出异常，由调用者处理
     *  */
    private fun parseAiResult(json: String?) {
        // 1. 提取 AI 返回的字符串内容
        if (json == null) throw IllegalArgumentException("AI 返回内容为空")

        // 将json字符串转换成其内容中的一堆键值对
        val root = JSONObject(json)

        // 检查是否有 error 字段（Zhipu AI 有时会返回错误信息）
        if (root.has("error")) {
            throw RuntimeException(root.optJSONObject("error")?.optString("message") ?: "未知 API 错误")
        }

        // 层层获取，层层解析
        val choices = root.optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            throw RuntimeException("AI 未返回有效选项")
        }
        
        val content = choices.getJSONObject(0).getJSONObject("message").getString("content")

        // 2. 将内容清洗并转为 JSON 对象
        // 改进逻辑：寻找包含 "isResume" 关键字的最近的大括号，防止被前面的免责声明干扰
        var cleanContent = content
        val keyIndex = content.indexOf("\"isResume\"")
        
        if (keyIndex != -1) {
            val startIndex = content.lastIndexOf("{", keyIndex)
            val endIndex = content.lastIndexOf("}")
            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                cleanContent = content.substring(startIndex, endIndex + 1)
            } else {
                 throw RuntimeException("无法定位 JSON 起止符号。原始内容：$content")
            }
        } else {
            // 如果没找到 isResume，说明 AI 完全跑偏了，直接把原始内容抛出来以便调试
            throw RuntimeException("AI 返回数据格式错误（未找到关键字段）。原始内容：$content")
        }
        
        val obj = try {
            JSONObject(cleanContent)
        } catch (e: JSONException) {
             throw RuntimeException("JSON 格式解析失败: ${e.message}。截取内容：$cleanContent")
        }

        // 3. 防糊弄逻辑：检查是否为简历
        val isResume = obj.optBoolean("isResume", true)
        
        if (!isResume) {
            // 设置 UI 状态
            aiScore = "0"
            aiCompanies = "分析失败：检测到图片非简历"
            aiGap = "请重新上传清晰、真实的个人简历图片"
            // 抛出异常以中断流程并显示 Toast
            throw RuntimeException("AI 认为这张图片不是简历，请重新上传")
        }

        // 4. 解析基础得分与文字建议
        aiScore = obj.optInt("score").toString()
        aiCompanies = obj.optString("suitableCompanies")
        aiGap = obj.optString("gapToTarget")

        // 5. 【新增】解析动态维度名称 (Radar Labels)
        val dimArray = obj.optJSONArray("dimensions")

        // 在kotlin中，if-else可以直接返回一个值
        val dimList = if (dimArray != null) {
            // 将左侧数字映射成右边代码的下标，然后通过下标获取元素
            (0 until 5).map { dimArray.getString(it) }
        } else {
            listOf("专业技能", "项目经验", "学历背景", "沟通能力", "行业匹配")
        }
        aiDimensions = dimList

        // 6. 解析雷达图数据 (Radar Values)
        val chartArray = obj.optJSONArray("chartData")
        val valueList = if (chartArray != null) {
            (0 until 5).map { chartArray.getDouble(it).toFloat() / 100f }
        } else {
            listOf(0f, 0f, 0f, 0f, 0f)
        }
        aiChartData = valueList

        // 7. 持久化保存到本地，以便下次打开 App 还能看到
        prefs.edit().apply {
            putString("ai_score", aiScore)
            putString("ai_companies", aiCompanies)
            putString("ai_gap", aiGap)
            putString("ai_dimensions", dimList.joinToString(",")) // 保存维度名
            putString("ai_chart_data", valueList.joinToString(",")) // 保存数据点
        }.apply()
    }

    // UserViewModel.kt 内部

    fun deleteResume() {
        // 1. 清除内存中的数据
        resumeUri = null
        aiScore = "0"
        aiCompanies = ""
        aiGap = ""

        // 定义一个列表，其中5个值全为0
        aiChartData = listOf(0f, 0f, 0f, 0f, 0f)
        aiDimensions = listOf("专业技能", "项目经验", "学历背景", "沟通能力", "行业匹配")

        // 2. 删除本地物理文件

        // 获取当前APP的实例，创建一个名为“my_resume.jpg”的文件
        val file = File(getApplication<Application>().filesDir, "my_resume.jpg")

        if (file.exists()) file.delete()

        // 3. 清除 SharedPreferences 缓存

        // 开启编辑器
        prefs.edit().

        // 执行以下操作
        apply {
            remove("resume_uri")
            remove("ai_score")
            remove("ai_companies")
            remove("ai_gap")
            remove("ai_chart_data")
            remove("ai_dimensions")
        }
            // 确认修改
            .apply()
    }
}