package com.verdura.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ImageCompressor(private val context: Context) {

    companion object {
        private const val MAX_WIDTH = 1080
        private const val MAX_HEIGHT = 1080
        private const val QUALITY = 80
    }

    fun compressImage(uri: Uri, maxSizeKb: Int = 500): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            val scaledBitmap = scaleBitmap(originalBitmap)
            val compressedFile = compressBitmapToFile(scaledBitmap, maxSizeKb)

            if (scaledBitmap != originalBitmap) {
                originalBitmap.recycle()
            }

            compressedFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= MAX_WIDTH && height <= MAX_HEIGHT) {
            return bitmap
        }

        val ratio = minOf(
            MAX_WIDTH.toFloat() / width,
            MAX_HEIGHT.toFloat() / height
        )

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun compressBitmapToFile(bitmap: Bitmap, maxSizeKb: Int): File {
        val outputFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")

        var quality = QUALITY
        var outputStream: ByteArrayOutputStream

        do {
            outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            quality -= 10
        } while (outputStream.size() / 1024 > maxSizeKb && quality > 10)

        FileOutputStream(outputFile).use { fos ->
            fos.write(outputStream.toByteArray())
        }

        bitmap.recycle()
        return outputFile
    }

    fun getCompressedImageSize(file: File): Long {
        return file.length()
    }
}
