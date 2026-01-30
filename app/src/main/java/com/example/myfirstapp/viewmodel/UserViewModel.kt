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


class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    var userName by mutableStateOf(prefs.getString("user_name", "") ?: "")
    var realName by mutableStateOf(prefs.getString("user_real_name", "") ?: "")
    var age by mutableStateOf(prefs.getString("user_age", "") ?: "")
    var signature by mutableStateOf(prefs.getString("user_sign", "") ?: "")
    var userAvatarUri by mutableStateOf<Uri?>(prefs.getString("avatar_uri", null)?.let { Uri.parse(it) })
    var resumeUri by mutableStateOf<Uri?>(prefs.getString("resume_uri", null)?.let { Uri.parse(it) })

    var aiScore by mutableStateOf(prefs.getString("ai_score", "0") ?: "0")
    var aiCompanies by mutableStateOf(prefs.getString("ai_companies", "暂无分析") ?: "暂无分析")
    var aiGap by mutableStateOf(prefs.getString("ai_gap", "暂无分析") ?: "暂无分析")
    var aiChartData by mutableStateOf(decodeChartData(prefs.getString("ai_chart_data", "0,0,0,0,0")))

    // 在 UserViewModel 类内部，其他变量定义处添加
    var aiDimensions by mutableStateOf(decodeDimensions(prefs.getString("ai_dimensions", "专业技能,项目经验,学历背景,沟通能力,行业匹配")))

    // 辅助工具函数：将存放在 Prefs 里的逗号分隔字符串转回列表
    private fun decodeDimensions(data: String?): List<String> =
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

    private fun saveFileToInternal(uri: Uri, fileName: String): Uri {
        val context = getApplication<Application>()
        val file = File(context.filesDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        return Uri.fromFile(file)
    }

    private fun decodeChartData(data: String?): List<Float> =
        data?.split(",")?.map { it.toFloatOrNull() ?: 0f } ?: listOf(0f,0f,0f,0f,0f)

    fun startResumeAnalysis(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val base64 = ImageUtils.uriToBase64(context, resumeUri!!) ?: return@launch


                // 极其严格的 Prompt
                val strictPrompt = """
                你是一名资深的 HR 专家。请按以下流程严格执行：
                1. 验证图片：如果图片内容不是个人简历，请将 isResume 设为 false，并结束分析。
                2. 识别行业：根据简历内容判断所属行业（如：互联网、金融、教育等）。
                3. 动态维度：针对该行业，提取 5 个最核心的评价指标（如程序员是：技术栈、算法、项目、学历、软实力）。
                4. 评分：给出每个指标的评分（0-100）。
                5. 岗位差距：如果没有识别到明确的求职意向或岗位，请在 gapToTarget 字段回答“请先在‘发展意向’中明确具体岗位”。
                
                必须返回以下格式的纯 JSON，不要包含任何 markdown 标识：
                {
                  "isResume": true,
                  "industry": "行业名称",
                  "score": 85,
                  "dimensions": ["维度1", "维度2", "维度3", "维度4", "维度5"],
                  "chartData": [80, 70, 90, 60, 85],
                  "suitableCompanies": "建议公司",
                  "gapToTarget": "差距分析"
                }
            """.trimIndent()

                val request = ZhipuRequest(messages = listOf(ZhipuMessage("user", listOf(
                    ZhipuContent("text", strictPrompt),
                    ZhipuContent("image_url", image_url = ZhipuImageUrl("data:image/jpeg;base64,$base64"))
                ))))


                val resp = RetrofitClient.instance.analyzeResume("Bearer 38283e409b9e47c0aaf617524843ac78.iX1eeSzlsyMK0CEM", request)
                if (resp.isSuccessful) parseAiResult(resp.body()?.string())
            } catch (e: Exception) { e.printStackTrace() }
            finally { onComplete() }
        }
    }

    private fun parseAiResult(json: String?) {
        try {
            // 1. 提取 AI 返回的字符串内容
            val root = JSONObject(json!!)
            val content = root.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")

            // 2. 将内容清洗并转为 JSON 对象
            val obj = JSONObject(content.substringAfter("{").substringBeforeLast("}").let { "{$it}" })

            // 3. 防糊弄逻辑：检查是否为简历
            val isResume = obj.optBoolean("isResume", true)
            if (!isResume) {
                aiScore = "0"
                aiCompanies = "分析失败：检测到图片非简历"
                aiGap = "请重新上传清晰、真实的个人简历图片"
                return
            }

            // 4. 解析基础得分与文字建议
            aiScore = obj.optInt("score").toString()
            aiCompanies = obj.optString("suitableCompanies")
            aiGap = obj.optString("gapToTarget")

            // 5. 【新增】解析动态维度名称 (Radar Labels)
            val dimArray = obj.optJSONArray("dimensions")
            val dimList = if (dimArray != null) {
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

        } catch (e: Exception) {
            e.printStackTrace()
            // 出错时给出提示信息
            aiCompanies = "分析出错，请检查网络或简历清晰度"
        }
    }

    // UserViewModel.kt 内部

    fun deleteResume() {
        // 1. 清除内存中的数据
        resumeUri = null
        aiScore = "0"
        aiCompanies = ""
        aiGap = ""
        aiChartData = listOf(0f, 0f, 0f, 0f, 0f)
        aiDimensions = listOf("专业技能", "项目经验", "学历背景", "沟通能力", "行业匹配")

        // 2. 删除本地物理文件
        val file = File(getApplication<Application>().filesDir, "my_resume.jpg")
        if (file.exists()) file.delete()

        // 3. 清除 SharedPreferences 缓存
        prefs.edit().apply {
            remove("resume_uri")
            remove("ai_score")
            remove("ai_companies")
            remove("ai_gap")
            remove("ai_chart_data")
            remove("ai_dimensions")
        }.apply()
    }
}