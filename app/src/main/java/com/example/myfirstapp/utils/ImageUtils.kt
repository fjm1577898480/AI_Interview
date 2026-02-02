package com.example.myfirstapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {
    // 把 Uri 图片转成 Base64 字符串（数据流 -> 解码 -> 缩放 -> 压缩 -> 编码）
    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            // 1. 先只读取图片尺寸，不加载到内存
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            // 2. 计算缩放比例：限制长边最大为 1280px (原 800)
            // 800px 可能导致简历文字模糊，1280px 是一个平衡点
            var inSampleSize = 1
            val maxDimension = 1280
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight: Int = options.outHeight / 2
                val halfWidth: Int = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            // 3. 重新加载并按比例缩小图片
            val finalOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = inSampleSize
            }
            
            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, finalOptions)
            } ?: return null

            // 4. 压缩并转 Base64
            val outputStream = ByteArrayOutputStream()
            // 质量设为 70 (原 60)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            
            // 及时回收 bitmap
            bitmap.recycle()

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}