package com.atomx.genericprinter

import android.graphics.Bitmap
import kotlin.math.roundToInt

object EscPosImage {

    /**
     * Converts bitmap to ESC/POS raster command: GS v 0
     * mode 0 = normal
     */
    fun toRasterBytes(bitmap: Bitmap, targetWidthPx: Int): ByteArray {
        val resized = resizeToWidth(bitmap, targetWidthPx)
        val mono = toMonochrome(resized)

        val width = mono.width
        val height = mono.height
        val bytesPerRow = (width + 7) / 8

        val header = ByteArray(8)
        header[0] = 0x1D
        header[1] = 'v'.code.toByte()
        header[2] = '0'.code.toByte()
        header[3] = 0x00 // normal mode
        header[4] = (bytesPerRow and 0xFF).toByte()         // xL
        header[5] = ((bytesPerRow shr 8) and 0xFF).toByte() // xH
        header[6] = (height and 0xFF).toByte()              // yL
        header[7] = ((height shr 8) and 0xFF).toByte()      // yH

        val imageData = ByteArray(bytesPerRow * height)
        var idx = 0

        for (y in 0 until height) {
            for (xByte in 0 until bytesPerRow) {
                var b = 0
                for (bit in 0 until 8) {
                    val x = xByte * 8 + bit
                    if (x < width) {
                        val pixel = mono.getPixel(x, y)
                        // mono pixel is either black (0xFF000000) or white (0xFFFFFFFF)
                        val isBlack = (pixel and 0x00FFFFFF) == 0x000000
                        if (isBlack) b = b or (1 shl (7 - bit))
                    }
                }
                imageData[idx++] = b.toByte()
            }
        }

        return header + imageData
    }

    private fun resizeToWidth(bitmap: Bitmap, widthPx: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w == widthPx) return bitmap

        val scale = widthPx.toFloat() / w.toFloat()
        val newH = (h * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, widthPx, newH, true)
    }

    private fun toMonochrome(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val p = bitmap.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val gray = (r + g + b) / 3
                // threshold; tweak if needed
                val newPixel = if (gray < 160) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                out.setPixel(x, y, newPixel)
            }
        }
        return out
    }
}
