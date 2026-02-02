package com.example.myfirstapp.data.network


import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object RetrofitClient {
    private const val BASE_URL = "https://open.bigmodel.cn/api/paas/v4/"

    // 配置 OkHttpClient，延长超时时间以适应 AI 分析
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    // 使用 get() 属性而不是 lazy，确保每次获取时都是最新的（虽然单例下区别不大，但为了防止缓存问题，可以配合重新启动App）
    val instance: AiApiService
        get() = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiApiService::class.java)
}