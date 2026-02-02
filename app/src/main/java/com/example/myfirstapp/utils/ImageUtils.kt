package com.example.myfirstapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {
    // 把 Uri 图片转成 Base64 字符串（数据流 -> 解码 -> 压缩 -> 编码）
        fun uriToBase64(context: Context, uri: Uri): String? {
        return try {

            // 将原始图片转换为数据流
            val inputStream = context.contentResolver.openInputStream(uri)

            // 将数据流进行解码
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()

            // 压缩图片
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()

            // 编码数据流
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}