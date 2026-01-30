package com.example.myfirstapp.data.network

import com.example.myfirstapp.data.model.ZhipuRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AiApiService {
    @POST("chat/completions")
    suspend fun analyzeResume(
        @Header("Authorization") token: String, // 放入你的 API Key
        @Body request: ZhipuRequest
    ): Response<ResponseBody> // 先用 ResponseBody 接收原始字符串，方便调试
}