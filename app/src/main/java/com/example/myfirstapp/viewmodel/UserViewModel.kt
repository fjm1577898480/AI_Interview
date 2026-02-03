package com.example.myfirstapp.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfirstapp.data.model.*
import com.example.myfirstapp.data.network.RetrofitClient
import com.example.myfirstapp.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    // 你的专属 API KEY
    private val API_KEY = "Bearer ecbb5887d4f4427e90671235f7819f85.WxcOv5MWV45ydbgb"

    // --- 用户基础资料 ---
    var userName by mutableStateOf(prefs.getString("user_name", "") ?: "")
    var realName by mutableStateOf(prefs.getString("user_real_name", "") ?: "")
    var age by mutableStateOf(prefs.getString("user_age", "") ?: "")
    var signature by mutableStateOf(prefs.getString("user_sign", "") ?: "")
    var userAvatarUri by mutableStateOf<Uri?>(prefs.getString("avatar_uri", null)?.let { Uri.parse(it) })
    var resumeUri by mutableStateOf<Uri?>(prefs.getString("resume_uri", null)?.let { Uri.parse(it) })

    // --- AI 动态画像结果 ---
    var aiScore by mutableStateOf(prefs.getString("ai_score", "0") ?: "0")
    var aiCompanies by mutableStateOf(prefs.getString("ai_companies", "暂无分析建议") ?: "暂无分析建议")
    var aiGap by mutableStateOf(prefs.getString("ai_gap", "暂无分析建议") ?: "暂无分析建议")

    var aiChartData by mutableStateOf(decodeChartData(prefs.getString("ai_chart_data", "0,0,0,0,0")))
    var aiDimensions by mutableStateOf(decodeDimensions(prefs.getString("ai_dimensions", "专业能力,实践经验,学历背景,综合素质,岗位匹配")))

    var errorMessage by mutableStateOf<String?>(null)
    var isAnalyzing by mutableStateOf(false)

    // --- 工具函数 ---
    // --- 强化版的解码工具函数 ---
    private fun decodeDimensions(data: String?): List<String> {
        // 默认的 5 个维度
        val defaultList = listOf("专业能力", "实践经验", "学历背景", "综合素质", "岗位匹配")
        if (data.isNullOrBlank()) return defaultList

        val list = data.split(",").filter { it.isNotBlank() }
        // 如果解析出来的数量不是 5 个，强行修正为 5 个，防止雷达图绘制崩溃
        return if (list.size == 5) list else defaultList
    }

    private fun decodeChartData(data: String?): List<Float> {
        val defaultData = listOf(0f, 0f, 0f, 0f, 0f)
        if (data.isNullOrBlank()) return defaultData

        return try {
            val list = data.split(",").map { it.toFloatOrNull() ?: 0f }
            if (list.size == 5) list else defaultData
        } catch (e: Exception) {
            defaultData
        }
    }


    // --- 业务方法 (全保留) ---
    fun saveProfile(name: String, rName: String, uAge: String, sign: String, avatar: Uri?) {
        userName = name; realName = rName; age = uAge; signature = sign
        userAvatarUri = if (avatar != null && avatar.scheme != "file") saveFileToInternal(avatar, "user_avatar.jpg") else avatar
        prefs.edit().apply {
            putString("user_name", userName); putString("user_real_name", realName)
            putString("user_age", age); putString("user_sign", signature)
            putString("avatar_uri", userAvatarUri?.toString())
        }.apply()
    }

    fun saveResumeUri(uri: Uri) {
        val savedUri = saveFileToInternal(uri, "my_resume.jpg")
        resumeUri = Uri.parse("${savedUri}?time=${System.currentTimeMillis()}")
        prefs.edit().putString("resume_uri", savedUri.toString()).apply()
    }

    private fun saveFileToInternal(uri: Uri, fileName: String): Uri {
        val context = getApplication<Application>()
        val file = File(context.filesDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        return Uri.fromFile(file)
    }

    fun deleteResume() {
        resumeUri = null
        aiScore = "0"; aiCompanies = "暂无建议"; aiGap = "暂无分析"
        aiChartData = listOf(0f, 0f, 0f, 0f, 0f)
        aiDimensions = listOf("专业能力", "实践经验", "学历背景", "综合素质", "岗位匹配")
        File(getApplication<Application>().filesDir, "my_resume.jpg").apply { if (exists()) delete() }
        prefs.edit().clear().apply()
    }

    // --- 强化版 AI 分析逻辑 ---
    fun startResumeAnalysis(context: Context, onComplete: () -> Unit) {
        if (isAnalyzing) return
        isAnalyzing = true
        errorMessage = "正在进行个性化画像侧写..."

        viewModelScope.launch {
            try {
                val base64 = withContext(Dispatchers.IO) { ImageUtils.uriToBase64(context, resumeUri!!) }
                if (base64 == null) {
                    errorMessage = "图片转换失败"; isAnalyzing = false; return@launch
                }

                // 极端强化 Prompt，防止 AI 偷懒
                val prompt = """
                    作为职业测评专家，请分析简历图片并给出5个维度的能力侧写。
                    【硬性要求】：
                    1. 必须返回 5 个不同的维度名称，严禁少于 5 个。
                    2. 维度名称必须具体（如：Java开发、沟通协调、项目管理），禁止返回“维度1”等占位词。
                    3. values 必须是 5 个 0.1-1.0 之间的浮点数。
                    4. 严格 JSON 格式，不含任何解释文字。
                    
                    输出示例：
                    {
                      "dimensions": ["核心技术", "行业经验", "软技能", "学习潜力", "岗位匹配度"],
                      "values": [0.85, 0.6, 0.75, 0.9, 0.8],
                      "companies": "推荐公司名",
                      "gap": "短板分析"
                    }
                """.trimIndent()

                val request = ZhipuRequest(
                    model = "glm-4v",
                    messages = listOf(ZhipuMessage("user", listOf(
                        ZhipuContent("text", prompt),
                        ZhipuContent("image_url", image_url = ZhipuImageUrl(base64))
                    ))),
                    temperature = 0.85f // 保持适度灵活性
                )

                val response = RetrofitClient.instance.analyzeResume(API_KEY, request)
                if (response.isSuccessful) {
                    val rawBody = response.body()?.string() ?: ""
                    parseSafeDynamicResult(rawBody)
                    errorMessage = null
                } else {
                    errorMessage = "AI 响应失败: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "网络连接错误"
                Log.e("UserViewModel", "Analysis failed", e)
            } finally {
                isAnalyzing = false
                onComplete()
            }
        }
    }

    private fun parseSafeDynamicResult(rawResponse: String) {
        try {
            val root = JSONObject(rawResponse)
            val content = root.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")

            // 改进正则：更贪婪地匹配 JSON
            val jsonMatch = Regex("""\{.*\}""", RegexOption.DOT_MATCHES_ALL).find(content)
            val data = JSONObject(jsonMatch?.value ?: content)

            val dimArray = data.optJSONArray("dimensions")
            val valArray = data.optJSONArray("values")

            val newDims = mutableListOf<String>()
            val newVals = mutableListOf<Float>()
            var totalSum = 0f

            // 强制循环 5 次，确保雷达图不崩
            for (i in 0 until 5) {
                // 1. 尝试获取 AI 返回的维度名，如果获取不到或为空，则使用默认侧写
                val defaultList = listOf("核心技能", "综合表现", "成长潜力", "行业认知", "实践能力")
                val dName = dimArray?.optString(i)?.takeIf { it.isNotBlank() && !it.contains("维度") }
                    ?: defaultList[i]
                newDims.add(dName)

                // 2. 尝试获取分值，并进行归一化和钳位
                var v = valArray?.optDouble(i, 0.5)?.toFloat() ?: 0.5f
                if (v > 1.0f) v /= 100f
                val finalVal = v.coerceIn(0.1f, 1.0f) // 最小值 0.1 保证雷达图有形状
                newVals.add(finalVal)
                totalSum += finalVal
            }

            // 3. 更新 UI
            aiDimensions = newDims
            aiChartData = newVals
            aiScore = (totalSum / 5f * 100).roundToInt().toString()
            aiCompanies = data.optString("companies", "建议参考头部企业")
            aiGap = data.optString("gap", "建议加强项目深度")

            // 4. 持久化
            prefs.edit().apply {
                putString("ai_score", aiScore)
                putString("ai_companies", aiCompanies)
                putString("ai_gap", aiGap)
                putString("ai_dimensions", aiDimensions.joinToString(","))
                putString("ai_chart_data", aiChartData.joinToString(","))
            }.apply()

        } catch (e: Exception) {
            Log.e("UserViewModel", "Critical Parse Error", e)
            errorMessage = "分析完成，但结果侧写生成异常，请重试"
        }
    }

    // --- 测试连接 (全保留) ---
    fun testApiConnection() {
        if (isAnalyzing) return
        errorMessage = "正在握手 AI 服务..."
        viewModelScope.launch {
            try {
                val testRequest = ZhipuRequest(
                    model = "glm-4",
                    messages = listOf(ZhipuMessage("user", listOf(ZhipuContent("text", "hi"))))
                )
                val response = RetrofitClient.instance.analyzeResume(API_KEY, testRequest)
                errorMessage = if (response.isSuccessful) "连接成功！" else "握手失败: ${response.code()}"
            } catch (e: Exception) {
                errorMessage = "网络连接失败"
            }
        }
    }
}