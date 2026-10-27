package com.scanrobot.app.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

object QRCodeGenerator {

    fun generateQRCode(content: String, size: Int = 800): Bitmap? {
        return generateStyledQRCode(content, size, Color.BLACK, Color.WHITE, false, null)
    }

    fun generateStyledQRCode(
        content: String,
        size: Int = 800,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE,
        useRoundDots: Boolean = false,
        logoBitmap: Bitmap? = null
    ): Bitmap? {
        return try {
            val hints = mutableMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            hints[EncodeHintType.MARGIN] = 1
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

            canvas.drawColor(backgroundColor)

            if (useRoundDots) {
                paint.color = foregroundColor
                val dotSize = width.toFloat() / (if (width > 0) (content.toByteArray().size.coerceAtLeast(1)) else 1).toFloat().coerceAtLeast(1f)
                val cellSize = width.toFloat() / 25f
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        if (bitMatrix.get(x, y)) {
                            canvas.drawCircle(
                                x * cellSize + cellSize / 2,
                                y * cellSize + cellSize / 2,
                                cellSize / 2,
                                paint
                            )
                        }
                    }
                }
            } else {
                val cellSize = width.toFloat() / bitMatrix.width.toFloat().coerceAtLeast(1f)
                paint.color = foregroundColor
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        if (bitMatrix.get(x, y)) {
                            canvas.drawRect(
                                x * cellSize,
                                y * cellSize,
                                (x + 1) * cellSize,
                                (y + 1) * cellSize,
                                paint
                            )
                        }
                    }
                }
            }

            if (logoBitmap != null) {
                val logoSize = (width * 0.2f).toInt()
                val left = (width - logoSize) / 2
                val top = (height - logoSize) / 2
                val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, logoSize, logoSize, true)
                val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                bgPaint.color = Color.WHITE
                val padding = 8
                canvas.drawRoundRect(
                    (left - padding).toFloat(),
                    (top - padding).toFloat(),
                    (left + logoSize + padding).toFloat(),
                    (top + logoSize + padding).toFloat(),
                    12f, 12f, bgPaint
                )
                canvas.drawBitmap(scaledLogo, left.toFloat(), top.toFloat(), null)
            }

            bmp
        } catch (e: Exception) {
            null
        }
    }

    fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String = "qr_${System.currentTimeMillis()}"): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.png")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRCode")
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
                uri
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "QRCode")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "$fileName.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun shareImage(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享二维码"))
    }
}
