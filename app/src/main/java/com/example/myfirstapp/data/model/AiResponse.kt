package com.example.myfirstapp.data.model

// 智谱标准请求体
data class ZhipuRequest(
    val model: String = "glm-4v", // 使用具备视觉能力的模型
    val messages: List<ZhipuMessage>,
    val max_tokens: Int? = null,   // 改为可空，如果不传则使用默认值
    val temperature: Float? = null // 改为可空
)

data class ZhipuMessage(
    val role: String,
    val content: List<ZhipuContent>
)

data class ZhipuContent(
    val type: String,
    val text: String? = null,
    val image_url: ZhipuImageUrl? = null
)

data class ZhipuImageUrl(
    val url: String // 这里存放 base64 字符串数据
)